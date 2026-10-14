package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Effect tags: {@code <peffect:player,effects>} hits one player,
 * {@code <geffect:effects>} hits everyone online, and
 * {@code <reffect:role,effects>} hits the named role members
 * ({@code peff}, {@code geff}, and {@code reff} alias them). The
 * effects nest as {@code [[id, amplifier, seconds], ...]}, and a
 * trailing {@code hide} flag (default true) controls particles.
 * Every tag yields empty. No Bukkit types: delivery runs through
 * the context backends.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagEffects {

    private TagEffects() {
    }


    /**
     * Edit-time arity for the effect tags: top-level split, quote
     * hygiene, plus the per-tag shape. Mirrors the runtime warns.
     */
    public static Optional<String> effectError(String name, String args) {
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

    public static boolean isPlayer(String name) {
        return name.equals("peffect") || name.equals("peff");
    }

    public static boolean isRole(String name) {
        return name.equals("reffect") || name.equals("reff");
    }

    private static boolean arityOk(String name, int count) {
        if (name.equals("geffect") || name.equals("geff")) {
            return count == 1 || count == 2;
        }
        return count == 2 || count == 3;
    }

    private static String arityText(String name) {
        if (name.equals("geffect") || name.equals("geff")) {
            return "1 or 2 args";
        }
        return "2 or 3 args";
    }

    private static String example(String name) {
        if (isPlayer(name)) {
            return "<peffect:player,effects>";
        }
        if (isRole(name)) {
            return "<reffect:role,effects>";
        }
        return "<geffect:effects>";
    }
}
