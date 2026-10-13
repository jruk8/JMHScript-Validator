package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * The lazy {@code <switch>} multi-way dispatch tag:
 * {@code <switch:value,case,result,...[,default]>}.
 *
 * <p>The value and every case label evaluate exactly once, compare as strings after one quote
 * layer is stripped, and only the first matching result arm (or the trailing default, when the
 * arm count is odd) evaluates. Every other arm stays inert, so effect tags in unselected arms
 * never fire. The tag pre-pass defers switch bodies the same way it defers lazy if branches; the
 * plain dispatch entry below only runs when no pre-pass span claims the tag, in which case the
 * arms are already resolved text. No Bukkit types.
  * Validation carve: evaluation stays engine-side.
*/
final class TagSwitch {


    private TagSwitch() {
    }


    static Optional<String> opError(String name, String args) {
        if (args == null) {
            return Optional.of("Tag <" + name + "> needs a value plus at least one result "
                    + "or default: <switch:value,case,result,...[,default]>");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 2) {
            return Optional.of("Tag <" + name + "> needs a value plus at least one result "
                    + "or default: <switch:value,case,result,...[,default]>");
        }
        for (String part : parts) {
            Optional<String> parsed = TagHeads.parsePickItem(part);
            if (parsed.isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <" + name + "> has a malformed quoted part: " + args);
            }
        }
        return Optional.empty();
    }


}
