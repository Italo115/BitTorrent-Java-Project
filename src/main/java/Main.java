import com.google.gson.Gson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

public class Main {
    private static final Gson gson = new Gson();
    private static final String PEER_ID = "00112233445566778899";
    private static final int PEER_PORT = 6881;

    public static void main(String[] args) throws Exception {
        processCommand(args[0], args[1]);
    }

    private static void processCommand(String command, String arg)
            throws IOException, URISyntaxException, InterruptedException {
        switch (command) {
            case "info" -> processInfoCommand(Path.of(arg));
            case "decode" -> processDecodeCommand(arg);
            case "peers" -> processPeersCommand(Path.of(arg));
            default -> throw new NoSuchElementException("Unsupported command.");
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
            Object decoded = BencodeEncoder.decode(arg);
            System.out.println(gson.toJson(decoded));
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

    private static Map<?, ?> getDecodedMap(Path filePath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(filePath);
        return (Map<?, ?>) Objects.requireNonNull(BencodeEncoder.decode(fileBytes));
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
        long length = (Long) infoDict.get("length");
        byte[] infoHash = calculateHash(infoDict);
        return new Info(trackerUrl, infoDict, length, infoHash);
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

    private record Info(String trackerUrl, Map<?, ?> infoDict, Long length, byte[] infoHash) {
    }
}
