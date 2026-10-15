package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Effect live read: {@code <phaseffect:player,effect,min>} yields
 * true or false. Effect writes moved to the shared ledger
 * ({@code <pfx.set>} and its twins); the old {@code <peffect>}
 * family reports through the removed-tag pointer instead.
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

    private static boolean arityOk(String name, int count) {
        return count == 2 || count == 3;
    }

    private static String arityText(String name) {
        return "2 or 3 args";
    }

    private static String example(String name) {
        return "<phaseffect:player,effect>";
    }
}
