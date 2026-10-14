package com.jruk8.jmhvalidator.core.share;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal JSON for the share codec: a strict parser plus a
 * minified emitter with Gson-identical bytes for the codec
 * subset (html-safe string table, Double decimals, insertion
 * key order). Numbers always parse to Double, matching Gson
 * untyped maps, so decode and re-export stay byte-stable.
 */
public final class ShareJson {
    /** Deepest nesting the parser accepts before giving up. */
    static final int MAX_DEPTH = 100;

    private ShareJson() {
    }

    /**
     * Parses one JSON value. Empty when malformed, when trailing
     * content follows the value, or when the value is JSON null
     * (the codec treats null roots as failures either way).
     */
    public static Optional<Object> parse(String json) {
        if (json == null) {
            return Optional.empty();
        }
        Parser parser = new Parser(json);
        try {
            Object value = parser.value(0);
            parser.skipGaps();
            if (!parser.atEnd()) {
                return Optional.empty();
            }
            return Optional.ofNullable(value);
        } catch (Invalid failed) {
            return Optional.empty();
        }
    }

    /**
     * Minified JSON for maps, lists, strings, numbers, booleans,
     * and null. Unknown values render as quoted strings. Non-finite
     * doubles throw, matching Gson.
     */
    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            writeString(text, out);
        } else if (value instanceof Boolean flag) {
            out.append(flag ? "true" : "false");
        } else if (value instanceof Map<?, ?> map) {
            writeObject(map, out);
        } else if (value instanceof List<?> list) {
            writeArray(list, out);
        } else if (value instanceof Number number) {
            writeNumber(number, out);
        } else {
            writeString(String.valueOf(value), out);
        }
    }

    private static void writeObject(Map<?, ?> map, StringBuilder out) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeString(String.valueOf(entry.getKey()), out);
            out.append(':');
            write(entry.getValue(), out);
        }
        out.append('}');
    }

    private static void writeArray(List<?> list, StringBuilder out) {
        out.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                out.append(',');
            }
            first = false;
            write(item, out);
        }
        out.append(']');
    }

    private static void writeNumber(Number number, StringBuilder out) {
        if (number instanceof Double value) {
            requireFinite(value);
            out.append(Double.toString(value));
        } else if (number instanceof Float value) {
            requireFinite(value.doubleValue());
            out.append(Double.toString(value.doubleValue()));
        } else if (number instanceof BigDecimal value) {
            out.append(value.toString());
        } else {
            out.append(number.toString());
        }
    }

    private static void requireFinite(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Non-finite numbers cannot be shared");
        }
    }

    private static void writeString(String text, StringBuilder out) {
        out.append('"');
        for (int index = 0; index < text.length(); index++) {
            writeChar(text.charAt(index), out);
        }
        out.append('"');
    }

    private static void writeChar(char head, StringBuilder out) {
        switch (head) {
            case '"':
                out.append("\\\"");
                break;
            case '\\':
                out.append("\\\\");
                break;
            case '\b':
                out.append("\\b");
                break;
            case '\f':
                out.append("\\f");
                break;
            case '\n':
                out.append("\\n");
                break;
            case '\r':
                out.append("\\r");
                break;
            case '\t':
                out.append("\\t");
                break;
            default:
                writePlain(head, out);
                break;
        }
    }

    /** Gson html-safe table: controls, separators, and lt/gt/amp. */
    private static void writePlain(char head, StringBuilder out) {
        if (head < 0x20 || head == 0x2028 || head == 0x2029
                || head == '<' || head == '>' || head == '&' || head == '=' || head == '\'') {
            out.append("\\u");
            out.append(Character.forDigit((head >> 12) & 0xF, 16));
            out.append(Character.forDigit((head >> 8) & 0xF, 16));
            out.append(Character.forDigit((head >> 4) & 0xF, 16));
            out.append(Character.forDigit(head & 0xF, 16));
            return;
        }
        out.append(head);
    }

    /** Recursive descent over one JSON text. */
    private static final class Parser {
        private final String text;
        private int pos;

        Parser(String text) {
            this.text = text;
        }

        Object value(int depth) {
            skipGaps();
            if (atEnd()) {
                throw new Invalid();
            }
            char head = text.charAt(pos);
            return switch (head) {
                case '{' -> object(depth + 1);
                case '[' -> array(depth + 1);
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object(int depth) {
            if (depth > MAX_DEPTH) {
                throw new Invalid();
            }
            Map<String, Object> map = new LinkedHashMap<>();
            pos++;
            skipGaps();
            if (take('}')) {
                return map;
            }
            while (true) {
                skipGaps();
                if (atEnd() || text.charAt(pos) != '"') {
                    throw new Invalid();
                }
                String key = string();
                skipGaps();
                if (!take(':')) {
                    throw new Invalid();
                }
                map.put(key, value(depth));
                skipGaps();
                if (take(',')) {
                    continue;
                }
                if (take('}')) {
                    return map;
                }
                throw new Invalid();
            }
        }

        private List<Object> array(int depth) {
            if (depth > MAX_DEPTH) {
                throw new Invalid();
            }
            List<Object> list = new ArrayList<>();
            pos++;
            skipGaps();
            if (take(']')) {
                return list;
            }
            while (true) {
                list.add(value(depth));
                skipGaps();
                if (take(',')) {
                    continue;
                }
                if (take(']')) {
                    return list;
                }
                throw new Invalid();
            }
        }

        private String string() {
            pos++;
            StringBuilder out = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw new Invalid();
                }
                char head = text.charAt(pos++);
                if (head == '"') {
                    return out.toString();
                }
                if (head == '\\') {
                    out.append(escape());
                    continue;
                }
                if (head < 0x20) {
                    throw new Invalid();
                }
                out.append(head);
            }
        }

        private char escape() {
            if (atEnd()) {
                throw new Invalid();
            }
            char head = text.charAt(pos++);
            switch (head) {
                case '"':
                    return '"';
                case '\\':
                    return '\\';
                case '/':
                    return '/';
                case 'b':
                    return '\b';
                case 'f':
                    return '\f';
                case 'n':
                    return '\n';
                case 'r':
                    return '\r';
                case 't':
                    return '\t';
                case 'u':
                    return unicode();
                default:
                    throw new Invalid();
            }
        }

        private char unicode() {
            if (pos + 4 > text.length()) {
                throw new Invalid();
            }
            int code = 0;
            for (int index = 0; index < 4; index++) {
                int digit = Character.digit(text.charAt(pos++), 16);
                if (digit < 0) {
                    throw new Invalid();
                }
                code = code * 16 + digit;
            }
            return (char) code;
        }

        private Object literal(String word, Object value) {
            if (text.startsWith(word, pos)) {
                pos += word.length();
                return value;
            }
            throw new Invalid();
        }

        private Double number() {
            int start = pos;
            take('-');
            if (atEnd()) {
                throw new Invalid();
            }
            if (text.charAt(pos) == '0') {
                pos++;
            } else if (isDigit(text.charAt(pos))) {
                digits();
            } else {
                throw new Invalid();
            }
            if (take('.')) {
                digits();
            }
            if (!atEnd() && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
                pos++;
                if (!atEnd() && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) {
                    pos++;
                }
                digits();
            }
            try {
                return Double.parseDouble(text.substring(start, pos));
            } catch (NumberFormatException failed) {
                throw new Invalid();
            }
        }

        private void digits() {
            if (atEnd() || !isDigit(text.charAt(pos))) {
                throw new Invalid();
            }
            while (!atEnd() && isDigit(text.charAt(pos))) {
                pos++;
            }
        }

        private boolean take(char want) {
            if (!atEnd() && text.charAt(pos) == want) {
                pos++;
                return true;
            }
            return false;
        }

        private void skipGaps() {
            while (!atEnd()) {
                char head = text.charAt(pos);
                if (head != ' ' && head != '\t' && head != '\n' && head != '\r') {
                    return;
                }
                pos++;
            }
        }

        private boolean atEnd() {
            return pos >= text.length();
        }

        private static boolean isDigit(char head) {
            return head >= '0' && head <= '9';
        }
    }

    /** Control-flow failure: malformed JSON, always caught into empty. */
    private static final class Invalid extends RuntimeException {
        Invalid() {
            super(null, null, false, false);
        }
    }
}
