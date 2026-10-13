package com.jruk8.jmhvalidator.core.script;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Positional angle-bracket check for one behavior line. Escapes mask
 * first (a backslash pair is opaque, positions intact), then
 * if-spans mask length-preservingly (mirroring
 * {@code CommandSyntax} so bare {@code < >} in conditions never
 * fault), then a quote-stack scan like
 * {@code TagExpressions.spanEnd} finds the first fault. Pure.
 */
public final class BracketCheck {

    private BracketCheck() {
    }

    /** Which bracket faulted: an opener left hanging, or a stray closer. */
    public enum FaultKind {
        UNCLOSED_OPEN,
        UNMATCHED_CLOSE
    }

    /** Zero-based char index of the faulting bracket, plus its kind. */
    public record Fault(int index, FaultKind kind) {
    }

    /** First bracket fault in the line, or empty when balanced. */
    public static Optional<Fault> firstFault(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        String masked = maskIfSpans(maskEscapes(line));
        Deque<Integer> opens = new ArrayDeque<>();
        StringBuilder quotes = new StringBuilder();
        for (int index = 0; index < masked.length(); index++) {
            char letter = masked.charAt(index);
            if (quotes.length() > 0) {
                if (letter == quotes.charAt(quotes.length() - 1)) {
                    quotes.setLength(quotes.length() - 1);
                } else if (letter == '"' || letter == '\'') {
                    quotes.append(letter);
                }
                continue;
            }
            if (letter == '"' || letter == '\'') {
                quotes.append(letter);
            } else if (letter == '<') {
                opens.push(index);
            } else if (letter == '>') {
                if (opens.isEmpty()) {
                    return Optional.of(new Fault(index, FaultKind.UNMATCHED_CLOSE));
                }
                opens.pop();
            }
        }
        if (opens.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Fault(opens.peekLast(), FaultKind.UNCLOSED_OPEN));
    }

    /**
     * Blanks backslash pairs (the escaped char is inert downstream)
     * without moving positions; a lone trailing backslash stays.
     */
    private static String maskEscapes(String line) {
        StringBuilder masked = new StringBuilder(line);
        for (int index = 0; index + 1 < masked.length(); index++) {
            if (masked.charAt(index) == '\\') {
                masked.setCharAt(index, '?');
                masked.setCharAt(index + 1, '?');
                index++;
            }
        }
        return masked.toString();
    }

    /**
     * Length-preserving if-span collapse: same ready-span loop as
     * {@code CommandSyntax}, but every covered char becomes a mask
     * so fault indexes still point at the original line.
     */
    private static String maskIfSpans(String line) {
        String current = line;
        for (int pass = 0; pass < 25; pass++) {
            List<TagControlFlow.IfSpan> ready = new ArrayList<>();
            for (TagControlFlow.IfSpan span : TagControlFlow.findIfSpans(current)) {
                if (!TagControlFlow.hasNestedIf(span.args())) {
                    ready.add(span);
                }
            }
            if (ready.isEmpty()) {
                return current;
            }
            ready.sort((first, second) -> Integer.compare(second.start(), first.start()));
            StringBuilder masked = new StringBuilder(current);
            for (TagControlFlow.IfSpan span : ready) {
                for (int index = span.start(); index < span.end(); index++) {
                    masked.setCharAt(index, '?');
                }
            }
            current = masked.toString();
        }
        return current;
    }
}
