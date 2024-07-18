import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
// import com.dampcake.bencode.Bencode; - available if you need it!

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        String command = args[0];
        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            Object decoded;
            try {
                decoded = decodeBencode(bencodedValue);
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
            System.out.println(gson.toJson(decoded));

        } else {
            System.out.println("Unknown command: " + command);
        }

    }

    static Object decodeBencode(String bencodedString) {
        if (Character.isDigit(bencodedString.charAt(0))) {
            return decodeString(bencodedString);
        } else if (bencodedString.charAt(0) == 'i') {
            return decodeInteger(bencodedString);
        } else if (bencodedString.charAt(0) == 'l') {
            return decodeList(bencodedString);
        } else {
            throw new RuntimeException("Only strings are supported at the moment");
        }
    }

    static String decodeString(String bencodedString) {
        int firstColonIndex = 0;
        for (int i = 0; i < bencodedString.length(); i++) {
            if (bencodedString.charAt(i) == ':') {
                firstColonIndex = i;
                break;
            }
        }
        int length = Integer.parseInt(bencodedString.substring(0, firstColonIndex));
        return bencodedString.substring(firstColonIndex + 1, firstColonIndex + 1 + length);

    }

    static long decodeInteger(String bencodedString) {
        int length = bencodedString.length() - 1;
        return Long.parseLong(bencodedString.substring(1, length));
    }

    static List<Object> decodeList(String bencodedString) {
        List<Object> list = new ArrayList<>();
        String element = bencodedString.substring(1, bencodedString.length() - 1);
//        System.out.println("Changes in element : " + element);
//        System.out.println("Bencoded String :  " + bencodedString);
        if (element.length() == 0) {
            return list;
        }

        while (element.length() > 0) {
            //System.out.println(list);
            if (Character.isDigit(element.charAt(0))) {
                String temp = element.substring(0, element.charAt(0) + 1);
                list.add(decodeString(temp));
                System.out.println("IS Word : + " + list);
                element = element.replaceFirst(temp, "");
            } else if (element.charAt(0) == 'i') {
                String temp = element.substring(0, element.indexOf('e'));
                list.add(decodeInteger(temp));
                System.out.println("IS Integer : + " + list);

                element = element.replaceFirst(temp, "");
            }


        }
        return list;
    }


}
