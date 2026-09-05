package network.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonParser {

    private final String text;
    private int pos;

    private JsonParser(String text) {
        this.text = text;
    }

    public static Object parse(String text) {
        JsonParser parser = new JsonParser(text);
        parser.skipWhitespace();
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw new IllegalArgumentException("Unexpected trailing content in JSON");
        }
        return value;
    }

    private Object parseValue() {
        char c = peek();
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't', 'f' -> parseBoolean();
            case 'n' -> parseNull();
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObject() {
        expect('{');
        Map<String, Object> result = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            Object value = parseValue();
            result.put(key, value);
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                pos++;
            } else if (next == '}') {
                pos++;
                break;
            } else {
                throw new IllegalArgumentException("Expected ',' or '}' at position " + pos);
            }
        }
        return result;
    }

    private List<Object> parseArray() {
        expect('[');
        List<Object> result = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            result.add(parseValue());
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                pos++;
            } else if (next == ']') {
                pos++;
                break;
            } else {
                throw new IllegalArgumentException("Expected ',' or ']' at position " + pos);
            }
        }
        return result;
    }

    private String parseString() {
        expect('"');
        StringBuilder builder = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw new IllegalArgumentException("Unterminated string");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                if (pos >= text.length()) {
                    throw new IllegalArgumentException("Unterminated escape sequence");
                }
                char escaped = text.charAt(pos++);
                switch (escaped) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> {
                        if (pos + 4 > text.length()) {
                            throw new IllegalArgumentException("Invalid unicode escape");
                        }
                        String hex = text.substring(pos, pos + 4);
                        pos += 4;
                        builder.append((char) Integer.parseInt(hex, 16));
                    }
                    default -> throw new IllegalArgumentException("Invalid escape character: " + escaped);
                }
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private Boolean parseBoolean() {
        if (text.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (text.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Invalid literal at position " + pos);
    }

    private Object parseNull() {
        if (text.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new IllegalArgumentException("Invalid literal at position " + pos);
    }

    private Double parseNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
            pos++;
        }
        if (pos < text.length() && text.charAt(pos) == '.') {
            pos++;
            while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
                pos++;
            }
        }
        if (pos < text.length() && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
            pos++;
            if (pos < text.length() && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) {
                pos++;
            }
            while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
                pos++;
            }
        }
        if (pos == start) {
            throw new IllegalArgumentException("Invalid number at position " + pos);
        }
        return Double.parseDouble(text.substring(start, pos));
    }

    private char peek() {
        if (pos >= text.length()) {
            throw new IllegalArgumentException("Unexpected end of JSON input");
        }
        return text.charAt(pos);
    }

    private void expect(char c) {
        if (peek() != c) {
            throw new IllegalArgumentException("Expected '" + c + "' at position " + pos);
        }
        pos++;
    }

    private void skipWhitespace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }
}
