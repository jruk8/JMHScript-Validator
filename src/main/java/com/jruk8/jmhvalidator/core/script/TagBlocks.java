package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Absolute single-block tags: {@code <setblock:loc,material>}
 * writes one block, {@code <getblock:loc>} reads one,
 * {@code <issolid:material>} tests one for collision. Locs are
 * strict 4- or 6-element lists; materials read non-blank.
 * Validation carve: evaluation stays engine-side.
 */
public final class TagBlocks {

    private TagBlocks() {
    }

    /**
     * Edit-time shape for the block tags. Mirrors the runtime
     * warns: arity, loc-list shape, and a non-blank material.
     */
    public static Optional<String> blocksError(String name, String args) {
        if ("getblock".equals(name)) {
            return getblockError(name, args);
        }
        if ("issolid".equals(name)) {
            return issolidError(args);
        }
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <setblock> needs a loc and a material like "
                    + "<setblock:[0, 64, 0, world],stone>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 2) {
            return Optional.of("Tag <setblock> needs a loc and a material like "
                    + "<setblock:[0, 64, 0, world],stone>.");
        }
        Optional<String> loc = TagSinks.teleportLocationError(name, parts.get(0));
        if (loc.isPresent()) {
            return loc;
        }
        if (parts.get(1).isBlank()) {
            return Optional.of("Tag <setblock> needs a material like stone.");
        }
        if (TagHeads.parsePickItem(parts.get(1)).isEmpty()) {
            return Optional.of("Tag <setblock> mixes quotes.");
        }
        return Optional.empty();
    }

    private static Optional<String> issolidError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <issolid> needs a material like <issolid:stone>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of("Tag <issolid> needs a material like <issolid:stone>.");
        }
        if (parts.get(0).isBlank()) {
            return Optional.of("Tag <issolid> needs a material like <issolid:stone>.");
        }
        if (TagHeads.parsePickItem(parts.get(0)).isEmpty()) {
            return Optional.of("Tag <issolid> mixes quotes.");
        }
        return Optional.empty();
    }

    private static Optional<String> getblockError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of(
                    "Tag <getblock> needs a loc like <getblock:[0, 64, 0, world]>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of(
                    "Tag <getblock> needs a loc like <getblock:[0, 64, 0, world]>.");
        }
        return TagSinks.teleportLocationError(name, parts.get(0));
    }
}
