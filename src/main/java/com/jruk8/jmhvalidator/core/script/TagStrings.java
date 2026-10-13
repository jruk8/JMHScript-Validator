package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * String foundation for JMHScript: the SSOT behind
 * {@code <str.join>}, {@code <str.split>}, {@code <str.lower>},
 * {@code <str.upper>}, {@code <str.contains>}, {@code <str.slice>},
 * and {@code <str.matches>}, plus the Oxford list formatter
 * {@code <string.listformat>} (aliases {@code <string.lformat>},
 * {@code <s.listformat>}, {@code <s.lformat>}).
 * {@code <list.join>} delegates to the same join. Splits are
 * literal (never regex) and keep every part, so a join-split
 * round trip restores the list exactly. No Bukkit types.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagStrings {

    private TagStrings() {
    }


    /**
     * Edit-time arity for the string ops: top-level split, quote
     * hygiene, plus the per-op shape. Mirrors the runtime warns.
     */
    static Optional<String> opError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs " + arityText(name) + " like "
                    + example(name) + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (!arityOk(name, parts.size())) {
            return Optional.of("Tag <" + name + "> needs " + arityText(name) + " like "
                    + example(name) + ".");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

    private static boolean arityOk(String name, int count) {
        if (name.equals("str.slice")) {
            return count == 2 || count == 3;
        }
        if (isListformat(name)) {
            return count >= 1 && count <= 3;
        }
        return switch (name) {
            case "str.lower", "str.upper" -> count == 1;
            default -> count == 2;
        };
    }

    private static String arityText(String name) {
        if (name.equals("str.slice")) {
            return "2 to 3 args";
        }
        if (isListformat(name)) {
            return "1 to 3 args";
        }
        return switch (name) {
            case "str.lower", "str.upper" -> "1 arg";
            default -> "2 args";
        };
    }

    private static boolean isListformat(String name) {
        return name.equals("string.listformat") || name.equals("string.lformat")
                || name.equals("s.listformat") || name.equals("s.lformat");
    }

    private static String example(String name) {
        return switch (name) {
            case "string.listformat" -> "<string.listformat:list,itemcolor,sepcolor>";
            case "string.lformat" -> "<string.lformat:list,itemcolor,sepcolor>";
            case "s.listformat" -> "<s.listformat:list,itemcolor,sepcolor>";
            case "s.lformat" -> "<s.lformat:list,itemcolor,sepcolor>";
            case "str.join" -> "<str.join:list,sep>";
            case "str.split" -> "<str.split:text,delim>";
            case "str.lower" -> "<str.lower:text>";
            case "str.upper" -> "<str.upper:text>";
            case "str.slice" -> "<str.slice:text,start,end>";
            case "str.matches" -> "<str.matches:text,regex>";
            default -> "<str.contains:text,needle>";
        };
    }
}
