package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Statically visible paid args behind free heads: Bukkit stat
 * keys, JMH {@code tag}/{@code keep} values, and static
 * {@code schematic:} place targets. Mirrors the engine load scan
 * 1:1 (see JManhunt {@code PaidTagScan}); dynamic args stay for
 * the runtime backstop. Masked nested tags ({@code ?}) read as
 * dynamic, matching the engine's raw-text rule. Pure and never
 * throws. Validation carve: evaluation stays engine-side.
 */
public final class TagPaidArgs {

    /** Heads taking item payloads, where JMH pairs may hide. */
    private static final Set<String> ITEM_HEADS =
            Set.of("give", "rgive", "ggive", "dropitem", "intercept");

    private TagPaidArgs() {
    }

    /**
     * True when one tag body carries statically visible paid
     * args. The name reads lowercase, like the registry form.
     */
    public static boolean hasPaidArgs(String name, String args) {
        if (name == null || args == null) {
            return false;
        }
        if (name.equals("pstat") || name.equals("rstat")) {
            return hasPaidStatKey(args);
        }
        if (ITEM_HEADS.contains(name)) {
            return hasPaidItemAttrs(args);
        }
        if (name.equals("place")) {
            return hasPaidSchematic(args);
        }
        return false;
    }

    /** Bukkit-sourced player keys; match keys, unknown keys, and dynamic keys pass. */
    private static boolean hasPaidStatKey(String args) {
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() <= 1) {
            return false;
        }
        String raw = unquoted(parts.get(1)).strip();
        if (raw.isEmpty() || raw.indexOf('<') >= 0 || raw.indexOf('?') >= 0) {
            return false;
        }
        String normalized = TagStats.normalizeKey(raw);
        return !TagStats.isPlayerKey(normalized) && TagStats.bukkitType(normalized).isPresent();
    }

    /** JMH {@code [tag, v]} and {@code [keep, v]} pairs with effective values. */
    private static boolean hasPaidItemAttrs(String args) {
        int index = 0;
        while (index < args.length()) {
            if (args.charAt(index) != '[') {
                index++;
                continue;
            }
            int comma = index + 1;
            while (comma < args.length() && isKeyChar(args.charAt(comma))) {
                comma++;
            }
            if (comma < args.length() && args.charAt(comma) == ',') {
                String key = unquoted(args.substring(index + 1, comma)).strip()
                        .toLowerCase(Locale.ROOT);
                String value = balancedListValue(args, comma + 1);
                if (value != null && isEffectiveAttr(key, value)) {
                    return true;
                }
            }
            index++;
        }
        return false;
    }

    /**
     * Effective use, mirroring runtime: a non-blank tag, or keep
     * with literal true. Anything else parses to absent.
     */
    private static boolean isEffectiveAttr(String key, String value) {
        String unquoted = unquoted(value).strip();
        if (unquoted.isEmpty() || unquoted.indexOf('<') >= 0 || unquoted.indexOf('?') >= 0) {
            return false;
        }
        if (key.equals("tag")) {
            return true;
        }
        if (key.equals("keep")) {
            return unquoted.equalsIgnoreCase("true");
        }
        return false;
    }

    /** Pair value to the matching close bracket, or null when unbalanced. */
    private static String balancedListValue(String args, int from) {
        StringBuilder current = new StringBuilder();
        char quote = 0;
        int depth = 1;
        for (int index = from; index < args.length(); index++) {
            char letter = args.charAt(index);
            if (quote != 0) {
                current.append(letter);
                if (letter == quote) {
                    quote = 0;
                }
            } else if (letter == '"' || letter == '\'') {
                quote = letter;
                current.append(letter);
            } else if (letter == '[') {
                depth++;
                current.append(letter);
            } else if (letter == ']') {
                depth--;
                if (depth == 0) {
                    return current.toString();
                }
                current.append(letter);
            } else {
                current.append(letter);
            }
        }
        return null;
    }

    /** Static {@code schematic:} place targets; blank and dynamic ids pass to runtime. */
    private static boolean hasPaidSchematic(String args) {
        List<String> parts = TagLists.splitTopLevel(args);
        String target;
        if (parts.size() == 2) {
            target = parts.get(1);
        } else if (parts.size() == 4) {
            target = parts.get(3);
        } else {
            return false;
        }
        String raw = unquoted(target).strip();
        if (raw.length() <= "schematic:".length()
                || !raw.substring(0, "schematic:".length()).equalsIgnoreCase("schematic:")) {
            return false;
        }
        String id = raw.substring("schematic:".length()).strip();
        return !id.isEmpty() && id.indexOf('<') < 0 && id.indexOf('?') < 0;
    }

    /** Pair-key charset: scalar text up to the comma or a bracket; quotes nest. */
    private static boolean isKeyChar(char letter) {
        return letter != ',' && letter != ']' && letter != '[' && letter != '<'
                && letter != '>';
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
}
