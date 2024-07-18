package torrent;

import bencode.Bdecoder;
import bencode.Bencoder;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import utils.Utils;

public class TorrentBuilder {
    TorrentBuilder() {
    }

    @NotNull
    public static Torrent fromFile(@NotNull String name)
            throws IOException, URISyntaxException, NoSuchAlgorithmException {
        try (var stream = new BufferedInputStream(new FileInputStream(name))) {
            var decoded = new Bdecoder(stream).decode().unwrapped();
            if (!(decoded instanceof Map<?, ?> map)) {
                throw new IllegalStateException("Invalid file - must be a dict");
            }
            var announce = new URI(new String(Utils.requireBytes(map.get("announce"),
                    "announce")))
                    .toURL();
            var info = Utils.requireMap(map.get("info"), "info");
            var infoHash = getHexHash(info);
            var pieces = Utils.requireBytes(info.get("pieces"), "pieces");
            var pieceHashes = getPieceHashes(pieces);
            return new Torrent(
                    announce,
                    new Info(Utils.requireLong(info.get("length"), "length"),
                            new String(Utils.requireBytes(info.get("name"), "name")),
                            Utils.requireLong(info.get("piece length"), "piece length"),
                            pieces, pieceHashes),
                    infoHash);
        }
    }

    @NotNull
    private static List<String> getPieceHashes(byte @NotNull [] pieces) {
        if (pieces.length % 20 != 0) {
            throw new IllegalStateException(
                    "Invalid pieces length - must be a multiple of 20");
        }
        var result = new ArrayList<String>(pieces.length / 20);
        for (int i = 0; i < pieces.length; i += 20) {
            result.add(HexUtils.bytesToHex(pieces, i, 20));
        }
        return result;
    }

    @NotNull
    private static String getHexHash(Map<?, ?> info)
            throws IOException, NoSuchAlgorithmException {
        var encoded = new Bencoder().encode(info);
        var hash = HexUtils.sha1(encoded);
        return HexUtils.bytesToHex(hash);
    }
}