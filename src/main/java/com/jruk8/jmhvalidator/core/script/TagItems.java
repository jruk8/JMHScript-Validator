package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Inventory tags behind {@code <phasitem:player,item,qty>},
 * {@code <pcountitem:player,item>}, and {@code <pheld:player>}:
 * storage counts and the main-hand material. Reads through the
 * context roster; managers own every Bukkit call. {@code phasitem}
 * always resolves "true" or "false" so {@code == true} comparisons
 * stay total; {@code pcountitem} resolves the decimal count ("0"
 * when unreadable) so math around it stays total.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagItems {

    private TagItems() {
    }


    /**
     * Edit-time shape for {@code <phasitem>}: a player, an item, plus
     * an optional count, quotes parsed. Mirrors the runtime warns.
     */
    public static Optional<String> syntaxError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs a player, an item, "
                    + "and an optional count.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 2 || parts.size() > 3) {
            return Optional.of("Tag <" + name + "> needs a player, an item, "
                    + "and an optional count.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

    /**
     * Edit-time shape for {@code <pcountitem>}: a player plus an
     * item, quotes parsed. Mirrors the runtime warns.
     */
    public static Optional<String> countSyntaxError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs a player and an item.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 2) {
            return Optional.of("Tag <" + name + "> needs a player and an item.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }


}
