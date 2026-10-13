package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Hashsets for JMHScript: {@code set.*} with no token alias, since
 * {@code s} belongs to the string family. A set rides a plain list
 * and reads deduped in first-occurrence order, so dirty data still
 * behaves like a set while writes always store canonical members.
 * Membership matches verbatim and case-sensitively.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagSets {

    private TagSets() {
    }


    /** Verb behind the head: set.add reads add. */
    private static String verb(String op) {
        return op.substring(op.indexOf('.') + 1);
    }


    private static int arity(String verb) {
        return switch (verb) {
            case "add", "remove", "contains" -> 2;
            default -> 1;
        };
    }

    private static String example(String op) {
        return switch (verb(op)) {
            case "add", "remove", "contains" -> "<" + op + ":[a, b],a>";
            default -> "<" + op + ":[a, b]>";
        };
    }

    /**
     * Edit-time arity for the set tags: top-level split, quote
     * hygiene, plus the per-verb shape. Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        int arity = arity(verb(name));
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs " + arity + " args like "
                    + example(name) + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != arity) {
            return Optional.of("Tag <" + name + "> needs " + arity + " args like "
                    + example(name) + ".");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }
}
