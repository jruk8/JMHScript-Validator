package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Line-delay tags for JMHScript: {@code <delay:seconds,id?>} pauses
 * the command list (seconds as a decimal, converted to ticks, at
 * most 600), and {@code <delaycancel:id>} cancels a pending delay
 * by its string id. The delay id is optional; a blank id is a
 * shape error. Masked nested tags abstain (runtime resolves them).
 * Validation carve: evaluation stays engine-side.
 */
public final class TagDelay {

    /** Largest delay in seconds; larger values are a shape error. */
    public static final double MAX_SECONDS = 600.0;

    private TagDelay() {
    }

    /**
     * Edit-time arity for the delay tags: top-level split, quote
     * hygiene, plus the per-head shape. Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        return switch (name) {
            case "delay" -> delayError(args);
            case "delaycancel" -> cancelError(args);
            default -> Optional.empty();
        };
    }

    /** Delay: seconds plus an optional id. */
    private static Optional<String> delayError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <delay> needs seconds like <delay:1.2>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 1 || parts.size() > 2) {
            return Optional.of("Tag <delay> needs 1 or 2 args like <delay:1.2,id>.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <delay> mixes quotes.");
            }
        }
        String seconds = parts.get(0).strip();
        if (seconds.isBlank()) {
            return Optional.of("Tag <delay> needs seconds like <delay:1.2>.");
        }
        if (!seconds.contains("?")) {
            Optional<String> bad = secondsError(seconds);
            if (bad.isPresent()) {
                return bad;
            }
        }
        if (parts.size() == 2 && parts.get(1).strip().isBlank()
                && !parts.get(1).contains("?")) {
            return Optional.of("Tag <delay> id must not be blank.");
        }
        return Optional.empty();
    }

    /** Seconds shape: a finite number within 0..600. */
    private static Optional<String> secondsError(String raw) {
        double value;
        try {
            value = Double.parseDouble(raw);
        } catch (NumberFormatException unmatched) {
            return Optional.of("Tag <delay> seconds \"" + raw + "\" is not a number.");
        }
        if (!Double.isFinite(value) || value < 0 || value > MAX_SECONDS) {
            return Optional.of("Tag <delay> seconds \"" + raw
                    + "\" must be within 0..600.");
        }
        return Optional.empty();
    }

    /** Cancel: exactly one id. */
    private static Optional<String> cancelError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <delaycancel> needs an id like <delaycancel:storm>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of("Tag <delaycancel> needs 1 arg like <delaycancel:storm>.");
        }
        if (TagHeads.parsePickItem(parts.get(0)).isEmpty() && !parts.get(0).isBlank()) {
            return Optional.of("Tag <delaycancel> mixes quotes.");
        }
        if (parts.get(0).strip().isBlank() && !parts.get(0).contains("?")) {
            return Optional.of("Tag <delaycancel> needs an id like <delaycancel:storm>.");
        }
        return Optional.empty();
    }
}
