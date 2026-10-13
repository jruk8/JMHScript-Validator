package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * {@code <replace>} cuboid tag. Parses the request and delegates
 * to the world-replace seam; the match layer chooses the WorldEdit
 * API or the direct loop behind it. No Bukkit types, so unit tests
 * cover it directly.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagReplace {

    private TagReplace() {
    }


    /**
     * Edit-time shape for {@code <replace>}: world, pos1, pos2,
     * source, and target. Mirrors the runtime warns.
     */
    static Optional<String> replaceError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of(
                    "Tag <" + name + "> needs world, pos1, pos2, source, and target.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 5) {
            return Optional.of(
                    "Tag <" + name + "> needs world, pos1, pos2, source, and target like "
                            + "<replace:world,[0,60,0],[2,62,2],air,water>.");
        }
        return Optional.empty();
    }
}
