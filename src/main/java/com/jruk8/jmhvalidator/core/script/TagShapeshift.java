package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Shapeshift tags for JMHScript:
 * {@code <pshapeshift.mob:player,flag?,promptSeconds?,holdSeconds?>}
 * opens the sneak-jump prompt for the player's pending mob (their last
 * mob kill), and {@code <pshapeshift.exit:player>} closes a live shift
 * now. The flag is an optional true or false (default false); true
 * lands without per-mob abilities. The timings are optional seconds
 * owned by the triggering modifier (runtime clamps them); masked
 * nested tags abstain (runtime resolves them).
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

    /** Prompt: one player, an optional flag, and optional timings. */
    private static Optional<String> mobError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <pshapeshift.mob> needs a player like "
                    + "<pshapeshift.mob:Steve>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 1 || parts.size() > 4) {
            return Optional.of("Tag <pshapeshift.mob> needs 1 to 4 args like "
                    + "<pshapeshift.mob:Steve,true,5,3>.");
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
        if (parts.size() >= 2 && !parts.get(1).contains("?")) {
            Optional<String> flag = flagError(parts.get(1).strip());
            if (flag.isPresent()) {
                return flag;
            }
        }
        if (parts.size() >= 3 && !parts.get(2).contains("?")) {
            Optional<String> prompt = secondsError(parts.get(2).strip(), "prompt seconds");
            if (prompt.isPresent()) {
                return prompt;
            }
        }
        if (parts.size() >= 4 && !parts.get(3).contains("?")) {
            Optional<String> hold = secondsError(parts.get(3).strip(), "hold seconds");
            if (hold.isPresent()) {
                return hold;
            }
        }
        return Optional.empty();
    }

    /** Timing: a finite number of seconds; the runtime clamps the range. */
    private static Optional<String> secondsError(String raw, String what) {
        Optional<String> item = TagHeads.parsePickItem(raw);
        if (item.isEmpty() || item.get().isBlank()) {
            return Optional.of("Tag <pshapeshift.mob> " + what + " must be a number.");
        }
        try {
            double value = Double.parseDouble(item.get().strip());
            if (!Double.isFinite(value)) {
                return Optional.of("Tag <pshapeshift.mob> " + what + " must be finite.");
            }
        } catch (NumberFormatException unmatched) {
            return Optional.of("Tag <pshapeshift.mob> " + what + " \"" + raw
                    + "\" is not a number.");
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
