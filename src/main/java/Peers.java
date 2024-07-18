import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Peers {
    private final TorrentInfo torrent;

    public Peers(TorrentInfo torrent) {
        this.torrent = torrent;
    }

    public void discoverPeers() throws Exception {
        String infoHash = urlEncode(torrent.infoHash);
        String peerId = "00112233445566778899";
        int port = 6881;
        int uploaded = 0;
        int downloaded = 0;
        long left = torrent.length;
        int compact = 1;

        String urlStr = String.format("%s?info_hash=%s&peer_id=%s&port=%d&uploaded=%d&downloaded=%d&left=%d&compact=%d",
                torrent.announce, infoHash, peerId, port, uploaded, downloaded, left, compact);

        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            Bencode bencode = new Bencode(false);
            Map<String, Object> responseMap = bencode.decode(response.toString().getBytes(StandardCharsets.ISO_8859_1), Type.DICTIONARY);
            System.out.println("Response: " + responseMap);

            byte[] peers = ((String) responseMap.get("peers")).getBytes(StandardCharsets.ISO_8859_1);
            for (int i = 0; i < peers.length; i += 6) {
                String ip = String.format("%d.%d.%d.%d", (peers[i] & 0xff), (peers[i + 1] & 0xff), (peers[i + 2] & 0xff), (peers[i + 3] & 0xff));
                int peerPort = ((peers[i + 4] & 0xff) << 8) | (peers[i + 5] & 0xff);
                System.out.println("Peer: " + ip + ":" + peerPort);
            }
        } else {
            System.out.println("Failed to get response from tracker");
        }
    }

    private String urlEncode(byte[] bytes) {
        StringBuilder encoded = new StringBuilder();
        for (byte b : bytes) {
            if ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || (b >= '0' && b <= '9') || b == '-' || b == '_' || b == '.' || b == '~') {
                encoded.append((char) b);
            } else {
                encoded.append(String.format("%%%02X", b));
            }
        }
        return encoded.toString();
    }
}
