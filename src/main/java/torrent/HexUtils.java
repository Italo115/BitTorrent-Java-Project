package torrent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jetbrains.annotations.NotNull;

class HexUtils {
    HexUtils() {
    }

    static byte @NotNull [] sha1(byte[] input) throws NoSuchAlgorithmException {
        // Get an instance of the SHA-1 MessageDigest
        var sha1Digest = MessageDigest.getInstance("SHA-1");
        // Update the digest with the input byte array
        sha1Digest.update(input);
        // Compute the SHA-1 hash
        return sha1Digest.digest();
    }

    @NotNull
    static String bytesToHex(byte @NotNull [] bytes) {
        return bytesToHex(bytes, 0, bytes.length);
    }

    // Helper method to convert byte array to a hexadecimal string
    @NotNull
    static String bytesToHex(byte @NotNull [] bytes, int offset, int length) {
        var hexString = new StringBuilder();
        for (int i = offset; i < offset + length; i++) {
            byte b = bytes[i];
            var hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}