import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class TorrentInfo {
    public String announce;
    public long length;
    public byte[] infoHash;
    public long pieceLength;
    public List<byte[]> pieceHashes = new ArrayList<>();

    public TorrentInfo(byte[] bytes) throws Exception {
        Bencode bencode1 = new Bencode(false);
        Bencode bencode2 = new Bencode(true);
        Map<String, Object> root = bencode1.decode(bytes, Type.DICTIONARY);
        Map<String, Object> info = (Map<String, Object>) root.get("info");
        announce = (String) root.get("announce");
        length = (long) info.get("length");
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        infoHash = digest.digest(bencode2.encode((Map<String, Object>) bencode2.decode(bytes, Type.DICTIONARY).get("info")));
        pieceLength = (long) info.get("piece length");
        byte[] pieceHashes = ((ByteBuffer) ((Map<String, Object>) bencode2.decode(bytes, Type.DICTIONARY).get("info")).get("pieces")).array();
        for (int i = 0; i < pieceHashes.length; i += 20) {
            byte[] pieceHash = Arrays.copyOfRange(pieceHashes, i, i + 20);
            this.pieceHashes.add(pieceHash);
        }
    }
}


