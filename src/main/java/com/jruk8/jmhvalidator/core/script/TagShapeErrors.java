package com.jruk8.jmhvalidator.core.script;

import java.util.Optional;

/** Vector, shape, session, and display head dispatch behind {@link TagSyntaxErrors}. */
final class TagShapeErrors {

    private TagShapeErrors() {
    }

    static Optional<Optional<String>> dispatch(String name, String args) {
        return switch (name) {
            case "vec.add", "vec.sub", "vec.mult", "vec.normalize", "vec.mag", "vec.mag2d", "vec.sqrdist", "vec.dist",
                    "vec.sqrdist2d", "vec.dist2d", "vec.dot", "vec.cross", "loc.shift", "pdir",
                    "rdir", "gdir", "raycast", "raycastext", "intercept", "give", "dropitem",
                    "itemtag", "itemkeep", "rgive", "ggive", "ritemtag", "ritemkeep",
                    "gitemtag", "gitemkeep", "summon", "gamerule",
                    "rule", "timeset", "timeadd", "timeget", "place", "kill", "delay", "delaycancel",
                    "pghost.enter", "pghost.exit", "pghost.control", "pisghost",
                    "pshapeshift.mob", "pshapeshift.exit", "display.item",
                    "display.block", "display.head", "display.cancel", "display.teleport" ->
                    Optional.of(shapeError(name, args));
            default -> Optional.empty();
        };
    }

    private static Optional<String> shapeError(String name, String args) {
        return switch (name) {
            case "vec.add", "vec.sub", "vec.mult", "vec.normalize", "vec.mag", "vec.mag2d", "vec.sqrdist", "vec.dist",
                    "vec.sqrdist2d", "vec.dist2d", "vec.dot", "vec.cross", "loc.shift", "pdir",
                    "rdir", "gdir" ->
                    TagVectors.opError(name, args);
            case "raycast", "raycastext" -> TagRaycast.opError(name, args);
            case "intercept", "give", "dropitem", "itemtag", "itemkeep", "rgive", "ggive",
                    "ritemtag", "ritemkeep", "gitemtag", "gitemkeep" ->
                    TagItemOps.opError(name, args);
            case "summon" -> TagSummon.opError(name, args);
            case "gamerule", "rule", "timeset", "timeadd", "timeget", "weatherget",
                    "weatherset" ->
                    TagWorldClock.opError(name, args);
            case "place", "kill" -> TagPlaceKill.opError(name, args);
            case "delay", "delaycancel" -> TagDelay.opError(name, args);
            case "pghost.enter", "pghost.exit", "pghost.control", "pisghost" ->
                    TagPghost.opError(name, args);
            case "pshapeshift.mob", "pshapeshift.exit" ->
                    TagShapeshift.opError(name, args);
            case "display.item", "display.block", "display.head", "display.cancel",
                    "display.teleport" ->
                    TagDisplay.opError(name, args);
            default -> throw new IllegalArgumentException("Unknown shape tag: " + name);
        };
    }
}
