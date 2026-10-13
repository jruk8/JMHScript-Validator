package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Cooldown tags: {@code <pcooldown:player,key,seconds>} gates one
 * player, {@code <gcooldown:key,seconds>} gates match-wide, each
 * with {@code .get} (remaining window) and {@code .reset} (clear)
 * siblings. The set tags stamp only when the gate opens (no stamp
 * or the window elapsed) and yield {@code "true"} then, else
 * {@code "false"} without touching the stamp. Player keys
 * namespace below the lower-cased name; offline or unknown players
 * resolve {@code null} silently. No Bukkit types: the store
 * arrives through the context.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagCooldowns {

    private TagCooldowns() {
    }


    /**
     * Edit-time arity for the cooldown ops: top-level split, quote
     * hygiene, plus the per-op shape. Mirrors the runtime warns.
     */
    public static Optional<String> syntaxError(String name, String args) {
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
            case "pcooldown", "pcooldown.get" -> 3;
            case "pcooldown.reset", "gcooldown", "gcooldown.get" -> 2;
            default -> 1;
        };
    }

    private static String example(String name) {
        return switch (name) {
            case "pcooldown" -> "<pcooldown:player,key,seconds>";
            case "pcooldown.get" -> "<pcooldown.get:player,key,seconds>";
            case "pcooldown.reset" -> "<pcooldown.reset:player,key>";
            case "gcooldown" -> "<gcooldown:key,seconds>";
            case "gcooldown.get" -> "<gcooldown.get:key,seconds>";
            default -> "<gcooldown.reset:key>";
        };
    }
}
