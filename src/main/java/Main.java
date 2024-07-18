import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        String command = args[0];
        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            BencodeDecoder decoder = new BencodeDecoder(bencodedValue.getBytes(StandardCharsets.UTF_8));
            Object decoded;
            try {
                decoded = decoder.decode();
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
            System.out.println(gson.toJson(decoded));
        } else if ("info".equals(command)) {
            byte[] res = Files.readAllBytes(Path.of(args[1]));
            TorrentInfo torrent = new TorrentInfo(res);
            System.out.println("Tracker URL: " + torrent.announce);
            System.out.println("Length: " + torrent.length);
            System.out.println("Info Hash: " + bytesToHex(torrent.infoHash));
            System.out.println("Piece Length: " + torrent.pieceLength);
            System.out.println("Piece Hashes:");
            for (String hash : torrent.pieceHashes) {
                System.out.println(hash);
            }
        } else {
            System.out.println("Unknown command: " + command);
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
