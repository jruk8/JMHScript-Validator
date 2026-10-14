package com.jruk8.jmhvalidator.core.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Pure tag-shape helpers carved from the engine's dispatch
 * class: head parsing, top-level spans, and pick splitting.
 * Bodies are verbatim; only the home changed. Runtime dispatch
 * (passes, sinks, budgets) stays engine-side.
 */
final class TagHeads {
    private TagHeads() {
    }

    /**
     * Every {@code <...>} span including nested ones, as absolute
     * [start, end) pairs in pre-order. Bodies slice from the
     * original line with these offsets.
     */
    static List<int[]> allTagSpans(String line) {
        List<int[]> out = new ArrayList<>();
        collectSpans(line, 0, out);
        return out;
    }

    private static void collectSpans(String line, int base, List<int[]> out) {
        for (int[] span : topLevelTagSpans(line)) {
            out.add(new int[] {base + span[0], base + span[1]});
            if (span[1] - span[0] > 2) {
                collectSpans(line.substring(span[0] + 1, span[1] - 1),
                        base + span[0] + 1, out);
            }
        }
    }

    /** Tag name: before any colon, trimmed, lowercase. Mirrors tag evaluation. */
    static String tagName(String body) {
        int separator = body.indexOf(':');
        String name = separator < 0 ? body : body.substring(0, separator);
        return name.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Top-level {@code <...>} spans as [start, end) pairs. Angle
     * brackets inside quotes do not count, and a {@code <} with no
     * matching close is prose, not a span. Pure for tests.
     */
    static List<int[]> topLevelTagSpans(String command) {
        List<int[]> spans = new ArrayList<>();
        int index = 0;
        while (index < command.length()) {
            int open = command.indexOf('<', index);
            if (open < 0) {
                return spans;
            }
            int close = spanClose(command, open);
            if (close < 0) {
                index = open + 1;
            } else {
                spans.add(new int[]{open, close + 1});
                index = close + 1;
            }
        }
        return spans;
    }

    /** Matching close of the tag opening at {@code open}; -1 when unclosed. Shared with the pre-pass. */
    static int spanClose(String command, int open) {
        int depth = 0;
        char quote = 0;
        for (int index = open; index < command.length(); index++) {
            char letter = command.charAt(index);
            if (quote != 0) {
                if (letter == quote) {
                    quote = 0;
                }
            } else if (letter == '"' || letter == '\'') {
                quote = letter;
            } else if (letter == '<') {
                depth++;
            } else if (letter == '>') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    /** Splits pick args on commas outside single or double quotes. */
    static List<String> splitPickArgs(String args) {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int index = 0; index < args.length(); index++) {
            char letter = args.charAt(index);
            if (quote != 0) {
                current.append(letter);
                if (letter == quote) {
                    quote = 0;
                }
            } else if (letter == '"' || letter == '\'') {
                quote = letter;
                current.append(letter);
            } else if (letter == ',') {
                items.add(current.toString());
                current.setLength(0);
            } else {
                current.append(letter);
            }
        }
        items.add(current.toString());
        return items;
    }

    /**
     * Validates one pick item: either bare with no quote characters, or one
     * balanced quoted string and nothing else. Empty when illegal.
     */
    static Optional<String> parsePickItem(String raw) {
        String item = raw.trim();
        if (item.isEmpty()) {
            return Optional.empty();
        }
        char first = item.charAt(0);
        if (first != '"' && first != '\'') {
            return item.indexOf('"') < 0 && item.indexOf('\'') < 0
                    ? Optional.of(item)
                    : Optional.empty();
        }
        if (item.length() < 2 || item.charAt(item.length() - 1) != first) {
            return Optional.empty();
        }
        String inner = item.substring(1, item.length() - 1);
        return inner.indexOf(first) < 0 ? Optional.of(inner) : Optional.empty();
    }
}
