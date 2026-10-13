package com.jruk8.jmhvalidator.core.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * List shapes for validation: literals, top-level splits, and
 * per-op arity. Evaluation stays engine-side.
 */
public final class TagLists {

    private TagLists() {
    }

    /** True when the trimmed text is bracket-wrapped (length >= 2). */
    public static boolean isList(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.strip();
        return trimmed.length() >= 2 && trimmed.startsWith("[") && trimmed.endsWith("]");
    }

    /**
     * Parses a list value to items. Non-lists and blank innards yield
     * an empty list; items split on top-level commas (square-depth
     * aware), trim, and keep empty items for round-trip stability.
     */
    public static List<String> parse(String text) {
        if (!isList(text)) {
            return List.of();
        }
        String inner = text.strip();
        inner = inner.substring(1, inner.length() - 1);
        if (inner.isBlank()) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        StringBuilder item = new StringBuilder();
        int depth = 0;
        for (int index = 0; index < inner.length(); index++) {
            char letter = inner.charAt(index);
            if (letter == '[') {
                depth++;
                item.append(letter);
            } else if (letter == ']') {
                if (depth > 0) {
                    depth--;
                }
                item.append(letter);
            } else if (letter == ',' && depth == 0) {
                items.add(item.toString().strip());
                item.setLength(0);
            } else {
                item.append(letter);
            }
        }
        items.add(item.toString().strip());
        return items;
    }


    /**
     * Edit-time arity for the list ops: top-level split, quote
     * hygiene, plus the per-op shape. Mirrors the runtime warns.
     */
    /** The {@code l.*} shorthand reads as its {@code list.*} twin. */
    public static String canonicalOp(String op) {
        return op.startsWith("l.") ? "list." + op.substring(2) : op;
    }

    public static Optional<String> opError(String name, String args) {
        name = canonicalOp(name);
        int arity = name.equals("list.set") || name.equals("list.slice") ? 3
                : name.equals("list.append") || name.equals("list.get") || name.equals("list.remove")
                        || name.equals("list.contains") || name.equals("list.filter")
                        || name.equals("list.join") ? 2 : 1;
        String example = switch (name) {
            case "list.append" -> "<list.append:list,x>";
            case "list.get" -> "<list.get:list,index>";
            case "list.set" -> "<list.set:list,index,x>";
            case "list.remove" -> "<list.remove:list,x>";
            case "list.contains" -> "<list.contains:list,x>";
            case "list.clear" -> "<list.clear:list>";
            case "list.pop" -> "<list.pop:list>";
            case "list.shuffle" -> "<list.shuffle:list>";
            case "list.filter" -> "<list.filter:list,cond>";
            case "list.reverse" -> "<list.reverse:list>";
            case "list.join" -> "<list.join:list,sep>";
            case "list.slice" -> "<list.slice:list,start,end>";
            case "list.first" -> "<list.first:list>";
            case "list.last" -> "<list.last:list>";
            default -> "<len:list>";
        };
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs " + arity + " args like " + example
                    + ".");
        }
        List<String> parts = splitTopLevel(args);
        if (parts.size() != arity) {
            return Optional.of("Tag <" + name + "> needs " + arity + " args like " + example
                    + ".");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }


    /**
     * Splits tag args on top-level commas: commas inside nested tags
     * ({@code <...}>), square brackets ({@code [...]}), or quotes
     * never split. List ops and flag values need this (the shared
     * splitter would shred list literals); other tags keep the shared
     * splitter.
     */
    public static List<String> splitTopLevel(String args) {
        List<String> parts = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        int angles = 0;
        int squares = 0;
        char quote = 0;
        for (int index = 0; index < args.length(); index++) {
            char letter = args.charAt(index);
            if (quote != 0) {
                part.append(letter);
                if (letter == quote) {
                    quote = 0;
                }
            } else if (letter == '"' || letter == '\'') {
                quote = letter;
                part.append(letter);
            } else if (letter == '<') {
                angles++;
                part.append(letter);
            } else if (letter == '>') {
                if (angles > 0) {
                    angles--;
                }
                part.append(letter);
            } else if (letter == '[') {
                squares++;
                part.append(letter);
            } else if (letter == ']') {
                if (squares > 0) {
                    squares--;
                }
                part.append(letter);
            } else if (letter == ',' && angles == 0 && squares == 0) {
                parts.add(part.toString());
                part.setLength(0);
            } else {
                part.append(letter);
            }
        }
        parts.add(part.toString());
        return parts;
    }
}
