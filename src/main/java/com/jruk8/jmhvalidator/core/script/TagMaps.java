package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Ordered maps for JMHScript: {@code map.*} plus the {@code m.*}
 * shorthand over one implementation. A map is an association list
 * {@code [[k1, v1], [k2, v2]]} in insertion order, stored like any
 * list value. Keys match verbatim and case-sensitively; the first
 * match wins on dirty data. Reads skip malformed pairs silently
 * while writes preserve them in place, so one bad pair never voids
 * the structure.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagMaps {

    private TagMaps() {
    }


    /** Verb behind either spelling: map.get and m.get both read get. */
    private static String verb(String op) {
        return op.substring(op.indexOf('.') + 1);
    }


    private static int arity(String verb) {
        return switch (verb) {
            case "add", "set" -> 3;
            case "get", "remove", "contains" -> 2;
            default -> 1;
        };
    }

    private static String example(String op) {
        return switch (verb(op)) {
            case "add", "set" -> "<" + op + ":[[k, v]],k,x>";
            case "get", "remove", "contains" -> "<" + op + ":[[k, v]],k>";
            default -> "<" + op + ":[[k, v]]>";
        };
    }

    /**
     * Edit-time arity for the map tags: top-level split, quote
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
