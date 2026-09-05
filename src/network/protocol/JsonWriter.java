package network.protocol;

import java.util.List;
import java.util.Map;

public final class JsonWriter {

    private JsonWriter() {
    }

    public static String write(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(value, builder);
        return builder.toString();
    }

    private static void writeValue(Object value, StringBuilder builder) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String) {
            writeString((String) value, builder);
        } else if (value instanceof Boolean) {
            builder.append(value.toString());
        } else if (value instanceof Number) {
            builder.append(value.toString());
        } else if (value instanceof Map<?, ?>) {
            writeObject((Map<?, ?>) value, builder);
        } else if (value instanceof List<?>) {
            writeArray((List<?>) value, builder);
        } else if (value instanceof Enum<?>) {
            writeString(((Enum<?>) value).name(), builder);
        } else {
            writeString(value.toString(), builder);
        }
    }

    private static void writeObject(Map<?, ?> map, StringBuilder builder) {
        builder.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            writeString(String.valueOf(entry.getKey()), builder);
            builder.append(':');
            writeValue(entry.getValue(), builder);
        }
        builder.append('}');
    }

    private static void writeArray(List<?> list, StringBuilder builder) {
        builder.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            writeValue(item, builder);
        }
        builder.append(']');
    }

    private static void writeString(String text, StringBuilder builder) {
        builder.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        builder.append('"');
    }
}
