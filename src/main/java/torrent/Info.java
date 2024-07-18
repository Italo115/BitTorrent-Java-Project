package torrent;

import java.util.List;
import java.util.Objects;

public record Info(long length, String name, long pieceLength, byte[] pieces,
                   List<String> pieceHashes) {
    public Info {
        // Validate that the name is not null or empty
        Objects.requireNonNull(name, "name must not be null");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        // Validate that the length is non-negative
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative");
        }
        // Validate that the pieceLength is positive
        if (pieceLength <= 0) {
            throw new IllegalArgumentException("pieceLength must be positive");
        }
        // Validate that pieces is not null
        Objects.requireNonNull(pieces, "pieces must not be null");
        Objects.requireNonNull(pieceHashes);
    }
}