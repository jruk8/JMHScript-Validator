package com.jruk8.jmhvalidator.core.script;

import com.jruk8.jmhvalidator.core.model.Provenance;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Source locator behind every JMHScript error message. Heads come
 * from the message text (the {@code Tag <head>} convention plus
 * the odd spellings below), chars from span-matching the head in
 * the source line. A char reports only on a UNIQUE span match:
 * zero or several matches stay line-level, never a guess. Pure.
 */
public final class ErrorLocate {

    private static final Pattern MAP_OP = Pattern.compile("Unknown map op:\\s*(\\S+)");
    private static final Pattern SET_OP = Pattern.compile("Unknown set op:\\s*(\\S+)");

    private ErrorLocate() {
    }

    /**
     * One location clause for every error surface: runtime
     * warnings append it, load and GUI reports end with it.
     * A non-positive char stays line-level.
     */
    public static String at(Provenance provenance, int charOneBased) {
        return " at " + provenance.describe(charOneBased);
    }

    /** 1-based char of the failing tag, or empty when not unique. */
    public static OptionalInt charInLine(String line, String message) {
        if (line == null || line.isBlank() || message == null || message.isBlank()) {
            return OptionalInt.empty();
        }
        List<Integer> hits = new ArrayList<>();
        for (String head : headsFromMessage(message)) {
            hits.addAll(spanStarts(line, head));
        }
        if (hits.size() == 1) {
            return OptionalInt.of(hits.get(0) + 1);
        }
        return OptionalInt.empty();
    }

    /**
     * Candidate heads, most specific first: map/set op spellings
     * expand both alias forms, otherwise the first bracketed root
     * in the message. Lowercased for span matching.
     */
    static List<String> headsFromMessage(String message) {
        Matcher maps = MAP_OP.matcher(message);
        if (maps.find()) {
            String op = maps.group(1).toLowerCase(Locale.ROOT);
            return List.of("map." + op, "m." + op);
        }
        Matcher sets = SET_OP.matcher(message);
        if (sets.find()) {
            return List.of("set." + sets.group(1).toLowerCase(Locale.ROOT));
        }
        for (int open = message.indexOf('<'); open >= 0; open = message.indexOf('<', open + 1)) {
            int cursor = open + 1;
            while (cursor < message.length() && TagExpressions.isRootChar(message.charAt(cursor))) {
                cursor++;
            }
            if (cursor > open + 1) {
                return List.of(message.substring(open + 1, cursor).toLowerCase(Locale.ROOT));
            }
        }
        return List.of();
    }

    /**
     * Zero-based starts of every span (top-level or nested) whose
     * root names the head, case-insensitive. Heads never hold
     * escapes, so the raw line scans directly.
     */
    private static List<Integer> spanStarts(String line, String head) {
        List<Integer> hits = new ArrayList<>();
        collectSpans(line, 0, head, hits);
        return hits;
    }

    private static void collectSpans(String text, int base, String head, List<Integer> hits) {
        for (int[] span : TagHeads.topLevelTagSpans(text)) {
            if (rootAt(text, span[0]).equals(head)) {
                hits.add(base + span[0]);
            }
            if (span[1] - span[0] > 2) {
                collectSpans(text.substring(span[0] + 1, span[1] - 1), base + span[0] + 1,
                        head, hits);
            }
        }
    }

    /** Lowercase root behind one span opener: skips space, reads to `:`, space, or `>`. */
    private static String rootAt(String text, int open) {
        int cursor = open + 1;
        while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
            cursor++;
        }
        int end = cursor;
        while (end < text.length() && TagExpressions.isRootChar(text.charAt(end))) {
            end++;
        }
        return text.substring(cursor, end).toLowerCase(Locale.ROOT);
    }
}
