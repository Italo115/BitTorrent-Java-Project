import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        String command = args[0];
        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            Object decoded;
            try {
                BencodeDecoder decoder = new BencodeDecoder(bencodedValue.getBytes(StandardCharsets.UTF_8));
                decoded = decoder.decode();
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
            System.out.println(gson.toJson(decoded));
        } else if ("info".equals(command)) {
            String torrentFilePath = args[1];
            byte[] torrentData;
            try {
                torrentData = readFile(torrentFilePath);
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
                return;
            }
            BencodeDecoder decoder = new BencodeDecoder(torrentData);
            Map<String, Object> decodedDictionary;
            try {
                decodedDictionary = (Map<String, Object>) decoder.decode();
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
            printTorrentInfo(decodedDictionary);
        } else {
            System.out.println("Unknown command: " + command);
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
