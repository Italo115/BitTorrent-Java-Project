import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BencodeDecoder {
    private byte[] encodedValue;
    private int current = 0;

    public BencodeDecoder(byte[] encodedValue) {
        this(encodedValue, 0);
    }

    private BencodeDecoder(byte[] encodedValue, int current) {
        this.encodedValue = encodedValue;
        this.current = current;
    }

    public Object decode() {
        if (Character.isDigit((char) encodedValue[current]))
            return decodeString();
        if (encodedValue[current] == 'i')
            return decodeInteger();
        if (encodedValue[current] == 'l')
            return decodeList();
        if (encodedValue[current] == 'd')
            return decodeDictionary();
        return null;
    }

    private String decodeString() {
        int delimiterIndex = 0;
        for (int i = current; i < encodedValue.length; i++) {
            if (encodedValue[i] == ':') {
                delimiterIndex = i;
                break;
            }
        }
        int length = Integer.parseInt(new String(encodedValue, current, delimiterIndex - current));
        int start = delimiterIndex + 1, end = start + length;
        current = end;
        return new String(encodedValue, start, length);
    }

    private Long decodeInteger() {
        int start = current + 1, end = 0;
        for (int i = start; i < encodedValue.length; i++) {
            if (encodedValue[i] == 'e') {
                end = i;
                break;
            }
        }
        current = end + 1;
        return Long.parseLong(new String(encodedValue, start, end - start));
    }

    private List<Object> decodeList() {
        List<Object> list = new ArrayList<>();
        current++;
        while (encodedValue[current] != 'e') {
            list.add(decode());
        }
        current++;
        return list;
    }

    private Map<String, Object> decodeDictionary() {
        Map<String, Object> map = new HashMap<>();
        current++;
        while (encodedValue[current] != 'e') {
            String key = (String) decode();
            Object value = decode();
            map.put(key, value);
        }
        current++;
        return map;
    }

    public static byte[] readFile(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
    }

    public static byte[] bencode(Map<String, Object> dictionary) {
        StringBuilder sb = new StringBuilder();
        bencodeHelper(dictionary, sb);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void bencodeHelper(Object obj, StringBuilder sb) {
        if (obj instanceof String) {
            String str = (String) obj;
            sb.append(str.length()).append(':').append(str);
        } else if (obj instanceof Long) {
            sb.append('i').append(obj).append('e');
        } else if (obj instanceof List) {
            sb.append('l');
            for (Object item : (List<?>) obj) {
                bencodeHelper(item, sb);
            }
            sb.append('e');
        } else if (obj instanceof Map) {
            sb.append('d');
            List<String> keys = new ArrayList<>(((Map<String, Object>) obj).keySet());
            Collections.sort(keys);
            for (String key : keys) {
                bencodeHelper(key, sb);
                bencodeHelper(((Map<String, Object>) obj).get(key), sb);
            }
            sb.append('e');
        }
    }

    public static void printTorrentInfo(Map<String, Object> decodedDictionary) throws NoSuchAlgorithmException {
        String announce = (String) decodedDictionary.get("announce");
        Map<String, Object> info = (Map<String, Object>) decodedDictionary.get("info");
        Long length = (Long) info.get("length");
        System.out.println("Info goes here : ----- " + info.entrySet());
        System.out.println("Tracker URL: " + announce);
        System.out.println("Length: " + length);

        byte[] bencodedInfo = bencode(info);
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        byte[] infoHash = sha1Digest.digest(bencodedInfo);

        System.out.println("Info Hash: " + bytesToHex(infoHash));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();

    }
}
