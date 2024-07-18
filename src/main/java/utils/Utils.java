package utils;

import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public class Utils {
    public static byte
    @NotNull [] requireBytes(@NotNull Object o, @NotNull String name) {
        Objects.requireNonNull(o, "Object " + name + " cannot be null");
        if (o instanceof byte[] bytes) {
            return bytes;
        }
        throw new IllegalStateException("Invalid " + name +
                " field - must be a string");
    }

    public static long requireLong(@NotNull Object o, @NotNull String name) {
        Objects.requireNonNull(o, "Object " + name + " cannot be null");
        if (o instanceof Long l) {
            return l;
        }
        if (o instanceof Integer i) {
            return i;
        }
        throw new IllegalStateException("Invalid " + name +
                " field - must be a long, got " + o);
    }

    @NotNull
    public static Map<?, ?> requireMap(@NotNull Object o, @NotNull String name) {
        Objects.requireNonNull(o, "Object " + name + " cannot be null");
        if (o instanceof Map<?, ?> map) {
            return map;
        }
        throw new IllegalStateException("Invalid " + name +
                " field - must be a map");
    }
}