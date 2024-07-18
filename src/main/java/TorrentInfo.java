import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

class TorrentInfo {
    public String announce;
    public long length;
    public byte[] infoHash;
    public long pieceLength;
    public byte[] pieceHashes;

    @SuppressWarnings("unchecked")
    public TorrentInfo(byte[] bytes) throws NoSuchAlgorithmException {
        Bencode bencode1 = new Bencode(false);
        Bencode bencode2 = new Bencode(true);
        Map<String, Object> root = bencode1.decode(bytes, Type.DICTIONARY);
        Map<String, Object> info = (Map<String, Object>) root.get("info");
        announce = (String) root.get("announce");
        length = (long) info.get("length");
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        infoHash = digest.digest(bencode2.encode((Map<String, Object>) bencode2.decode(bytes, Type.DICTIONARY).get("info")));
        pieceLength = (long) info.get("piece length");

        int i = 0;
        while (i < ((byte[]) info.get("pieces")).length) {
            pieceHashes =
                    Arrays.copyOfRange((byte[]) info.get("pieces"), i, i + 20);
            System.out.print(bytesToHex(pieceHashes));
            i += 20;
            if (i < ((byte[]) info.get("pieces")).length)
                System.out.println();
        }

    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}