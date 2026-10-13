package com.jruk8.jmhvalidator.core.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Extended tag expressions for modifier commands: {@code <if>},
 * {@code <min>}, {@code <max>}, {@code <clamp>}, {@code <id>},
 * message and sound tags, math values, conditions, and the lone
 * {@code exit} line. Pure: side effects run through the context
 * sinks. No Bukkit types.
 */
public final class TagExpressions {

    private TagExpressions() {
    }

    /** Condition structure problem: callers warn and yield empty. */
    public static final class ExprException extends Exception {
        ExprException(String message) {
            super(message);
        }
    }


    /** A surrounding group, as opposed to parentheses around a math operand. */
    private static boolean wrappedCondition(String text) {
        if (!text.startsWith("(") || !text.endsWith(")")) {
            return false;
        }
        int depth = 0;
        char quote = 0;
        for (int index = 0; index < text.length(); index++) {
            final char letter = text.charAt(index);
            final Integer tagClose = quote == 0 ? nestedTagClose(text, index) : null;
            if (tagClose != null) {
                index = tagClose;
                continue;
            }
            if (quote != 0) {
                if (letter == quote) {
                    quote = 0;
                }
            } else if (letter == '"' || letter == '\'') {
                quote = letter;
            } else if (letter == '(') {
                depth++;
            } else if (letter == ')' && --depth == 0) {
                return index == text.length() - 1;
            }
        }
        return false;
    }

    private static boolean isNotWord(String text) {
        if (text.length() < 3 || !text.regionMatches(true, 0, "not", 0, 3)) {
            return false;
        }
        return text.length() == 3 || Character.isWhitespace(text.charAt(3));
    }


    /**
     * Closing {@code >} of the nested tag opening at {@code index},
     * or null when the bracket opens no tag. Only a tag-name letter
     * after {@code <} qualifies for the skip; anything else is a
     * stray bracket that validation reports.
     */
    static Integer nestedTagClose(String part, int index) {
        if (part.charAt(index) != '<' || index + 1 >= part.length()
                || Character.isWhitespace(part.charAt(index + 1))) {
            return null;
        }
        if (!Character.isLetter(part.charAt(index + 1))) {
            return null;
        }
        return spanEnd(part, index);
    }

    /**
     * True when a comparison sits outside quotes, groups, and
     * nested tags. Shared by runtime evaluation and static
     * validation.
     */
    static boolean hasComparison(String part) {
        for (final String orPart : splitWords(part, "or")) {
            for (final String andPart : splitWords(orPart, "and")) {
                String stripped = andPart.strip();
                while (isNotWord(stripped)) {
                    stripped = stripped.substring(3).strip();
                }
                if (wrappedCondition(stripped)
                        ? hasComparison(stripped.substring(1, stripped.length() - 1))
                        : TagComparison.findOperator(stripped) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Rejects stray angle brackets outside quotes, groups, and nested
     * tags: they are not comparisons anymore. Shared by runtime
     * evaluation and static validation.
     */
    static void validateConditionBrackets(String part) throws ExprException {
        int depth = 0;
        char quote = 0;
        for (int index = 0; index < part.length(); index++) {
            char letter = part.charAt(index);
            if (quote != 0) {
                if (letter == quote) {
                    quote = 0;
                }
                continue;
            }
            final Integer tagClose = nestedTagClose(part, index);
            if (tagClose != null) {
                index = tagClose;
                continue;
            }
            if (letter == '"' || letter == '\'') {
                quote = letter;
            } else if (letter == '(') {
                depth++;
                if (depth > 64) {
                    throw new ExprException("condition exceeds 64 nested parentheses");
                }
            } else if (letter == ')') {
                if (--depth < 0) {
                    throw new ExprException("condition has unmatched parentheses");
                }
            } else if (depth == 0) {
                if (letter == '<' || letter == '>') {
                    throw new ExprException(
                            "'" + letter + "' is not a comparison; use lt, le, gt, ge");
                }
            }
        }
        if (depth != 0) {
            throw new ExprException("condition has unmatched parentheses");
        }
    }


    private static List<String> splitWords(String text, String word) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        char quote = 0;
        int index = 0;
        while (index < text.length()) {
            char letter = text.charAt(index);
            final Integer tagClose = quote == 0 ? nestedTagClose(text, index) : null;
            if (tagClose != null) {
                current.append(text, index, tagClose + 1);
                index = tagClose + 1;
                continue;
            }
            if (quote != 0) {
                current.append(letter);
                if (letter == quote) {
                    quote = 0;
                }
                index++;
            } else if (letter == '"' || letter == '\'') {
                quote = letter;
                current.append(letter);
                index++;
            } else if (letter == '(') {
                depth++;
                current.append(letter);
                index++;
            } else if (letter == ')') {
                depth = Math.max(0, depth - 1);
                current.append(letter);
                index++;
            } else if (depth == 0 && isWord(text, index, word)) {
                parts.add(current.toString());
                current.setLength(0);
                index += word.length() + 1;
            } else {
                current.append(letter);
                index++;
            }
        }
        parts.add(current.toString());
        return parts;
    }

    private static boolean isWord(String text, int index, String word) {
        if (index == 0 || !Character.isWhitespace(text.charAt(index - 1))) {
            return false;
        }
        int end = index + word.length();
        return end < text.length() && Character.isWhitespace(text.charAt(end))
                && text.substring(index, end).toLowerCase(Locale.ROOT).equals(word);
    }


    static boolean isRootChar(char letter) {
        return letter == '_' || letter == '-' || letter == '.'
                || Character.isLetterOrDigit(letter);
    }

    /** Inclusive index of the balancing {@code >}, or null when unclosed. Shared with loops. */
    static Integer spanEnd(String line, int open) {
        int depth = 0;
        StringBuilder quotes = new StringBuilder();
        for (int index = open; index < line.length(); index++) {
            char letter = line.charAt(index);
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
                depth++;
            } else if (letter == '>') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return null;
    }

    /** True when the args hold another {@code <if} tag. */
    /** True when the line is just {@code exit}, ignoring one leading slash. */
    /** True when {@code exit} leads the line but is not alone. */
}
