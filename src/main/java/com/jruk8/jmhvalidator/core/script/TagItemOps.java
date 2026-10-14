package com.jruk8.jmhvalidator.core.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Item-payload tags for JMHScript: {@code <give:player,item>},
 * {@code <dropitem:location,item,cooldown>}, and
 * {@code <itemtag:player,slot>}. Arity plus quote hygiene per head,
 * with literal JMH tag-length checks on the item payloads (literals
 * only; dynamic values validate at runtime). The intercept head
 * routes here too and delegates to its own shape check, keeping one
 * item-payload entry point.
 * Validation carve: evaluation stays engine-side.
 */
public final class TagItemOps {

    /** Longest literal JMH tag value before the engine rejects it. */
    static final int MAX_TAG_CHARS = 100;

    private TagItemOps() {
    }

    /**
     * Edit-time arity for one item-payload tag: top-level split,
     * quote hygiene, plus the per-head shape. Mirrors the runtime
     * warns.
     */
    public static Optional<String> opError(String name, String args) {
        return switch (name) {
            case "intercept" -> TagIntercept.opError(name, args);
            case "give", "itemtag" -> fixedArity(name, args, 2);
            case "dropitem" -> dropitemError(args);
            default -> Optional.empty();
        };
    }

    /** Two-arg heads: player plus payload, or player plus slot. */
    private static Optional<String> fixedArity(String name, String args, int arity) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs " + arity + " args like "
                    + example(name) + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != arity) {
            return Optional.of("Tag <" + name + "> needs " + arity + " args like "
                    + example(name) + ".");
        }
        for (int index = 0; index < parts.size(); index++) {
            if (isPayload(name, index)) {
                continue;
            }
            if (TagHeads.parsePickItem(parts.get(index)).isEmpty()
                    && !parts.get(index).isBlank()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        if ("give".equals(name)) {
            return tagLengthError(name, parts.get(1));
        }
        return Optional.empty();
    }

    /** Item payload segments carry quotes structurally, so hygiene skips them. */
    private static boolean isPayload(String name, int index) {
        return ("give".equals(name) || "dropitem".equals(name)) && index == 1;
    }

    /** Dropitem: location, payload, plus an optional cooldown. */
    private static Optional<String> dropitemError(String args) {
        String name = "dropitem";
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <dropitem> needs 2 or 3 args like " + example(name)
                    + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 2 && parts.size() != 3) {
            return Optional.of("Tag <dropitem> needs 2 or 3 args like " + example(name)
                    + ".");
        }
        for (int index = 0; index < parts.size(); index++) {
            if (isPayload(name, index)) {
                continue;
            }
            if (TagHeads.parsePickItem(parts.get(index)).isEmpty()
                    && !parts.get(index).isBlank()) {
                return Optional.of("Tag <dropitem> mixes quotes.");
            }
        }
        return tagLengthError(name, parts.get(1));
    }

    /**
     * Literal JMH tag values over the limit fail; payloads holding
     * nested tags or masked values abstain for runtime. Shared with
     * the intercept shape check.
     */
    static Optional<String> tagLengthError(String name, String payload) {
        if (payload == null || payload.contains("<") || payload.contains(">")
                || payload.contains("?")) {
            return Optional.empty();
        }
        if (overlongTag(payload)) {
            return Optional.of("Tag <" + name + "> tag value allows 100 chars at most.");
        }
        return Optional.empty();
    }

    /** True when any literal [tag, value] pair runs over the limit. */
    private static boolean overlongTag(String payload) {
        for (String item : splitItems(payload)) {
            if (isTagPair(item)) {
                String value = unquoted(splitItems(item).get(1));
                if (value.length() > MAX_TAG_CHARS) {
                    return true;
                }
            } else if (TagLists.isList(item) && overlongTag(item)) {
                return true;
            }
        }
        return false;
    }

    /** True for a two-element list headed by the tag key. */
    private static boolean isTagPair(String item) {
        if (!TagLists.isList(item)) {
            return false;
        }
        List<String> pair = splitItems(item);
        return pair.size() == 2 && "tag".equalsIgnoreCase(unquoted(pair.get(0)).strip());
    }

    /** Quote-aware inner split: commas inside quotes never split. */
    private static List<String> splitItems(String text) {
        if (!TagLists.isList(text)) {
            return List.of();
        }
        String inner = text.strip();
        inner = inner.substring(1, inner.length() - 1);
        if (inner.isBlank()) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        for (String part : TagLists.splitTopLevel(inner)) {
            items.add(part.strip());
        }
        return items;
    }

    /** Strips one pair of surrounding single or double quotes. */
    private static String unquoted(String raw) {
        String text = raw.strip();
        if (text.length() >= 2 && ((text.startsWith("\"") && text.endsWith("\""))
                || (text.startsWith("'") && text.endsWith("'")))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    public static String example(String name) {
        return switch (name) {
            case "give" -> "<give:player,item>";
            case "dropitem" -> "<dropitem:location,item,cooldown>";
            default -> "<itemtag:player,slot>";
        };
    }
}
