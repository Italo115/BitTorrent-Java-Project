package client;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.net.URL;

@RecordBuilder
public record Request(URL url, String infoHash, String peerId, int port,
                      long uploaded, long downloaded, long left, int compact) {
    public Request {
        // Validate that url is not null
        if (url == null) {
            throw new IllegalArgumentException("url must not be null or empty");
        }
        // Validate that infoHash is not null
        if (infoHash == null) {
            throw new IllegalArgumentException("infoHash must not be null");
        }
        // Validate that peerId is not null and has a length of 20 characters
        if (peerId == null || peerId.length() != 20) {
            throw new IllegalArgumentException(
                    "peerId must not be null and must be 20 characters long");
        }
        // Validate that port is within valid range
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        // Validate that uploaded, downloaded, and left are non-negative
        if (uploaded < 0) {
            throw new IllegalArgumentException("uploaded must be non-negative");
        }
        if (downloaded < 0) {
            throw new IllegalArgumentException("downloaded must be non-negative");
        }
        if (left < 0) {
            throw new IllegalArgumentException("left must be non-negative");
        }
        // Validate that compact is 0 or 1
        if (compact < 0 || compact > 1) {
            throw new IllegalArgumentException("compact must be either 0 or 1");
        }
    }
}