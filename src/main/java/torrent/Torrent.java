package torrent;

import java.net.URL;
import java.util.Objects;

public record Torrent(URL announce, Info info, String infoHash) {
    public Torrent {
        Objects.requireNonNull(announce);
        Objects.requireNonNull(info);
        Objects.requireNonNull(infoHash);
    }
}