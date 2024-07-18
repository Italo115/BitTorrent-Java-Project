import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BencodeEncoder {

    static Object decode(String bencodedString) throws UnsupportedEncodingException {
        return new Decoder().decode(bencodedString.getBytes(StandardCharsets.UTF_8));
    }

    static Object decode(byte[] data) {
        return new Decoder().decode(data);
    }

    public static byte[] encodeToByteBuff(Object obj) {
        return switch (obj) {
            case Integer i -> String.format("i%de", i).getBytes(StandardCharsets.ISO_8859_1);
            case Long l -> String.format("i%de", l).getBytes(StandardCharsets.ISO_8859_1);
            case String str -> {
                byte[] strBytes = str.getBytes(StandardCharsets.ISO_8859_1);
                yield (str.length() + ":" + str).getBytes(StandardCharsets.ISO_8859_1);
            }
            case List<?> lst -> {
                StringBuilder sb = new StringBuilder("l");
                for (var elem : lst) sb.append(new String(encodeToByteBuff(elem), StandardCharsets.ISO_8859_1));
                sb.append('e');
                yield sb.toString().getBytes(StandardCharsets.ISO_8859_1);
            }
            case Map<?, ?> m -> {
                StringBuilder sb = new StringBuilder("d");
                m.keySet().stream().map(String.class::cast).sorted().forEach(k -> {
                    sb.append(new String(encodeToByteBuff(k), StandardCharsets.ISO_8859_1));
                    sb.append(new String(encodeToByteBuff(m.get(k)), StandardCharsets.ISO_8859_1));
                });
                sb.append('e');
                yield sb.toString().getBytes(StandardCharsets.ISO_8859_1);
            }
            default -> throw new RuntimeException("encode: unrecognized type.");
        };
    }

    static String encode(Object obj) {
        return switch (obj) {
            case Integer i -> String.format("i%de", i);
            case Long l -> String.format("i%de", l);
            case String str -> str.length() + ":" + str;
            case List<?> lst -> {
                StringBuilder sb = new StringBuilder("l");
                for (var elem : lst) sb.append(encode(elem));
                sb.append('e');
                yield sb.toString();
            }
            case Map<?, ?> m -> {
                StringBuilder sb = new StringBuilder("d");
                m.keySet().stream().map(String.class::cast).sorted().forEach(k -> {
                    sb.append(encode(k));
                    sb.append(encode(m.get(k)));
                });
                sb.append('e');
                yield sb.toString();
            }
            default -> throw new RuntimeException("encode: unrecognized type.");
        };
    }

    static class Decoder {
        private int index;

        public Object decode(byte[] data) {
            return switch (data[index]) {
                case 'i' -> decodeInteger(data);
                case 'l' -> decodeList(data);
                case 'd' -> decodeDictionary(data);
                default -> Character.isDigit(data[index]) ? decodeString(data) : throw new RuntimeException("unrecognized type.");
            };
        }

        private String decodeString(byte[] data) {
            int len = 0;
            while (data[index] != ':') len = len * 10 + data[index++] - '0';
            byte[] strBytes = new byte[len];
            System.arraycopy(data, ++index, strBytes, 0, len);
            index += len;
            return new String(strBytes, StandardCharsets.ISO_8859_1);
        }

        private List<Object> decodeList(byte[] data) {
            List<Object> list = new ArrayList<>();
            while (data[++index] != 'e') list.add(decode(data));
            index++;
            return list;
        }

        private Long decodeInteger(byte[] data) {
            int start = ++index;
            while (data[index] != 'e') index++;
            return Long.parseLong(new String(data, start, index++ - start));
        }

        private Map<String, Object> decodeDictionary(byte[] data) {
            Map<String, Object> dictionary = new HashMap<>();
            while (data[++index] != 'e') dictionary.put(decodeString(data), decode(data));
            index++;
            return dictionary;
        }
    }
}
