import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

class TorrentInfo {
    public String announce;
    public long length;
    public byte[] infoHash;

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
    }
}