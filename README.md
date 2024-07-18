# BitTorrent Project in Java

## Completed Elements

### Bencode Decoding
Bencode (pronounced Bee-encode) is a serialization format used in the BitTorrent protocol. It supports four data types: strings, integers, arrays, and dictionaries. The following decoding capabilities have been implemented:

#### Strings
Strings are encoded as `<length>:<contents>`. For example, the string "hello" is encoded as "5:hello".
```sh
$ ./your_bittorrent.sh decode 5:hello
"hello"
```

#### Integers
Integers are encoded as `i<number>e`. For example, 52 is encoded as `i52e` and -52 is encoded as `i-52e`.
```sh
$ ./your_bittorrent.sh decode i52e
52
```

#### Lists
Lists are encoded as `l<bencoded_elements>e`. For example, `["hello", 52]` would be encoded as `l5:helloi52ee`.
```sh
$ ./your_bittorrent.sh decode l5:helloi52ee
["hello",52]
```

#### Dictionaries
A dictionary is encoded as `d<key1><value1>...<keyN><valueN>e`. Keys are sorted in lexicographical order and must be strings. For example, `{"hello": 52, "foo":"bar"}` would be encoded as `d3:foo3:bar5:helloi52ee`.
```sh
$ ./your_bittorrent.sh decode d3:foo3:bar5:helloi52ee
{"foo":"bar","hello":52}
```

### Torrent File Parsing
A torrent file (metainfo file) contains a bencoded dictionary with the following keys and values:
- **announce**: URL to a tracker.
- **info**: A dictionary with keys:
  - **length**: size of the file in bytes.
  - **name**: suggested name to save the file/directory.
  - **piece length**: number of bytes in each piece.
  - **pieces**: concatenated SHA-1 hashes of each piece.

#### Tracker URL and File Length
Extracted tracker URL and the length of the file from the torrent file.
```sh
$ ./your_bittorrent.sh info sample.torrent
Tracker URL: http://bittorrent-test-tracker.codecrafters.io/announce
Length: 92063
```

#### Info Hash
Calculated the info hash of a torrent file and printed it in hexadecimal format.
```sh
$ ./your_bittorrent.sh info sample.torrent
Tracker URL: http://bittorrent-test-tracker.codecrafters.io/announce
Length: 92063
Info Hash: d69f91e6b2ae4c542468d1073a71d4ea13879a7f
```

#### Piece Length and Hashes
Printed the piece length and a list of piece hashes in hexadecimal format.
```sh
$ ./your_bittorrent.sh info sample.torrent
Tracker URL: http://bittorrent-test-tracker.codecrafters.io/announce
Length: 92063
Info Hash: d69f91e6b2ae4c542468d1073a71d4ea13879a7f
Piece Length: 32768
Piece Hashes:
e876f67a2a8886e8f36b136726c30fa29703022d
6e2275e604a0766656736e81ff10b55204ad8d35
f00d937a0213df1982bc8d097227ad9e909acc17
```

### Tracker Communication
Made a GET request to a HTTP tracker to discover peers to download the file from.
```sh
$ ./your_bittorrent.sh peers sample.torrent
178.62.82.89:51470
165.232.33.77:51467
178.62.85.20:51489
```

### Peer Handshake
Established a TCP connection with a peer and completed a handshake.
```sh
$ ./your_bittorrent.sh handshake sample.torrent <peer_ip>:<peer_port>
Peer ID: 0102030405060708090a0b0c0d0e0f1011121314
```

## Future Additions

### Downloading a Piece
Download one piece and save it to disk. Exchange peer messages to download the file:
- Wait for a bitfield message.
- Send an interested message.
- Wait for an unchoke message.
- Send request messages for each block.
- Wait for piece messages and combine blocks into pieces.
- Verify piece integrity using piece hashes.

### Downloading the Entire File
Download the entire file and save it to disk. Use a single peer to download all the pieces, verify their integrity, and combine them to assemble the file.
```sh
$ ./your_bittorrent.sh download -o /tmp/test.txt sample.torrent
Downloaded sample.torrent to /tmp/test.txt.
```

### Optional Improvements
- Pipelining requests to improve download speeds (maintain 5 requests pending at once).
- Downloading from multiple peers at once using a work queue for each piece to be downloaded. Retry failed downloads due to network issues, hash mismatches, or missing pieces.
