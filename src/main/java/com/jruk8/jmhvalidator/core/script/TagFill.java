package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * {@code <fill>} cuboid tag. Parses the request and delegates to the
 * world-fill seam; the match layer chooses the WorldEdit API or the
 * direct fill behind it. No Bukkit types, so unit tests cover it
 * directly.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagFill {

    private TagFill() {
    }


    /**
     * Edit-time shape for {@code <fill>}: world, pos1, pos2,
     * material, and mode. Mirrors the runtime warns.
     */
    static Optional<String> fillError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of(
                    "Tag <" + name + "> needs world, pos1, pos2, material, and mode.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 5) {
            return Optional.of(
                    "Tag <" + name + "> needs world, pos1, pos2, material, and mode like "
                            + "<fill:world,[0,60,0],[2,62,2],lava,set>.");
        }
        return Optional.empty();
    }
}
