package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Managed display tags for JMHScript:
 * {@code <display.item:id,loc,item>} spawns an item display,
 * {@code <display.block:id,loc,block>} spawns a block display,
 * {@code <display.head:id,loc,texture?>} spawns a head display with an
 * optional texture (blank means a random internal solid color),
 * {@code <display.cancel:id>} removes one, and
 * {@code <display.teleport:id,loc>} moves one. Locs are strict 4- or
 * 6-element lists. The head texture skips quote hygiene (raw user
 * content, like intercept payloads). Masked nested tags abstain
 * (runtime resolves them).
 * Validation carve: evaluation stays engine-side.
 */
public final class TagDisplay {

    private TagDisplay() {
    }

    /**
     * Edit-time arity for the display tags: top-level split, quote
     * hygiene, plus the per-head shape. Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        return switch (name) {
            case "display.item" -> itemError(args);
            case "display.block" -> blockError(args);
            case "display.head" -> headError(args);
            case "display.cancel" -> cancelError(args);
            case "display.teleport" -> teleportError(args);
            default -> Optional.empty();
        };
    }

    /** Item display: id, loc, plus an item payload. */
    private static Optional<String> itemError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <display.item> needs 3 args like "
                    + "<display.item:steve,[0, 64, 0, world],[dirt, 1]>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 3) {
            return Optional.of("Tag <display.item> needs 3 args like "
                    + "<display.item:steve,[0, 64, 0, world],[dirt, 1]>.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <display.item> mixes quotes.");
            }
        }
        if (parts.get(0).strip().isBlank() && !parts.get(0).contains("?")) {
            return Optional.of("Tag <display.item> needs an id like "
                    + "<display.item:steve,[0, 64, 0, world],[dirt, 1]>.");
        }
        if (parts.get(1).strip().isBlank()) {
            return Optional.of("Tag <display.item> needs a loc like [0, 64, 0, world].");
        }
        Optional<String> loc = locError("display.item", parts.get(1));
        if (loc.isPresent()) {
            return loc;
        }
        if (parts.get(2).strip().isBlank() && !parts.get(2).contains("?")) {
            return Optional.of("Tag <display.item> needs an item like [dirt, 1].");
        }
        return Optional.empty();
    }

    /** Block display: id, loc, plus a block material. */
    private static Optional<String> blockError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <display.block> needs 3 args like "
                    + "<display.block:steve,[0, 64, 0, world],stone>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 3) {
            return Optional.of("Tag <display.block> needs 3 args like "
                    + "<display.block:steve,[0, 64, 0, world],stone>.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <display.block> mixes quotes.");
            }
        }
        if (parts.get(0).strip().isBlank() && !parts.get(0).contains("?")) {
            return Optional.of("Tag <display.block> needs an id like "
                    + "<display.block:steve,[0, 64, 0, world],stone>.");
        }
        if (parts.get(1).strip().isBlank()) {
            return Optional.of("Tag <display.block> needs a loc like [0, 64, 0, world].");
        }
        Optional<String> loc = locError("display.block", parts.get(1));
        if (loc.isPresent()) {
            return loc;
        }
        if (parts.get(2).strip().isBlank() && !parts.get(2).contains("?")) {
            return Optional.of("Tag <display.block> needs a block like stone.");
        }
        return Optional.empty();
    }

    /** Head display: id, loc, plus an optional texture. */
    private static Optional<String> headError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <display.head> needs 2 or 3 args like "
                    + "<display.head:steve,[0, 64, 0, world],red>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 2 || parts.size() > 3) {
            return Optional.of("Tag <display.head> needs 2 or 3 args like "
                    + "<display.head:steve,[0, 64, 0, world],red>.");
        }
        for (int index = 0; index < 2; index++) {
            String part = parts.get(index);
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <display.head> mixes quotes.");
            }
        }
        if (parts.get(0).strip().isBlank() && !parts.get(0).contains("?")) {
            return Optional.of("Tag <display.head> needs an id like "
                    + "<display.head:steve,[0, 64, 0, world],red>.");
        }
        if (parts.get(1).strip().isBlank()) {
            return Optional.of("Tag <display.head> needs a loc like [0, 64, 0, world].");
        }
        return locError("display.head", parts.get(1));
    }

    /** Cancel: exactly one id. */
    private static Optional<String> cancelError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <display.cancel> needs an id like <display.cancel:steve>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of("Tag <display.cancel> needs 1 arg like <display.cancel:steve>.");
        }
        if (TagHeads.parsePickItem(parts.get(0)).isEmpty() && !parts.get(0).isBlank()) {
            return Optional.of("Tag <display.cancel> mixes quotes.");
        }
        if (parts.get(0).strip().isBlank() && !parts.get(0).contains("?")) {
            return Optional.of("Tag <display.cancel> needs an id like <display.cancel:steve>.");
        }
        return Optional.empty();
    }

    /** Teleport: id plus a loc list. */
    private static Optional<String> teleportError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <display.teleport> needs 2 args like "
                    + "<display.teleport:steve,[0, 64, 0, world]>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 2) {
            return Optional.of("Tag <display.teleport> needs 2 args like "
                    + "<display.teleport:steve,[0, 64, 0, world]>.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <display.teleport> mixes quotes.");
            }
        }
        if (parts.get(0).strip().isBlank() && !parts.get(0).contains("?")) {
            return Optional.of("Tag <display.teleport> needs an id like "
                    + "<display.teleport:steve,[0, 64, 0, world]>.");
        }
        if (parts.get(1).strip().isBlank()) {
            return Optional.of("Tag <display.teleport> needs a loc like [0, 64, 0, world].");
        }
        return locError("display.teleport", parts.get(1));
    }

    /** Loc shape: a 4- or 6-element list with finite coords. */
    private static Optional<String> locError(String head, String raw) {
        if (raw.contains("?")) {
            return Optional.empty();
        }
        if (!TagLists.isList(raw)) {
            return Optional.of("Tag <" + head + "> needs a loc like [0, 64, 0, world].");
        }
        List<String> elements = TagLists.parse(raw);
        if (elements.size() != 4 && elements.size() != 6) {
            return Optional.of("Tag <" + head + "> needs a loc like [0, 64, 0, world].");
        }
        for (int index = 0; index < 3; index++) {
            if (!elements.get(index).contains("?") && finiteDouble(elements.get(index)).isEmpty()) {
                return Optional.of("Tag <" + head + "> needs a loc like [0, 64, 0, world].");
            }
        }
        Optional<String> world = TagHeads.parsePickItem(elements.get(3));
        if (world.isEmpty() || world.get().isBlank()) {
            return Optional.of("Tag <" + head + "> needs a loc like [0, 64, 0, world].");
        }
        if (elements.size() == 6) {
            for (int index = 4; index < 6; index++) {
                if (!elements.get(index).contains("?")
                        && finiteDouble(elements.get(index)).isEmpty()) {
                    return Optional.of("Tag <" + head + "> needs a loc like "
                            + "[0, 64, 0, world].");
                }
            }
        }
        return Optional.empty();
    }

    /** Finite double, or empty when unparsable or non-finite. */
    private static Optional<Double> finiteDouble(String raw) {
        Optional<String> item = TagHeads.parsePickItem(raw);
        if (item.isEmpty()) {
            return Optional.empty();
        }
        try {
            double value = Double.parseDouble(item.get().strip());
            return Double.isFinite(value) ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException unparsable) {
            return Optional.empty();
        }
    }
}
