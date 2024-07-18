import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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

    public static void main(String[] args) {
        if (args.length != 2 || !args[0].equals("info")) {
            System.out.println("Usage: ./your_bittorrent.sh info <torrent file>");
            return;
        }

        String torrentFilePath = args[1];
        try {
            byte[] torrentData = readFile(torrentFilePath);
            BencodeDecoder decoder = new BencodeDecoder(torrentData);
            Map<String, Object> decodedDictionary = (Map<String, Object>) decoder.decode();
            printTorrentInfo(decodedDictionary);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] readFile(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
    }

    private static void printTorrentInfo(Map<String, Object> decodedDictionary) {
        String announce = (String) decodedDictionary.get("announce");
        Map<String, Object> info = (Map<String, Object>) decodedDictionary.get("info");
        Long length = (Long) info.get("length");

        System.out.println("Tracker URL: " + announce);
        System.out.println("Length: " + length);
    }
}
