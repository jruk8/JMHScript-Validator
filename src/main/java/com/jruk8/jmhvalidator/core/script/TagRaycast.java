package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Raycast tag for JMHScript: {@code <raycast:point,dir,reach>} plus an
 * optional layer mask. The point must be a located 6-element list
 * {@code [x, y, z, world, pitch, yaw]} (world as the normalized
 * alias); the direction is used as given after normalization, so
 * callers compose with {@code <vec.normalize>} or pass unit dirs.
 * True when an included layer hits within reach, false on a clean
 * miss, {@code null} on misuse (warned) or backend miss (silent).
 * No Bukkit types: the cast runs through the roster seam.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagRaycast {

    private TagRaycast() {
    }


    /**
     * Edit-time arity for the raycast tag: top-level split, quote
     * hygiene, plus literal shape checks (literals only; dynamic
     * values validate at runtime). Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <raycast> needs 3 or 4 args like " + example() + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 3 && parts.size() != 4) {
            return Optional.of("Tag <raycast> needs 3 or 4 args like " + example() + ".");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <raycast> mixes quotes.");
            }
        }
        if (TagLists.isList(parts.get(0)) && TagLists.parse(parts.get(0)).size() != 6) {
            return Optional.of("Tag <raycast> needs a point like [x, y, z, world, 0, 0].");
        }
        if (parts.size() == 4 && !parts.get(3).isBlank() && !parts.get(3).contains("<")
                && RayMask.parse(parts.get(3)).isEmpty()) {
            return Optional.of("Tag <raycast> needs a mask like block, player, liquid, "
                    + "all, or block+liquid.");
        }
        return Optional.empty();
    }

    public static String example() {
        return "<raycast:point,dir,reach,mask>";
    }
}
