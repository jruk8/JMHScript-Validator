package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Particle tags for JMHScript: {@code <particles.line:p1,p2,settings>}
 * and siblings, with the {@code parts.} namespace alias and the
 * {@code .l} line alias. Edit-time shape only: arity, quote hygiene,
 * plus literal shape checks (literals only; dynamic values validate
 * at runtime). Mirrors the runtime warns.
 * Validation carve: evaluation stays engine-side.
 */
public final class TagParticles {

    private TagParticles() {
    }

    /**
     * Edit-time shape for the particles groups: arity, quote
     * hygiene, plus literal shape checks (literals only; dynamic
     * values validate at runtime). Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        String op = opOf(name);
        if (op.equals("cancel")) {
            return cancelError(args);
        }
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <particles." + op + "> needs " + usage(op) + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (!arityOk(op, parts.size())) {
            return Optional.of("Tag <particles." + op + "> needs " + usage(op) + ".");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <particles." + op + "> mixes quotes.");
            }
        }
        Optional<String> shape = literalShapes(op, parts);
        if (shape.isPresent()) {
            return shape;
        }
        return literalSettings(op, parts);
    }

    private static String opOf(String name) {
        String op = name;
        if (op.startsWith("particles.")) {
            op = op.substring("particles.".length());
        } else if (op.startsWith("parts.")) {
            op = op.substring("parts.".length());
        }
        return op.equals("l") ? "line" : op;
    }

    private static boolean arityOk(String op, int size) {
        return switch (op) {
            case "point", "block" -> size == 2;
            case "cube" -> size == 3 || size == 4;
            case "line", "line2d", "players", "sphere", "circle", "circle2d",
                    "rectangle", "box" -> size == 3;
            default -> false;
        };
    }

    private static String usage(String op) {
        return switch (op) {
            case "point" -> "2 args like <particles.point:p,settings>";
            case "block" -> "2 args like <particles.block:pos,settings>";
            case "cube" -> "3 or 4 args like <particles.cube:pos,scale,anchor,settings>";
            case "line" -> "3 args like <particles.line:p1,p2,settings>";
            case "line2d" -> "3 args like <particles.line2d:p1,p2,settings>";
            case "players" -> "3 args like <particles.players:alice,bob,settings>";
            case "sphere" -> "3 args like <particles.sphere:point,radius,settings>";
            case "circle" -> "3 args like <particles.circle:p1,p2,settings>";
            case "circle2d" -> "3 args like <particles.circle2d:point,radius,settings>";
            case "rectangle" -> "3 args like <particles.rectangle:pos1,pos2,settings>";
            case "box" -> "3 args like <particles.box:pos1,pos2,settings>";
            default -> "a known particles group";
        };
    }

    private static Optional<String> cancelError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <particles.cancel> needs 1 arg like "
                    + "<particles.cancel:id>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of("Tag <particles.cancel> needs 1 arg like "
                    + "<particles.cancel:id>.");
        }
        if (TagHeads.parsePickItem(parts.get(0)).isEmpty()
                && !parts.get(0).isBlank()) {
            return Optional.of("Tag <particles.cancel> mixes quotes.");
        }
        return Optional.empty();
    }

    private static Optional<String> literalShapes(String op, List<String> parts) {
        int[] slots = geometrySlots(op);
        boolean flat = op.equals("line2d") || op.equals("circle2d") || op.equals("rectangle");
        for (int slot : slots) {
            String item = parts.get(slot);
            if (item.contains("<")) {
                continue;
            }
            if (!TagLists.isList(item)) {
                return Optional.of(pointUsage(op, flat));
            }
            int size = TagLists.parse(item).size();
            if (flat ? (size != 2 && size != 3 && size != 6) : (size != 3 && size != 6)) {
                return Optional.of(pointUsage(op, flat));
            }
        }
        return literalScalars(op, parts);
    }

    private static int[] geometrySlots(String op) {
        return switch (op) {
            case "line", "line2d", "circle", "rectangle", "box" -> new int[] {0, 1};
            case "point", "sphere", "circle2d", "cube", "block" -> new int[] {0};
            default -> new int[0];
        };
    }

    private static String pointUsage(String op, boolean flat) {
        if (flat) {
            return "Tag <particles." + op + "> needs a 2D point like [x, z].";
        }
        return "Tag <particles." + op + "> needs a point like [x, y, z].";
    }

    private static Optional<String> literalScalars(String op, List<String> parts) {
        if (op.equals("sphere") || op.equals("circle2d")) {
            return literalNumber(op, parts.get(1), "a positive radius");
        }
        if (op.equals("cube")) {
            String raw = parts.get(1);
            if (raw.contains("<")) {
                return Optional.empty();
            }
            if (TagLists.isList(raw)) {
                if (TagLists.parse(raw).size() != 3) {
                    return Optional.of("Tag <particles.cube> needs a scale "
                            + "like 2 or [2, 3, 2].");
                }
                return Optional.empty();
            }
            return literalNumber(op, raw, "a scale like 2 or [2, 3, 2]");
        }
        return Optional.empty();
    }

    private static Optional<String> literalNumber(String op, String raw, String what) {
        if (raw.contains("<") || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            double value = Double.parseDouble(raw.strip());
            if (Double.isFinite(value) && value > 0.0) {
                return Optional.empty();
            }
        } catch (NumberFormatException expected) {
            // Falls through to the shape error below.
        }
        return Optional.of("Tag <particles." + op + "> needs " + what + ".");
    }

    private static Optional<String> literalSettings(String op, List<String> parts) {
        String raw = parts.get(parts.size() - 1);
        if (raw.contains("<") || raw.isBlank() || !TagLists.isList(raw)) {
            return Optional.empty();
        }
        if (TagLists.parse(raw).size() > 5) {
            return Optional.of("Particle settings take at most 5 items "
                    + "[style, spacing, mode, id, spawner].");
        }
        return Optional.empty();
    }
}
