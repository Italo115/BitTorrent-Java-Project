import client.Client;
import client.Request;
import client.RequestBuilder;
import com.google.gson.Gson;
import bencode.Bdecoder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import org.jetbrains.annotations.NotNull;
import torrent.TorrentBuilder;


public class Main {
    private static final Gson gson = new Gson();
    public static void main(String @NotNull[] args) throws Exception {
        String command = args[0];
        switch (command) {case "decode" -> decode(args);
            case "info" -> info(args);case "peers" -> peers(args);
            default -> System.out.println("Unknown command: " + command);
        }
    }
    private static void decode(String @NotNull [] args) throws IOException {
        try (var stream = new ByteArrayInputStream(args[1].getBytes())) {
            var decoded = new Bdecoder(stream).decode();
            System.out.println(gson.toJson(decoded.toJson()));
        } catch(RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }private static void info(String @NotNull [] args) throws IOException, NoSuchAlgorithmException, URISyntaxException {
        var torrent = TorrentBuilder.fromFile(args[1]);
        System.out.println("Tracker URL: " + torrent.announce().toString());
        System.out.println("Length: " + torrent.info().length());
        System.out.println("Info Hash: " + torrent.infoHash());
        System.out.println("Piece Length: " + torrent.info().pieceLength());
        System.out.println("Piece Hashes:"); torrent.info().pieceHashes().forEach(System.out::println);private static void peers(String @NotNull [] args) throws IOException, URISyntaxException, NoSuchAlgorithmException
        {
            var torrent = TorrentBuilder.fromFile(args[1]);
            var request = RequestBuilder.builder()
                    .url(torrent.announce())
                    .infoHash(torrent.infoHash())
                    .peerId("00112233445566778899")
                    .port(6881)
                    .uploaded(0)
                    .downloaded(0)
                    .left(torrent.info().length())
                    .compact(1)
                    .build();
            var peers = new Client().discoverPeers(request);
            peers.stream().map(addr -> addr.toString().substring(1)).forEach(System.out::println);
        }
    }