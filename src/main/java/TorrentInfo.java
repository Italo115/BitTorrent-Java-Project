import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class TorrentInfo {
    public String announce;
    public long length;
    public byte[] infoHash;
    public long pieceLength;
    public List<String> pieceHashes;

    @SuppressWarnings("unchecked")
    public TorrentInfo(byte[] bytes) throws NoSuchAlgorithmException {
        Bencode bencode = new Bencode(false);
        Map<String, Object> root = bencode.decode(bytes, Type.DICTIONARY);
        Map<String, Object> info = (Map<String, Object>) root.get("info");

        announce = (String) root.get("announce");
        length = ((Number) info.get("length")).longValue();
        pieceLength = ((Number) info.get("piece length")).longValue();

        String piecesString = (String) info.get("pieces");
        byte[] piecesBytes = piecesString.getBytes();
        pieceHashes = new ArrayList<>();
        for (int i = 0; i < piecesBytes.length; i += 20) {
            byte[] hash = Arrays.copyOfRange(piecesBytes, i, i + 20);
            pieceHashes.add(bytesToHex(hash));
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        infoHash = digest.digest(bencode.encode((Map<String, Object>) info));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
