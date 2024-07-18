package client;

import bencode.Bdecoder;
import bencode.types.Bencoded;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import utils.Utils;

public class Client {
    private static final Pattern pattern =
            Pattern.compile("^([0-9]|[a-zA-Z]|-|_|~)$");

    @NotNull
    public List<InetSocketAddress> discoverPeers(@NotNull Request request)
            throws IOException, URISyntaxException {
        var requestUrl = buildRequestUrl(request);
        var response = sendGetRequest(requestUrl);
        return parseResponse(response);
    }

    @NotNull
    private List<InetSocketAddress> parseResponse(@NotNull Object response)
            throws UnknownHostException {
        var peersBytes = Utils.requireBytes(
                Utils.requireMap(response, "response").get("peers"), "peers");
        return parsePeers(peersBytes);
    }

    @NotNull
    private List<InetSocketAddress> parsePeers(byte @NotNull [] peersBytes)
            throws UnknownHostException {
        if (peersBytes.length % 6 != 0) {
            throw new IllegalArgumentException("peersBytes must be a multiple of 6");
        }
        var result = new ArrayList<InetSocketAddress>();
        for (int i = 0; i < peersBytes.length; i += 6) {
            result.add(parsePeerAddress(peersBytes, i));
        }
        return result;
    }

    @NotNull
    private static InetSocketAddress parsePeerAddress(byte @NotNull [] peersBytes,
                                                      int i)
            throws UnknownHostException {
        var addr =
                InetAddress.getByAddress(Arrays.copyOfRange(peersBytes, i, i + 4));
        int port = ((int) peersBytes[i + 5] & 0x000000FF) |
                (((int) peersBytes[i + 4] & 0x000000FF) << 8);
        return new InetSocketAddress(addr, port);
    }

    @NotNull
    private URL buildRequestUrl(@NotNull Request request)
            throws IOException, URISyntaxException {
        var urlS = String.format(
                "%s?info_hash=%s&peer_id=%s&port=%d&uploaded=%d&downloaded=%d&left=%d&compact=%d",
                request.url(), convertHexStringToASCII(request.infoHash()),
                request.peerId(), request.port(), request.uploaded(),
                request.downloaded(), request.left(), request.compact());
        return new URI(urlS).toURL();
    }

    @NotNull
    private Object sendGetRequest(@NotNull URL url) throws IOException {
        if (!(url.openConnection() instanceof HttpURLConnection connection)) {
            throw new IOException(
                    String.format("%s is not a valid HTTP connection", url));
        }
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("GET request failed with code " + responseCode);
        }
        try (var stream = new BufferedInputStream(connection.getInputStream())) {
            return new Bdecoder(stream).decode().unwrapped();
        }
    }

    @NotNull
    private static String convertHexStringToASCII(@NotNull String hex) {
        var output = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            var eq = (char) Integer.parseInt(hex, i, i + 2, 16);
            if (isUnreserved(eq)) {
                output.append(eq);
            } else {
                output.append("%").append(hex, i, i + 2);
            }
        }
        return output.toString();
    }

    private static boolean isUnreserved(@NotNull Character ch) {
        return pattern.matcher(ch.toString()).find();
    }
}