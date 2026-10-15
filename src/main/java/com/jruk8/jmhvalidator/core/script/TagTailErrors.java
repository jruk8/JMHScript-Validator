package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Trailing tag dispatch: sinks, proximity, math, loops, and defs. Split from {@link TagSyntaxErrors}. */
final class TagTailErrors {
    private TagTailErrors() {
    }

    /** Trailing tag names behind {@link #tagError}: sinks, proximity, math, loops, defs. */
    public static Optional<String> tailError(String name, String args) {
        return switch (name) {
            case "gmessage", "gmsg" -> TagSinks.messageError(name, args);
            case "pmessage", "pmsg" -> TagSinks.playerMessageError(name, args);
            case "rmessage", "rmsg" -> RoleTagSyntax.messageError(name, args);
            case "gerror", "gerrormessage" -> TagSinks.messageError(name, args);
            case "perror", "perrormessage" -> TagSinks.playerMessageError(name, args);
            case "rerror", "rerrormessage" -> RoleTagSyntax.messageError(name, args);
            case "gsound" -> TagSinks.soundError(name, args);
            case "psound" -> TagSinks.playerSoundError(name, args);
            case "lsound" -> TagSinks.locatedSoundError(name, args);
            case "rsound" -> RoleTagSyntax.soundError(name, args);
            case "phaseffect", "rhaseffect", "ghaseffect" -> TagEffects.effectError(name, args);
            case "gteleport" -> TagSinks.teleportError(name, args);
            case "pteleport" -> TagSinks.playerTeleportError(name, args);
            case "rteleport" -> RoleTagSyntax.teleportError(name, args);
            case "fill" -> TagFill.fillError(name, args);
            case "replace" -> TagReplace.replaceError(name, args);
            case "setblock", "getblock" -> TagBlocks.blocksError(name, args);
            case "pstate", "pstandingon", "pblock", "pismoving", "premoveitem", "ptitle", "pslot",
                    "clearslot", "pmaxhp.set", "pmaxhp.modify", "pmaxhp.get", "pmaxhp.clear",
                    "pfx.set", "pfx.get", "pfx.clear", "pfx.duration", "pisusinghanditem",
                    "pdamage", "puuid", "rstate", "rstandingon", "rblock", "rismoving",
                    "rremoveitem", "rtitle", "rslot", "rclearslot", "rmaxhp.set", "rmaxhp.modify",
                    "rmaxhp.get", "rmaxhp.clear", "rfx.set", "rfx.get", "rfx.clear", "rfx.duration",
                    "risusinghanditem", "rdamage", "ruuid", "gstate", "gstandingon", "gblock",
                    "gismoving", "gremoveitem", "gtitle", "gslot", "gclearslot", "gmaxhp.set",
                    "gmaxhp.modify", "gmaxhp.get", "gmaxhp.clear", "gfx.set", "gfx.get", "gfx.clear",
                    "gfx.duration", "gisusinghanditem", "gdamage", "guuid" ->
                    TagPlayers.opError(name, args);
            case "plos", "rlos", "glos" -> TagPlos.opError(name, args);
            case "overlap-players", "nearby-players" -> proximityError(name, args);
            case "distance" -> TagSyntaxErrors.topLevelArityError(name, args, 2, "<distance:loc1,loc2>");
            case "ismobhostile" -> TagEntities.opError(name, args);
            case "floor", "ceil", "round", "abs", "sign", "sqrt", "cbrt", "cos", "sin",
                    "tan", "arccos", "acos", "arcsin", "asin", "arctan", "sec", "csc",
                    "cot", "asec", "acsc", "acot", "log", "log10", "ln", "easein",
                    "easeout", "easeinout", "radtodeg", "r2d", "degtorad", "d2r" ->
                    mathUnaryError(name, args);
            case "root", "while", "for", "def", "format", "runblock", "setting", "s",
                    "modcolor" ->
                    flowError(name, args);
            default -> Optional.empty();
        };
    }

    /** Control-flow and meta tags behind {@link #tailError}. */
    private static Optional<String> flowError(String name, String args) {
        return switch (name) {
            case "root" -> TagSyntaxErrors.topLevelArityError(name, args, 2, "<root:x,n>");
            case "while", "for" -> loopError(name, args);
            case "def" -> TagSyntaxErrors.defError(name, args);
            case "format" -> TagFormat.syntaxError(name, args);
            case "runblock" -> TagRunBlock.opError(name, args);
            case "setting", "s" -> Optional.ofNullable(TagSettings.settingError(args));
            case "modcolor" -> TagSyntaxErrors.noArgsError(name, args);
            default -> throw new IllegalArgumentException("Unknown flow tag: " + name);
        };
    }

    /**
     * Loop shape: a condition or list plus a body. Split top-level
     * so list args survive, mirroring the runtime split.
     */
    public static Optional<String> loopError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs a condition or list plus a body.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 2) {
            return Optional.of("Tag <" + name + "> needs a condition or list plus a body.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

    /**

    /** Single-number math tags share one example shape. */
    public static Optional<String> mathUnaryError(String name, String args) {
        return TagSyntaxErrors.topLevelArityError(name, args, 1, "<" + name + ":x>");
    }

    /**
     * Active-players shape: one role, HUNTER, SPEEDRUNNER, or ALL.
     * A masked nested tag abstains (runtime resolves it).
     */
    /**

    /**
     * Proximity shape: an origin or player, a role, a radius, and
     * a max. A masked nested role abstains (runtime resolves it).
     */
    public static Optional<String> proximityError(String name, String args) {
        String shape = name.equals("overlap-players") ? "an origin" : "a player";
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs " + shape
                    + ", a role, a radius, and a max.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 4) {
            return Optional.of("Tag <" + name + "> needs " + shape
                    + ", a role, a radius, and a max.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        String role = TagHeads.parsePickItem(parts.get(1)).orElse("").strip()
                .toUpperCase(Locale.ROOT);
        if (role.contains("?")) {
            return Optional.empty();
        }
        if (!role.equals("HUNTER") && !role.equals("SPEEDRUNNER") && !role.equals("ALL")) {
            return Optional.of("Tag <" + name + "> needs HUNTER, SPEEDRUNNER, or ALL.");
        }
        return Optional.empty();
    }
}
