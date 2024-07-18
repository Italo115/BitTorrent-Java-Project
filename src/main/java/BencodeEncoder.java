import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BencodeEncoder {
    static Object decode(String bencodedString)
            throws UnsupportedEncodingException {
        byte[] data = bencodedString.getBytes(StandardCharsets.UTF_8);
        return new Decoder().decode(data);
    }

    static Object decode(byte[] data) {
        return new Decoder().decode(data);
    }

    public static byte[] encodeToByteBuff(Object obj) {
        switch (obj) {
            case Integer i -> {
                String repr = String.valueOf(i);
                int len = repr.length();
                byte[] bytes = new byte[len + 2];
                int k = 1;
                bytes[0] = 'i';
                for (char c : repr.toCharArray()) {
                    bytes[k++] = (byte) c;
                }
                bytes[len - 1] = 'e';
                return bytes;
            }
            case Long l -> {
                return String.format("i%de", l).getBytes(StandardCharsets.ISO_8859_1);
            }
            case List<?> lst -> {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append('l');
                for (var elem : lst) {
                    stringBuilder.append(new String(encodeToByteBuff(elem),
                            StandardCharsets.ISO_8859_1));
                }
                stringBuilder.append('e');
                return stringBuilder.toString().getBytes(
                        StandardCharsets.ISO_8859_1);
            }
            case String str -> {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str.length());
                stringBuilder.append(':');
                var prefix = stringBuilder.toString().getBytes(StandardCharsets.ISO_8859_1);
                var stringBytes = str.getBytes(StandardCharsets.ISO_8859_1);
                byte[] buff = new byte[prefix.length + stringBytes.length];
                System.arraycopy(prefix, 0, buff, 0, prefix.length);
                System.arraycopy(stringBytes, 0, buff, prefix.length, stringBytes.length);
                return buff;
            }
            case Map<?, ?> m -> {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append('d');
                m.keySet().stream().map(k -> (String) k).sorted().forEachOrdered(k -> {
                    stringBuilder.append(new String(encodeToByteBuff(k), StandardCharsets.ISO_8859_1));
                    stringBuilder.append(new String(encodeToByteBuff(m.get(k)), StandardCharsets.ISO_8859_1));
                });
                stringBuilder.append('e');
                return stringBuilder.toString().getBytes(StandardCharsets.ISO_8859_1);
            }
            case null, default -> {
                throw new RuntimeException("encode: unrecognized type.");
            }
        }
    }

    static String encode(Object obj) {
        switch (obj) {
            case Integer i -> {
                return String.format("i%de", i);
            }
            case Long l -> {
                return String.format("i%de", l);
            }
            case List<?> lst -> {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append('l');
                for (var elem : lst) {
                    stringBuilder.append(encode(elem));
                }
                stringBuilder.append('e');
                return stringBuilder.toString();
            }
            case String str -> {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str.length());
                stringBuilder.append(':');
                for (byte b : str.getBytes(StandardCharsets.ISO_8859_1)) {
                    stringBuilder.append((char) b);
                }
                // return String.format("%d:%s", str.length(), str);
                return stringBuilder.toString();
            }
            case Map<?, ?> m -> {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append('d');
                m.keySet().stream().map(k -> (String) k).sorted().forEachOrdered(k -> {
                    stringBuilder.append(encode(k));
                    stringBuilder.append(encode(m.get(k)));
                });
                stringBuilder.append('e');
                return stringBuilder.toString();
            }
            case null, default -> {
                throw new RuntimeException("encode: unrecognized type.");
            }
        }
    }

    static class Decoder {
        int index;

        public Decoder() {
            this.index = 0;
        }

        public Object decode(byte[] data) {
            char c = (char) data[index];
            if (Character.isDigit(c)) {
                //string
                return decodeString(data);
            } else if (c == 'l') {
                return decodeList(data);
            } else if (c == 'i') {
                return decodeInteger(data);
            } else if (c == 'd') {
                return decodeDictionary(data);
            } else {
                throw new RuntimeException("unrecognized type.");
            }
        }

        public String decodeString(byte[] data) {
            int len = 0;
            while (data[index] != ':') {
                len = 10 * len + ((char) data[index]) - '0';
                index++;
            }
            index++;
            byte[] strBytes = new byte[len];
            for (int i = 0; i < len; ++i) {
                strBytes[i] = data[index++];
            }
            return new String(strBytes, StandardCharsets.ISO_8859_1);
        }

        public List<Object> decodeList(byte[] data) {
            assert data[index] == 'l';
            index++;
            List<Object> list = new ArrayList<>();
            while (data[index] != 'e') {
                list.add(decode(data));
            }
            index++;
            return list;
        }

        public Long decodeInteger(byte[] data) {
            assert data[index] == 'i';
            index++;
            StringBuilder stringBuilder = new StringBuilder();
            while (data[index] != 'e') {
                stringBuilder.append((char) data[index++]);
            }
            index++;
            return Long.parseLong(stringBuilder.toString());
        }

        public Map<String, Object> decodeDictionary(byte[] data) {
            assert data[index] == 'd';
            Map<String, Object> dictionary = new HashMap<>();
            index++;
            while (data[index] != 'e') {
                String key = decodeString(data);
                Object value = decode(data);
                dictionary.put(key, value);
            }
            index++;
            return dictionary;
        }
    }
}