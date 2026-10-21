package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Ghost tags for JMHScript: {@code <pghost.enter:player,id,loc,ymode?>}
 * opens a ghost session (a static mannequin decoy plus a detached moving
 * anchor), {@code <pghost.exit:id>} closes one,
 * {@code <pghost.control:id,movement,smoothness?>} steers the anchor with
 * a 3-element movement vector and an optional 0..1 smoothness (default
 * 0.3), and {@code <pisghost:player>} reads the state. The ymode is an
 * optional WORLD (default) or CAMERA lift axis. Locs are strict 4- or
 * 6-element lists. Masked nested tags abstain (runtime resolves them).
 * Validation carve: evaluation stays engine-side.
 */
public final class TagPghost {

    private TagPghost() {
    }

    /**
     * Edit-time arity for the ghost tags: top-level split, quote
     * hygiene, plus the per-head shape. Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        return switch (name) {
            case "pghost.enter" -> enterError(args);
            case "pghost.exit" -> exitError(args);
            case "pghost.control" -> controlError(args);
            case "pisghost" -> isGhostError(args);
            default -> Optional.empty();
        };
    }

    /** Enter: player, id, a loc list, plus an optional ymode. */
    private static Optional<String> enterError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <pghost.enter> needs 3 or 4 args like "
                    + "<pghost.enter:Steve,drone,[0, 64, 0, world],CAMERA>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 3 || parts.size() > 4) {
            return Optional.of("Tag <pghost.enter> needs 3 or 4 args like "
                    + "<pghost.enter:Steve,drone,[0, 64, 0, world],CAMERA>.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <pghost.enter> mixes quotes.");
            }
        }
        if (parts.get(0).strip().isBlank() && !parts.get(0).contains("?")) {
            return Optional.of("Tag <pghost.enter> needs a player like "
                    + "<pghost.enter:Steve,drone,[0, 64, 0, world]>.");
        }
        if (parts.get(1).strip().isBlank() && !parts.get(1).contains("?")) {
            return Optional.of("Tag <pghost.enter> needs an id like "
                    + "<pghost.enter:Steve,drone,[0, 64, 0, world]>.");
        }
        if (parts.get(2).strip().isBlank()) {
            return Optional.of("Tag <pghost.enter> needs a loc like "
                    + "[0, 64, 0, world].");
        }
        Optional<String> loc = locError("pghost.enter", parts.get(2));
        if (loc.isPresent()) {
            return loc;
        }
        if (parts.size() == 4) {
            return ymodeError(parts.get(3).strip());
        }
        return Optional.empty();
    }

    /** Ymode: WORLD or CAMERA, case-blind; masked tags abstain. */
    private static Optional<String> ymodeError(String raw) {
        if (raw.contains("?")) {
            return Optional.empty();
        }
        if (raw.equalsIgnoreCase("WORLD") || raw.equalsIgnoreCase("CAMERA")) {
            return Optional.empty();
        }
        return Optional.of("Tag <pghost.enter> ymode \"" + raw + "\" must be WORLD or CAMERA.");
    }

    /** Exit: exactly one id. */
    private static Optional<String> exitError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <pghost.exit> needs an id like <pghost.exit:drone>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of("Tag <pghost.exit> needs 1 arg like <pghost.exit:drone>.");
        }
        if (TagHeads.parsePickItem(parts.get(0)).isEmpty() && !parts.get(0).isBlank()) {
            return Optional.of("Tag <pghost.exit> mixes quotes.");
        }
        if (parts.get(0).strip().isBlank() && !parts.get(0).contains("?")) {
            return Optional.of("Tag <pghost.exit> needs an id like <pghost.exit:drone>.");
        }
        return Optional.empty();
    }

    /** Control: id, movement vector, plus optional smoothness. */
    private static Optional<String> controlError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <pghost.control> needs 2 or 3 args like "
                    + "<pghost.control:drone,[0, 1, 0],0.3>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 2 || parts.size() > 3) {
            return Optional.of("Tag <pghost.control> needs 2 or 3 args like "
                    + "<pghost.control:drone,[0, 1, 0],0.3>.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <pghost.control> mixes quotes.");
            }
        }
        if (parts.get(0).strip().isBlank() && !parts.get(0).contains("?")) {
            return Optional.of("Tag <pghost.control> needs an id like "
                    + "<pghost.control:drone,[0, 1, 0],0.3>.");
        }
        Optional<String> movement = movementError(parts.get(1));
        if (movement.isPresent()) {
            return movement;
        }
        if (parts.size() == 3 && !parts.get(2).contains("?")) {
            return smoothnessError(parts.get(2).strip());
        }
        return Optional.empty();
    }

    /** Movement: a 3-element finite list. */
    private static Optional<String> movementError(String raw) {
        if (raw.contains("?")) {
            return Optional.empty();
        }
        if (!TagLists.isList(raw)) {
            return Optional.of("Tag <pghost.control> needs a movement vector like [0, 1, 0].");
        }
        List<String> elements = TagLists.parse(raw);
        if (elements.size() != 3) {
            return Optional.of("Tag <pghost.control> needs a movement vector like [0, 1, 0].");
        }
        for (String element : elements) {
            if (!element.contains("?") && finiteDouble(element).isEmpty()) {
                return Optional.of("Tag <pghost.control> needs a movement vector like "
                        + "[0, 1, 0].");
            }
        }
        return Optional.empty();
    }

    /** Smoothness: a finite number within 0..1. */
    private static Optional<String> smoothnessError(String raw) {
        Optional<Double> value = finiteDouble(raw);
        if (value.isEmpty() || value.get() < 0 || value.get() > 1) {
            return Optional.of("Tag <pghost.control> smoothness \"" + raw
                    + "\" must be within 0..1.");
        }
        return Optional.empty();
    }

    /** State read: exactly one player. */
    private static Optional<String> isGhostError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <pisghost> needs a player like <pisghost:Steve>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of("Tag <pisghost> needs 1 arg like <pisghost:Steve>.");
        }
        if (TagHeads.parsePickItem(parts.get(0)).isEmpty() && !parts.get(0).isBlank()) {
            return Optional.of("Tag <pisghost> mixes quotes.");
        }
        if (parts.get(0).strip().isBlank() && !parts.get(0).contains("?")) {
            return Optional.of("Tag <pisghost> needs a player like <pisghost:Steve>.");
        }
        return Optional.empty();
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
