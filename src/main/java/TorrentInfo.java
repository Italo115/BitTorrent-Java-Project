import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TorrentInfo {

    @SuppressWarnings("unchecked")
    public static void printTorrentInfo(Map<String, Object> decodedDictionary) throws NoSuchAlgorithmException, IOException {
        String announce = (String) decodedDictionary.get("announce");
        Map<String, Object> info = (Map<String, Object>) decodedDictionary.get("info");
        Long length = (Long) info.get("length");

        System.out.println("Tracker URL: " + announce);
        System.out.println("Length: " + length);

        byte[] bencodedInfo = bencode(info);
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        byte[] infoHash = sha1Digest.digest(bencodedInfo);

        System.out.println("Info Hash: " + bytesToHex(infoHash));
    }

    private static byte[] bencode(Map<String, Object> dictionary) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bencodeHelper(dictionary, baos);
        return baos.toByteArray();
    }

    private static void bencodeHelper(Object obj, ByteArrayOutputStream baos) throws IOException {
        if (obj instanceof String) {
            String str = (String) obj;
            baos.write(String.valueOf(str.length()).getBytes(StandardCharsets.UTF_8));
            baos.write(':');
            baos.write(str.getBytes(StandardCharsets.UTF_8));
        } else if (obj instanceof Long) {
            baos.write('i');
            baos.write(String.valueOf(obj).getBytes(StandardCharsets.UTF_8));
            baos.write('e');
        } else if (obj instanceof List) {
            baos.write('l');
            for (Object item : (List<?>) obj) {
                bencodeHelper(item, baos);
            }
            baos.write('e');
        } else if (obj instanceof Map) {
            baos.write('d');
            List<String> keys = new ArrayList<>(((Map<String, Object>) obj).keySet());
            Collections.sort(keys);
            for (String key : keys) {
                bencodeHelper(key, baos);
                bencodeHelper(((Map<String, Object>) obj).get(key), baos);
            }
            baos.write('e');
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
