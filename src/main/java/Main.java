import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        String command = args[0];
        String bencodedValue = args[1];
        BencodeDecoder decoder = new BencodeDecoder(bencodedValue.getBytes(StandardCharsets.UTF_8));
        if ("decode".equals(command)) {
            Object decoded;
            try {

                decoded = decoder.decode();
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
            System.out.println(gson.toJson(decoded));
        } else if ("info".equals(command)) {
            byte[] torrentData = BencodeDecoder.readFile(args[1]);
            decoder = new BencodeDecoder(torrentData);
            Map<String, Object> decodedDictionary = (Map<String, Object>) decoder.decode();
            BencodeDecoder.printTorrentInfo(decodedDictionary);
        } else {
            System.out.println("Unknown command: " + command);
        }
    }
}