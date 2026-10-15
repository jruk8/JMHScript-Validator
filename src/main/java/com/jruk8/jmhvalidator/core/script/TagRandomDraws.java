package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Shape checks behind the parameterized random draws: arg
 * counts, single-or-pair ranges, whole numbers where the
 * runtime refuses decimals, and clamp notes where literals
 * fall outside the runtime bounds. Masked nested tags ("?")
 * resolve inside-out at runtime and are never judged here.
 */
public final class TagRandomDraws {
    private TagRandomDraws() {
    }

    /** Arity and range shapes for one random draw tag. */
    public static Optional<String> randomDrawError(String name, String args) {
        return switch (name) {
            case "random-sound" -> soundError(args);
            case "random-effect" -> effectError(args);
            case "random-enchant" -> enchantError(args);
            case "random-biome" -> biomeError(args);
            case "random-hunter", "random-runner" ->
                    TagSyntaxErrors.noArgsError(name, args);
            default -> Optional.empty();
        };
    }

    private static Optional<String> soundError(String args) {
        List<String> parts = topArgs(args);
        if (parts.size() > 2) {
            return Optional.of("Tag <random-sound> takes at most two args like"
                    + " <random-sound:[0.8,1.2],[1.0]>.");
        }
        if (parts.size() > 0) {
            Optional<String> pitch = doubleRangeError(parts.get(0), "pitch");
            if (pitch.isPresent()) {
                return pitch.map(note -> "Tag <random-sound> " + note);
            }
            Optional<String> clamped = clampNote(parts.get(0), 0.0, 2.0,
                    "pitch clamps to 0..2");
            if (clamped.isPresent()) {
                return clamped.map(note -> "Tag <random-sound> " + note);
            }
        }
        if (parts.size() > 1) {
            Optional<String> volume = doubleRangeError(parts.get(1), "volume");
            if (volume.isPresent()) {
                return volume.map(note -> "Tag <random-sound> " + note);
            }
            Optional<String> floored = floorNote(parts.get(1), 0.0, "volume bottoms at 0");
            if (floored.isPresent()) {
                return floored.map(note -> "Tag <random-sound> " + note);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> effectError(String args) {
        List<String> parts = topArgs(args);
        if (parts.size() > 2) {
            return Optional.of("Tag <random-effect> takes at most two args like"
                    + " <random-effect:[0,2],[30]>.");
        }
        if (parts.size() > 0) {
            Optional<String> amp = intRangeError(parts.get(0), "amplifier");
            if (amp.isPresent()) {
                return amp.map(note -> "Tag <random-effect> " + note);
            }
            Optional<String> clamped = clampNote(parts.get(0), 0, 255,
                    "amplifier clamps to 0..255");
            if (clamped.isPresent()) {
                return clamped.map(note -> "Tag <random-effect> " + note);
            }
        }
        if (parts.size() > 1) {
            Optional<String> secs = intRangeError(parts.get(1), "seconds");
            if (secs.isPresent()) {
                return secs.map(note -> "Tag <random-effect> " + note);
            }
            Optional<String> floored = floorNote(parts.get(1), -1, "seconds bottom at -1");
            if (floored.isPresent()) {
                return floored.map(note -> "Tag <random-effect> " + note);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> enchantError(String args) {
        List<String> parts = topArgs(args);
        if (parts.size() > 1) {
            return Optional.of("Tag <random-enchant> takes at most one arg like"
                    + " <random-enchant:[1,3]>.");
        }
        if (parts.size() > 0) {
            Optional<String> level = intRangeError(parts.get(0), "level");
            if (level.isPresent()) {
                return level.map(note -> "Tag <random-enchant> " + note);
            }
            Optional<String> clamped = clampNote(parts.get(0), 0, 255,
                    "level clamps to 0..255");
            if (clamped.isPresent()) {
                return clamped.map(note -> "Tag <random-enchant> " + note);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> biomeError(String args) {
        List<String> parts = topArgs(args);
        if (parts.size() > 1) {
            return Optional.of("Tag <random-biome> takes at most one world like"
                    + " <random-biome:world>.");
        }
        return Optional.empty();
    }

    /** Top-level args, blanks dropped; blank input reads empty. */
    private static List<String> topArgs(String args) {
        if (args == null || args.isBlank()) {
            return List.of();
        }
        return TagLists.splitTopLevel(args).stream()
                .map(String::trim).filter(part -> !part.isEmpty()).toList();
    }

    /** Whole-number range shape: single, [single], or [min, max]. */
    private static Optional<String> intRangeError(String raw, String what) {
        if (raw.contains("?")) {
            return Optional.empty();
        }
        List<String> bounds = rangeBounds(raw);
        if (bounds == null) {
            return Optional.of(what + " \"" + raw.trim()
                    + "\" is not a whole number or [min, max] pair.");
        }
        for (String bound : bounds) {
            try {
                Integer.parseInt(bound.strip());
            } catch (NumberFormatException unmatched) {
                return Optional.of(what + " \"" + raw.trim()
                        + "\" needs whole numbers.");
            }
        }
        return Optional.empty();
    }

    /** Decimal range shape: single, [single], or [min, max]. */
    private static Optional<String> doubleRangeError(String raw, String what) {
        if (raw.contains("?")) {
            return Optional.empty();
        }
        List<String> bounds = rangeBounds(raw);
        if (bounds == null) {
            return Optional.of(what + " \"" + raw.trim()
                    + "\" is not a number or [min, max] pair.");
        }
        for (String bound : bounds) {
            try {
                double value = Double.parseDouble(bound.strip());
                if (!Double.isFinite(value)) {
                    return Optional.of(what + " \"" + raw.trim()
                            + "\" must be finite.");
                }
            } catch (NumberFormatException unmatched) {
                return Optional.of(what + " \"" + raw.trim()
                        + "\" is not a number or [min, max] pair.");
            }
        }
        return Optional.empty();
    }

    /** Literal bounds entirely outside a clamp window. */
    private static Optional<String> clampNote(String raw, double floor, double ceiling,
            String note) {
        List<Double> literals = literalDoubles(raw);
        if (literals == null || literals.isEmpty()) {
            return Optional.empty();
        }
        double low = Math.min(literals.get(0), literals.get(literals.size() - 1));
        double high = Math.max(literals.get(0), literals.get(literals.size() - 1));
        if (high < floor || low > ceiling) {
            return Optional.of(note + ".");
        }
        return Optional.empty();
    }

    /** Literal bounds entirely below a floor. */
    private static Optional<String> floorNote(String raw, double floor, String note) {
        List<Double> literals = literalDoubles(raw);
        if (literals == null || literals.isEmpty()) {
            return Optional.empty();
        }
        double high = Math.max(literals.get(0), literals.get(literals.size() - 1));
        if (high < floor) {
            return Optional.of(note + ".");
        }
        return Optional.empty();
    }

    /** Bounds of one range arg, or null when the shape refuses. */
    private static List<String> rangeBounds(String raw) {
        String text = raw.trim();
        if (text.startsWith("[") && text.endsWith("]") && text.length() >= 2) {
            List<String> parts = List.of(text.substring(1, text.length() - 1).split(",", -1))
                    .stream().map(String::trim).toList();
            if (parts.size() < 1 || parts.size() > 2) {
                return null;
            }
            return parts;
        }
        if (text.contains("[") || text.contains("]") || text.contains(",")) {
            return null;
        }
        return List.of(text);
    }

    /** Literal double bounds, or null when any bound is dynamic. */
    private static List<Double> literalDoubles(String raw) {
        List<String> bounds = rangeBounds(raw);
        if (bounds == null) {
            return null;
        }
        try {
            return bounds.stream().map(bound -> Double.parseDouble(bound.strip())).toList();
        } catch (NumberFormatException unmatched) {
            return null;
        }
    }
}
