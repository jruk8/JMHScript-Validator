package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Mob-spawn tag for JMHScript: {@code <summon:loc,mob,data?>}.
 * The loc is a located list or bare coords (space or comma
 * separated, ~/^ relative); the mob is a type with an optional
 * appended JMH summon data group; the data is a native NBT blob
 * without braces. Quotes stay structural inside the mob group
 * and the blob, so no quote hygiene applies there.
 * Validation carve: evaluation stays engine-side.
 */
public final class TagSummon {

    private TagSummon() {
    }

    /**
     * Edit-time arity for the summon tag: top-level split, quote
     * hygiene on the loc only, plus the mob and blob shape.
     * Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <summon> needs 2 or 3 args like "
                    + "<summon:loc,mob,data?>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        String mob;
        if (parts.size() == 2 || parts.size() == 3) {
            if (TagHeads.parsePickItem(parts.get(0)).isEmpty() && !parts.get(0).isBlank()) {
                return Optional.of("Tag <summon> mixes quotes.");
            }
            if (parts.get(0).isBlank()) {
                return Optional.of("Tag <summon> needs a loc like "
                        + "<summon:0 100 0,creeper>.");
            }
            mob = parts.get(1);
        } else if ((parts.size() == 4 || parts.size() == 5) && bareTriple(parts)) {
            mob = parts.get(3);
        } else {
            return Optional.of("Tag <summon> needs 2 or 3 args like "
                    + "<summon:loc,mob,data?>.");
        }
        String head = mobHead(mob);
        if (head.isBlank()) {
            return Optional.of("Tag <summon> needs a mob like <summon:loc,creeper>.");
        }
        if (mob.indexOf('[') >= 0 && !balancedGroup(mob.substring(mob.indexOf('[')))) {
            return Optional.of("Tag <summon> trailing summon data wants [...] pairs.");
        }
        return Optional.empty();
    }

    /** True when the head three segments all read as coord components. */
    private static boolean bareTriple(List<String> parts) {
        return coordShaped(parts.get(0)) && coordShaped(parts.get(1))
                && coordShaped(parts.get(2));
    }

    /** True for an absolute double or a ~/^ offset component. */
    private static boolean coordShaped(String raw) {
        String text = raw.strip();
        if (text.isEmpty()) {
            return false;
        }
        if (text.startsWith("~") || text.startsWith("^")) {
            String tail = text.substring(1).strip();
            return tail.isEmpty() || finiteDouble(tail);
        }
        return finiteDouble(text);
    }

    /** True for a finite double literal. */
    private static boolean finiteDouble(String raw) {
        try {
            return Double.isFinite(Double.parseDouble(raw.strip()));
        } catch (NumberFormatException unparsable) {
            return false;
        }
    }

    /** Mob type before the appended group, unquoted. */
    private static String mobHead(String raw) {
        String text = raw.strip();
        if (text.length() >= 2 && ((text.startsWith("\"") && text.endsWith("\""))
                || (text.startsWith("'") && text.endsWith("'")))) {
            text = text.substring(1, text.length() - 1);
        }
        int split = text.indexOf('[');
        return (split < 0 ? text : text.substring(0, split)).strip();
    }

    /** Balanced brackets outside quotes. */
    private static boolean balancedGroup(String text) {
        int squares = 0;
        char quote = 0;
        for (int index = 0; index < text.length(); index++) {
            char letter = text.charAt(index);
            if (quote != 0) {
                if (letter == quote) {
                    quote = 0;
                }
                continue;
            }
            if (letter == '"' || letter == '\'') {
                quote = letter;
                continue;
            }
            if (letter == '[') {
                squares++;
            } else if (letter == ']') {
                squares--;
            }
            if (squares < 0) {
                return false;
            }
        }
        return quote == 0 && squares == 0;
    }
}
