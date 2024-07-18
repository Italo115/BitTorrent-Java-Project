import com.google.gson.Gson;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class Main {
    private static final Gson gson = new Gson();
    private static final String PEER_ID = "00112233445566778899";
    private static final int PEER_PORT = 6881;
    private static final int BLOCK_SIZE = 16 * 1024; // 16 KiB

    public static void main(String[] args) throws Exception {
        processCommand(args[0], args[1], args.length > 2 ? args[2] : null, args.length > 3 ? args[3] : null);
    }

    private static void processCommand(String command, String arg, String peer, String outFile)
            throws IOException, URISyntaxException, InterruptedException {
        switch (command) {
            case "info" -> processInfoCommand(Path.of(arg));
            case "decode" -> processDecodeCommand(arg);
            case "peers" -> processPeersCommand(Path.of(arg));
            case "handshake" -> processHandshakeCommand(Path.of(arg), peer);
            case "download_piece" -> processDownloadPieceCommand(Path.of(arg), peer, outFile);
            default -> throw new IllegalArgumentException("Unsupported command.");
        }
    }

    private static void processInfoCommand(Path filePath) throws IOException {
        Map<?, ?> infoDict = getDecodedMap(filePath);
        Info info = getInfoFromDict(infoDict);
        System.out.printf("Tracker URL: %s%nLength: %d%nInfo Hash: %s%nPiece Length: %d%n",
                info.trackerUrl(), info.length(), toHexString(info.infoHash()), info.infoDict().get("piece length"));
        printPieceHashes(info.infoDict());
    }

    private static void processDecodeCommand(String arg) {
        try {
            System.out.println(gson.toJson(BencodeEncoder.decode(arg)));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void processPeersCommand(Path filePath)
            throws IOException, URISyntaxException, InterruptedException {
        Map<?, ?> infoDict = getDecodedMap(filePath);
        Info info = getInfoFromDict(infoDict);
        HttpRequest httpGetRequest = createFirstRequest(info.trackerUrl(), info.length(), info.infoHash());
        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(httpGetRequest, HttpResponse.BodyHandlers.ofByteArray());
        printResponse(BencodeEncoder.decode(response.body()));
    }

    private static void processHandshakeCommand(Path filePath, String peerAddress)
            throws IOException, URISyntaxException, InterruptedException {
        if (peerAddress == null || !peerAddress.contains(":")) {
            throw new IllegalArgumentException("Peer address must be provided in the format <peer_ip>:<peer_port>");
        }
        String[] peerParts = peerAddress.split(":");
        String peerIp = peerParts[0];
        int peerPort = Integer.parseInt(peerParts[1]);

        Map<?, ?> infoDict = getDecodedMap(filePath);
        Info info = getInfoFromDict(infoDict);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(peerIp, peerPort), 10000); // 10-second timeout
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Send handshake
            ByteBuffer handshakeBuffer = createHandshake(info.infoHash());
            out.write(handshakeBuffer.array());
            out.flush();

            // Receive handshake
            byte[] response = new byte[68]; // Handshake message is always 68 bytes
            int bytesRead = in.read(response);
            if (bytesRead != 68) {
                throw new IOException("Incomplete handshake received");
            }

            // Extract and print peer ID
            byte[] receivedPeerId = new byte[20];
            System.arraycopy(response, 48, receivedPeerId, 0, 20);
            System.out.printf("Peer ID: %s%n", toHexString(receivedPeerId));
        }
    }

    private static void processDownloadPieceCommand(Path filePath, String peerAddress, String outFile)
            throws IOException, URISyntaxException, InterruptedException, NoSuchAlgorithmException {
        if (peerAddress == null || !peerAddress.contains(":")) {
            throw new IllegalArgumentException("Peer address must be provided in the format <peer_ip>:<peer_port>");
        }
        if (outFile == null) {
            throw new IllegalArgumentException("Output file path must be provided");
        }
        String[] peerParts = peerAddress.split(":");
        String peerIp = peerParts[0];
        int peerPort = Integer.parseInt(peerParts[1]);

        Map<?, ?> infoDict = getDecodedMap(filePath);
        Info info = getInfoFromDict(infoDict);

        int pieceIndex = 0; // Assuming piece index is provided as a fixed value

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(peerIp, peerPort), 10000); // 10-second timeout
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Send handshake
            ByteBuffer handshakeBuffer = createHandshake(info.infoHash());
            out.write(handshakeBuffer.array());
            out.flush();

            // Receive handshake
            byte[] response = new byte[68]; // Handshake message is always 68 bytes
            int bytesRead = in.read(response);
            if (bytesRead != 68) {
                throw new IOException("Incomplete handshake received");
            }

            // Wait for bitfield message (ID = 5)
            if (in.read() != 0) throw new IOException("Invalid bitfield message length");
            if (in.read() != 1) throw new IOException("Invalid bitfield message ID");
            if (in.read() != 5) throw new IOException("Invalid bitfield message");

            // Send interested message (ID = 2)
            out.write(new byte[]{0, 0, 0, 1, 2});
            out.flush();

            // Wait for unchoke message (ID = 1)
            if (in.read() != 0) throw new IOException("Invalid unchoke message length");
            if (in.read() != 1) throw new IOException("Invalid unchoke message ID");
            if (in.read() != 1) throw new IOException("Invalid unchoke message");

            int pieceLength = (int) info.infoDict().get("piece length");
            byte[] pieceData = new byte[pieceLength];
            int offset = 0;

            // Break the piece into blocks and send request for each block
            while (offset < pieceLength) {
                int blockSize = Math.min(BLOCK_SIZE, pieceLength - offset);
                ByteBuffer requestBuffer = ByteBuffer.allocate(17);
                requestBuffer.putInt(13); // Length of the message (1 + 4 + 4 + 4)
                requestBuffer.put((byte) 6); // Message ID for request
                requestBuffer.putInt(pieceIndex);
                requestBuffer.putInt(offset);
                requestBuffer.putInt(blockSize);
                out.write(requestBuffer.array());
                out.flush();

                // Wait for piece message (ID = 7)
                if (in.read() != 0) throw new IOException("Invalid piece message length");
                if (in.read() != (blockSize + 9) >> 24) throw new IOException("Invalid piece message length");
                if (in.read() != ((blockSize + 9) >> 16) & 0xff) throw new IOException("Invalid piece message length");
                if (in.read() != ((blockSize + 9) >> 8) & 0xff) throw new IOException("Invalid piece message length");
                if (in.read() != (blockSize + 9) & 0xff) throw new IOException("Invalid piece message length");
                if (in.read() != 7) throw new IOException("Invalid piece message ID");

                byte[] block = new byte[blockSize];
                in.readNBytes(block, 0, blockSize);
                System.arraycopy(block, 0, pieceData, offset, blockSize);
                offset += blockSize;
            }

            // Verify the piece hash
            byte[] pieceHash = Arrays.copyOfRange((byte[]) info.infoDict().get("pieces"), pieceIndex * 20, (pieceIndex + 1) * 20);
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            byte[] calculatedHash = sha1Digest.digest(pieceData);
            if (!Arrays.equals(pieceHash, calculatedHash)) {
                throw new IOException("Piece hash does not match");
            }

            // Save piece to file
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(pieceData);
            }
            System.out.printf("Piece %d downloaded to %s.%n", pieceIndex, outFile);
        }
    }

    private static ByteBuffer createHandshake(byte[] infoHash) {
        ByteBuffer buffer = ByteBuffer.allocate(68);
        buffer.put((byte) 19); // Protocol string length
        buffer.put("BitTorrent protocol".getBytes(StandardCharsets.US_ASCII));
        buffer.put(new byte[8]); // Reserved bytes
        buffer.put(infoHash);
        buffer.put(PEER_ID.getBytes(StandardCharsets.US_ASCII));
        buffer.flip();
        return buffer;
    }

    private static Map<?, ?> getDecodedMap(Path filePath) throws IOException {
        return (Map<?, ?>) Objects.requireNonNull(BencodeEncoder.decode(Files.readAllBytes(filePath)));
    }

    private static HttpRequest createFirstRequest(String trackerURL, long length, byte[] infoHash)
            throws UnsupportedEncodingException {
        String encodedInfoHash = URLEncoder.encode(new String(infoHash, StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);
        String encodedPeerId = URLEncoder.encode(PEER_ID, StandardCharsets.ISO_8859_1);
        String urlString = String.format("%s?info_hash=%s&peer_id=%s&port=%d&uploaded=0&downloaded=0&left=%d&compact=1",
                trackerURL, encodedInfoHash, encodedPeerId, PEER_PORT, length);
        return HttpRequest.newBuilder().GET().uri(URI.create(urlString)).build();
    }

    private static void printResponse(Object trackerResponse) {
        if (trackerResponse instanceof Map<?, ?> m) {
            ByteBuffer buffer = ByteBuffer.wrap(((String) m.get("peers")).getBytes(StandardCharsets.ISO_8859_1));
            while (buffer.hasRemaining()) {
                int ip = buffer.getInt();
                short port = buffer.getShort();
                System.out.printf("%d.%d.%d.%d:%d%n", (ip >> 24) & 0xff, (ip >> 16) & 0xff, (ip >> 8) & 0xff, ip & 0xff, port & 0xffff);
            }
        } else {
            throw new IllegalStateException("Unexpected value: " + trackerResponse);
        }
    }

    private static Info getInfoFromDict(Map<?, ?> m) {
        String trackerUrl = (String) m.get("announce");
        Map<?, ?> infoDict = (Map<?, ?>) m.get("info");
        return new Info(trackerUrl, infoDict, (Long) infoDict.get("length"), calculateHash(infoDict));
    }

    private static byte[] calculateHash(Map<?, ?> infoDict) {
        try {
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            sha1Digest.update(BencodeEncoder.encodeToByteBuff(infoDict));
            return sha1Digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printPieceHashes(Map<?, ?> infoDict) {
        byte[] bytes = ((String) infoDict.get("pieces")).getBytes(StandardCharsets.ISO_8859_1);
        System.out.print("Piece Hashes:");
        for (int i = 0; i < bytes.length; i++) {
            if (i % 20 == 0) System.out.println();
            System.out.printf("%02x", bytes[i]);
        }
    }

    private static String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private record Info(String trackerUrl, Map<?, ?> infoDict, long length, byte[] infoHash) {
    }
}
