package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Entity-type tags behind {@code <ismobhostile:type>}: true when
 * the named type counts as hostile to players. Hostility follows
 * the platform {@link Enemy} marker, so ghasts, slimes, and
 * shulkers count alongside zombies and skeletons. Reads enum
 * metadata only (the {@link SharedRandomRolls} precedent), so no
 * server is needed and unit tests cover it directly.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagEntities {

    private TagEntities() {
    }

    /**
     * Edit-time shape for {@code <ismobhostile>}: one entity type,
     * quotes parsed. Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs an entity type like "
                    + "<ismobhostile:ZOMBIE>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of("Tag <" + name + "> needs an entity type like "
                    + "<ismobhostile:ZOMBIE>.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

}
