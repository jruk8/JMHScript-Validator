package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Entity-type tags behind {@code <ismobhostile:type>} and
 * {@code <islivingentity:type>}: true when the named type counts
 * as hostile to players or spawns a living entity. Hostility
 * follows the platform enemy marker, liveness the living marker,
 * so reads need enum metadata only and unit tests cover them
 * directly.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagEntities {

    private TagEntities() {
    }

    /**
     * Edit-time shape for the entity-type reads: one entity type,
     * quotes parsed. Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        String example = "islivingentity".equals(name)
                ? "<islivingentity:ZOMBIE>" : "<ismobhostile:ZOMBIE>";
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs an entity type like "
                    + example + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of("Tag <" + name + "> needs an entity type like "
                    + example + ".");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

}
