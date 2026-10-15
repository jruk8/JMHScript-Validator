package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Player line-of-sight tag for JMHScript: 3D only. Cheap gates run
 * first (same dimension, distance, angle against the player's look
 * direction), then one block+player raycast from the eye to the
 * target head point decides reachability. True when every gate
 * passes, false on a clean fail, {@code null} on misuse (warned) or
 * backend miss (silent). No Bukkit types: every read runs through
 * the context backends.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagPlos {


    private TagPlos() {
    }


    /**
     * Edit-time arity for the plos tag: top-level split, quote
     * hygiene, plus literal limit checks (literals only; dynamic
     * values validate at runtime). Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        int floor = name.equals("glos") ? 1 : 2;
        int ceiling = name.equals("glos") ? 3 : 4;
        String range = name.equals("glos") ? "1 to 3 args" : "2 to 4 args";
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs " + range + " like "
                    + example(name) + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < floor || parts.size() > ceiling) {
            return Optional.of("Tag <" + name + "> needs " + range + " like "
                    + example(name) + ".");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        int options = parts.size() - floor;
        if (options > 0 && literalLimit(parts.get(parts.size() - options)) != null
                && literalLimit(parts.get(parts.size() - options)) <= 0) {
            return Optional.of("Tag <" + name + "> needs a positive maxdist.");
        }
        if (options > 1 && literalLimit(parts.get(parts.size() - 1)) != null
                && (literalLimit(parts.get(parts.size() - 1)) < 0
                || literalLimit(parts.get(parts.size() - 1)) > 180)) {
            return Optional.of("Tag <" + name + "> maxangle clamps to 0..180.");
        }
        return Optional.empty();
    }

    /** Clean literal double, or null when dynamic or unparsable. */
    private static Double literalLimit(String part) {
        if (part.contains("<")) {
            return null;
        }
        try {
            double value = Double.parseDouble(part.strip());
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException unparsable) {
            return null;
        }
    }

    public static String example(String name) {
        return switch (name) {
            case "rlos" -> "<rlos:role,target,maxdist,maxangle>";
            case "glos" -> "<glos:target,maxdist,maxangle>";
            default -> "<plos:player,target,maxdist,maxangle>";
        };
    }

    /** Engine-facing example for the {@code <plos>} head. */
    public static String example() {
        return example("plos");
    }
}
