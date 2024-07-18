import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BencodeEncoder {

    public static Object decode(String bencodedString) throws UnsupportedEncodingException {
        byte[] data = bencodedString.getBytes(StandardCharsets.UTF_8);
        return new Decoder().decode(data);
    }

    public static Object decode(byte[] data) {
        return new Decoder().decode(data);
    }

    public static byte[] encodeToByteBuff(Object obj) {
        return switch (obj) {
            case Integer i -> String.format("i%de", i).getBytes(StandardCharsets.ISO_8859_1);
            case Long l -> String.format("i%de", l).getBytes(StandardCharsets.ISO_8859_1);
            case String str -> (str.length() + ":" + str).getBytes(StandardCharsets.ISO_8859_1);
            case List<?> lst -> {
                StringBuilder sb = new StringBuilder("l");
                for (Object elem : lst) sb.append(new String(encodeToByteBuff(elem), StandardCharsets.ISO_8859_1));
                sb.append('e');
                yield sb.toString().getBytes(StandardCharsets.ISO_8859_1);
            }
            case Map<?, ?> m -> {
                StringBuilder sb = new StringBuilder("d");
                m.keySet().stream().map(k -> (String) k).sorted().forEach(k -> {
                    sb.append(new String(encodeToByteBuff(k), StandardCharsets.ISO_8859_1));
                    sb.append(new String(encodeToByteBuff(m.get(k)), StandardCharsets.ISO_8859_1));
                });
                sb.append('e');
                yield sb.toString().getBytes(StandardCharsets.ISO_8859_1);
            }
            default -> throw new RuntimeException("encode: unrecognized type.");
        };
    }

    public static String encode(Object obj) {
        return switch (obj) {
            case Integer i -> String.format("i%de", i);
            case Long l -> String.format("i%de", l);
            case String str -> str.length() + ":" + str;
            case List<?> lst -> {
                StringBuilder sb = new StringBuilder("l");
                for (Object elem : lst) sb.append(encode(elem));
                sb.append('e');
                yield sb.toString();
            }
            case Map<?, ?> m -> {
                StringBuilder sb = new StringBuilder("d");
                m.keySet().stream().map(k -> (String) k).sorted().forEach(k -> {
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
        private int index = 0;

        public Object decode(byte[] data) {
            char c = (char) data[index];
            return switch (c) {
                case 'i' -> decodeInteger(data);
                case 'l' -> decodeList(data);
                case 'd' -> decodeDictionary(data);
                default -> Character.isDigit(c) ? decodeString(data) : throw new RuntimeException("unrecognized type.");
            };
        }

        private String decodeString(byte[] data) {
            int len = 0;
            while (data[index] != ':') {
                len = len * 10 + data[index++] - '0';
            }
            index++;
            String str = new String(data, index, len, StandardCharsets.ISO_8859_1);
            index += len;
            return str;
        }

        private List<Object> decodeList(byte[] data) {
            index++;
            List<Object> list = new ArrayList<>();
            while (data[index] != 'e') {
                list.add(decode(data));
            }
            index++;
            return list;
        }

        private Long decodeInteger(byte[] data) {
            index++;
            int start = index;
            while (data[index] != 'e') index++;
            long value = Long.parseLong(new String(data, start, index - start, StandardCharsets.ISO_8859_1));
            index++;
            return value;
        }

        private Map<String, Object> decodeDictionary(byte[] data) {
            index++;
            Map<String, Object> dictionary = new HashMap<>();
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
