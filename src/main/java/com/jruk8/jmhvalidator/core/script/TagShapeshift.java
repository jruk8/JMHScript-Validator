package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Shapeshift tags for JMHScript: {@code <pshapeshift.mob:player,flag?>}
 * opens the sneak-jump prompt for the player's pending mob (their last
 * mob kill), and {@code <pshapeshift.exit:player>} closes a live shift
 * now. The flag is an optional true or false (default false); true
 * lands without per-mob abilities. Masked nested tags abstain
 * (runtime resolves them).
 * Validation carve: evaluation stays engine-side.
 */
public final class TagShapeshift {

    private TagShapeshift() {
    }

    /**
     * Edit-time arity for the shapeshift tags: top-level split, quote
     * hygiene, plus the per-head shape. Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        return switch (name) {
            case "pshapeshift.mob" -> mobError(args);
            case "pshapeshift.exit" -> exitError(args);
            default -> Optional.empty();
        };
    }

    /** Prompt: one player plus an optional true or false flag. */
    private static Optional<String> mobError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <pshapeshift.mob> needs a player like "
                    + "<pshapeshift.mob:Steve>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 1 || parts.size() > 2) {
            return Optional.of("Tag <pshapeshift.mob> needs 1 or 2 args like "
                    + "<pshapeshift.mob:Steve,true>.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <pshapeshift.mob> mixes quotes.");
            }
        }
        if (parts.get(0).strip().isBlank() && !parts.get(0).contains("?")) {
            return Optional.of("Tag <pshapeshift.mob> needs a player like "
                    + "<pshapeshift.mob:Steve>.");
        }
        if (parts.size() == 2 && !parts.get(1).contains("?")) {
            return flagError(parts.get(1).strip());
        }
        return Optional.empty();
    }

    /** Flag: true or false, case-blind; quoted flags parse first. */
    private static Optional<String> flagError(String raw) {
        Optional<String> item = TagHeads.parsePickItem(raw);
        if (item.isEmpty()) {
            return Optional.of("Tag <pshapeshift.mob> flag \"" + raw
                    + "\" must be true or false.");
        }
        String flag = item.get().strip();
        if (flag.equalsIgnoreCase("true") || flag.equalsIgnoreCase("false")) {
            return Optional.empty();
        }
        return Optional.of("Tag <pshapeshift.mob> flag \"" + raw
                + "\" must be true or false.");
    }

    /** Exit: exactly one player. */
    private static Optional<String> exitError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <pshapeshift.exit> needs a player like "
                    + "<pshapeshift.exit:Steve>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of("Tag <pshapeshift.exit> needs 1 arg like "
                    + "<pshapeshift.exit:Steve>.");
        }
        if (TagHeads.parsePickItem(parts.get(0)).isEmpty() && !parts.get(0).isBlank()) {
            return Optional.of("Tag <pshapeshift.exit> mixes quotes.");
        }
        if (parts.get(0).strip().isBlank() && !parts.get(0).contains("?")) {
            return Optional.of("Tag <pshapeshift.exit> needs a player like "
                    + "<pshapeshift.exit:Steve>.");
        }
        return Optional.empty();
    }
}
