package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Vector math for JMHScript: the SSOT behind {@code <vec.add>},
 * {@code <vec.sub>}, {@code <vec.mult>}, {@code <vec.normalize>},
 * {@code <vec.sqrdist>}, {@code <vec.dist>}, {@code <vec.dot>},
 * {@code <vec.cross>}, {@code <vec.sqrdist2d>},
 * {@code <vec.dist2d>}, {@code <loc.shift>}, and {@code <pdir>}.
 * Vector args accept 3-element lists or 6-element location
 * primitives (first three indices); the 2d pair takes {@code [x, z]}
 * pairs or 6-element primitives (first and third indices) instead.
 * Anything else warns plus {@code null} at the tag layer. Outputs
 * format with {@link TagMath#formatNumber}. No Bukkit types except
 * the direction read, which arrives through the roster.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagVectors {

    private TagVectors() {
    }


    /**
     * Edit-time arity for the vector ops: top-level split, quote
     * hygiene, plus the per-op shape. Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        int count = arity(name);
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs " + count + " args like "
                    + example(name) + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != count) {
            return Optional.of("Tag <" + name + "> needs " + count + " args like "
                    + example(name) + ".");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

    private static int arity(String name) {
        return switch (name) {
            case "pdir", "vec.normalize" -> 1;
            case "loc.shift" -> 3;
            default -> 2;
        };
    }

    private static String example(String name) {
        return switch (name) {
            case "pdir" -> "<pdir:player>";
            case "vec.add" -> "<vec.add:a,b>";
            case "vec.sub" -> "<vec.sub:a,b>";
            case "vec.mult" -> "<vec.mult:vec,scalar>";
            case "vec.normalize" -> "<vec.normalize:vec>";
            case "vec.sqrdist" -> "<vec.sqrdist:a,b>";
            case "vec.dist" -> "<vec.dist:a,b>";
            case "vec.sqrdist2d" -> "<vec.sqrdist2d:a,b>";
            case "vec.dist2d" -> "<vec.dist2d:a,b>";
            case "vec.dot" -> "<vec.dot:a,b>";
            case "vec.cross" -> "<vec.cross:a,b>";
            default -> "<loc.shift:location,direction,distance>";
        };
    }
}
