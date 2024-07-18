import com.google.gson.Gson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
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

// import com.dampcake.bencode.Bencode; //- available if you need it!
public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        // You can use print statements as follows for debugging, they'll be visible
        // when running tests.
        // System.out.println("Logs from your program will appear here!");
        String command = args[0];
        processCommand(command, args[1]);
    }

    private static void printPieceHashes(Map<?, ?> infoDict) {
        var data = (String) infoDict.get("pieces");
        var bytes = data.getBytes(StandardCharsets.ISO_8859_1);
        System.out.print("Piece Hashes:");
        for (int i = 0; i < bytes.length; ++i) {
            if (i % 20 == 0) {
                System.out.println();
            }
            System.out.printf("%02x", bytes[i]);
        }
    }

    private static String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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

    private static HttpRequest createFirstRequest(String trackerURL, long length,
                                                  byte[] infoHash)
            throws URISyntaxException, UnsupportedEncodingException,
            MalformedURLException {
        String strInfoHash = new String(infoHash, StandardCharsets.ISO_8859_1);
        String encodedInfoHash =
                URLEncoder.encode(strInfoHash, StandardCharsets.ISO_8859_1);
        String encodedPeerId =
                URLEncoder.encode("00112233445566778899", StandardCharsets.ISO_8859_1);
        String urlString = String.format(
                "%s?info_hash=%s&peer_id=%s&port=6881&uploaded=0&downloaded=0&left=%d&compact=1",
                trackerURL, encodedInfoHash, encodedPeerId, length);
        // URL url = new URL(urlString);
        return HttpRequest.newBuilder().GET().uri(URI.create(urlString)).build();
    }

    private static void processCommand(String command, String arg)
            throws IOException, URISyntaxException, InterruptedException {
        switch (command) {
            case "info": {
                Path filePath = Path.of(arg);
                var fileBytes = getFileBytes(filePath);
                Object decoded = BencodeEncoder.decode(fileBytes);
                if (Objects.requireNonNull(decoded) instanceof Map<?, ?> m) {
                    Info result = getInfoFromDict(m);
                    System.out.printf("Tracker URL: %s\n", result.trackerUrl());
                    System.out.printf("Length: %d\n", result.length());
                    System.out.printf("Info Hash: %s\n", toHexString(result.infoHash()));
                    System.out.printf("Piece Length: %d\n",
                            (long) result.infoDict().get("piece length"));
                    printPieceHashes(result.infoDict());
                } else {
                    throw new IllegalStateException("Unexpected value: " + decoded);
                }
            }
            break;
            case "decode": {
                Object decoded;
                try {
                    decoded = BencodeEncoder.decode(arg);
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                    return;
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(gson.toJson(decoded));
                break;
            }
            case "peers": {
                Path filePath = Path.of(arg);
                var fileBytes = getFileBytes(filePath);
                Object decoded = BencodeEncoder.decode(fileBytes);
                if (Objects.requireNonNull(decoded) instanceof Map<?, ?> m) {
                    Info info = getInfoFromDict(m);
                    var httpGetRequest =
                            createFirstRequest(info.trackerUrl, info.length, info.infoHash);
                    System.out.println(httpGetRequest);
                    var response = HttpClient.newHttpClient().send(
                            httpGetRequest, HttpResponse.BodyHandlers.ofByteArray());
                    var bencodedResponseBytes = response.body();
                    var trackerResponse = BencodeEncoder.decode(bencodedResponseBytes);
                    printResponse(trackerResponse);
                }
            }
            break;
            default:
                throw new NoSuchElementException("Unsupported command.");
        }
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
}
