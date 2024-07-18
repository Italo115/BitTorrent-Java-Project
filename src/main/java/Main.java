import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        String command = args[0];
        if (command.equals("decode")) {
            String bencodedValue = args[1];
            String decoded;
            switch (bencodedValue.charAt(0)) {
                case 'i' -> {
                    Bencode bencode = new Bencode(true);
                    decoded = "" + bencode.decode(bencodedValue.getBytes(), Type.NUMBER);
                }
                case 'l' -> {
                    Bencode bencode = new Bencode(false);
                    decoded = gson.toJson(bencode.decode(bencodedValue.getBytes(), Type.LIST));
                }
                case 'd' -> {
                    Bencode bencode = new Bencode(false);
                    decoded = gson.toJson(bencode.decode(bencodedValue.getBytes(), Type.DICTIONARY));
                }
                default -> {
                    try {
                        decoded = gson.toJson(decodeBencode(bencodedValue));
                    } catch (RuntimeException e) {
                        System.out.println(e.getMessage());
                        return;
                    }
                }
            }
            System.out.println(decoded);
        } else if (command.equals("info")) {
            String filePath = args[1];
            TorrentInfo torrent = new TorrentInfo(Files.readAllBytes(Path.of(filePath)));
            System.out.println("Tracker URL: " + torrent.announce);
            System.out.println("Length: " + torrent.length);
            System.out.println("Info Hash: " + bytesToHex(torrent.infoHash));
            System.out.println("Piece Length: " + torrent.pieceLength);
            for (byte[] pieceHash : torrent.pieceHashes) {
                System.out.println(bytesToHex(pieceHash));
            }
        } else if (command.equals("peers")) {
            String filePath = args[1];

        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    private static String decodeBencode(String bencodedString) {
        if (Character.isDigit(bencodedString.charAt(0))) {
            int firstColonIndex = 0;
            for (int i = 0; i < bencodedString.length(); i++) {
                if (bencodedString.charAt(i) == ':') {
                    firstColonIndex = i;
                    break;
                }
            }
            int length = Integer.parseInt(bencodedString.substring(0, firstColonIndex));
            return bencodedString.substring(firstColonIndex + 1, firstColonIndex + 1 + length);
        } else {
            throw new RuntimeException("Only strings are supported at the moment");
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static HttpRequest createFirstRequest(String trackerURL, long length, byte[] infoHash) throws URISyntaxException, UnsupportedEncodingException, MalformedURLException {
        String strInfoHash = new String(infoHash, StandardCharsets.ISO_8859_1);
        String encodedInfoHash = URLEncoder.encode(strInfoHash, StandardCharsets.ISO_8859_1);
        String encodedPeerId = URLEncoder.encode("00112233445566778899", StandardCharsets.ISO_8859_1);
        String urlString = String.format("%s?info_hash=%s&peer_id=%s&port=6881&uploaded=0&downloaded=0&left=%d&compact=1", trackerURL, encodedInfoHash, encodedPeerId, length);
        // URL url = new URL(urlString);
        return HttpRequest.newBuilder().GET().uri(URI.create(urlString)).build();
    }

    private static void printResponse(Object trackerResponse) {
        if (Objects.requireNonNull(trackerResponse) instanceof Map<?, ?> m) {
            var interval = (long) m.get("interval");
            var peers = (String) m.get("peers");
            var buffer = ByteBuffer.wrap(peers.getBytes(StandardCharsets.ISO_8859_1));
            while (buffer.hasRemaining()) {
                int ip = buffer.getInt();
                short port = buffer.getShort();
                System.out.printf("%d.%d.%d.%d:%d%n", (ip >> 24) & 0xff,
                        (ip >> 16) & 0xff, (ip >> 8) & 0xff, ip & 0xff,
                        port & 0xffff);
            }
        } else {
            throw new IllegalStateException("Unexpected value: " + trackerResponse);
        }
    }

    private static Info getInfoFromDict(Map<?, ?> m) {
        assert m.containsKey("announce") && m.containsKey("info");
        var trackerUrl = (String) m.get("announce");
        var infoDict = (Map<?, ?>) m.get("info");
        var length = (Long) infoDict.get("length");
        var infoHash = calculateHash(infoDict);
        return new Info(trackerUrl, infoDict, length, infoHash);
    }

    private record Info(String trackerUrl, Map<?, ?> infoDict, Long length,
                        byte[] infoHash) {
    }

    private static byte[] getFileBytes(Path filePath) throws IOException {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new RuntimeException("not a file.");
        }
        return Files.readAllBytes(filePath);
    }

    private static byte[] calculateHash(Map<?, ?> infoDict) {

        var stringEncoded = BencodeEncoder.encodeToByteBuff(infoDict);
        try {
            var sha1Digest = MessageDigest.getInstance("SHA-1");
            sha1Digest.update(stringEncoded);
            return sha1Digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
