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
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <plos> needs 2 to 4 args like " + example() + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 2 || parts.size() > 4) {
            return Optional.of("Tag <plos> needs 2 to 4 args like " + example() + ".");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <plos> mixes quotes.");
            }
        }
        if (parts.size() > 2 && literalLimit(parts.get(2)) != null
                && literalLimit(parts.get(2)) <= 0) {
            return Optional.of("Tag <plos> needs a positive maxdist.");
        }
        if (parts.size() > 3 && literalLimit(parts.get(3)) != null
                && (literalLimit(parts.get(3)) < 0 || literalLimit(parts.get(3)) > 180)) {
            return Optional.of("Tag <plos> maxangle clamps to 0..180.");
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

    public static String example() {
        return "<plos:player,target,maxdist,maxangle>";
    }
}
