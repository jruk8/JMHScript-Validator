package com.jruk8.jmhvalidator.core.script;

import com.jruk8.jmhvalidator.core.ports.TagRegistry;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Per-tag validation errors behind {@link CommandSyntax}. */
final class TagSyntaxErrors {
    private static final Pattern LETTERS_ONLY = Pattern.compile("[A-Za-z]+");

    private TagSyntaxErrors() {
    }

    public static Optional<String> tagError(String body) {
        String name = TagHeads.tagName(body);
        int separator = body.indexOf(':');
        String args = separator < 0 ? null : body.substring(separator + 1);
        return switch (name) {
            case "random-num" -> randomNumberError(args); case "random-pick" -> randomPickError(args);
            case "id", "i", "pi", "e" -> noArgsError(name, args);
            case "min", "max" -> arityError(name, args, 2, "two numbers");
            case "clamp" -> arityError(name, args, 3, "a value plus low and high");
            case "atan2" -> arityError(name, args, 2, "y and x");
            case "lerp" -> arityError(name, args, 3, "a start, an end, and a blend");
            case "smoothstep", "smootherstep" ->
                    arityError(name, args, 3, "two edges and a value");
            case "sum", "average", "avg", "median" ->
                    arityAtLeastError(name, args, 1, "one or more numbers");
            case "if" -> ifError(args); case "switch" -> TagSwitch.opError(name, args);
            case "pstat" -> playerStatError(name, args);
            case "gstat" -> statError(name, args, 0, TagStats.GSTAT_KEYS);
            case "rstat" -> roleStatError(name, args);
            case "phasitem", "pcountitem", "rhasitem", "rcountitem", "ghasitem", "gcountitem" ->
                    itemError(name, args);
            case "gflag", "gf", "pflag", "pf", "lflag", "lf" -> flagError(name, args);
            case "rflag" -> RoleTagSyntax.flagError(name, args);
            case "placeholder" -> arityError(name, args, 1, "one key");
            case "loseplayer" -> loseplayerError(name, args);
            case "win" -> winError(name, args);
            case "pswitch", "rswitch", "gswitch" -> pswitchError(name, args);
            case "args" -> argsError(name, args);
            case "default" -> topLevelArityError(name, args, 2, "<default:value,fallback>");
            case "active-players" -> activePlayersError(name, args);
            case "rcolor" -> rcolorError(name, args);
            case "plocation", "ploc", "prole", "peliminated", "pworld", "world", "px", "py",
                    "pz", "pyaw", "ppitch", "pheld", "pdirx", "pdiry", "pdirz" ->
                    playerNameError(name, args);
            case "rlocation", "rloc", "reliminated", "rworld", "rx", "ry", "rz", "ryaw",
                    "rpitch", "rheld", "rdirx", "rdiry", "rdirz" -> roleNameError(name, args);
            case "glocation", "gloc", "geliminated", "gworld", "gx", "gy", "gz", "gyaw",
                    "gpitch", "gheld", "gdirx", "gdiry", "gdirz" -> arglessError(name, args);
            case "vec.add", "vec.sub", "vec.mult", "vec.normalize", "vec.mag", "vec.mag2d", "vec.sqrdist", "vec.dist",
                    "vec.sqrdist2d", "vec.dist2d", "vec.dot", "vec.cross", "loc.shift", "pdir",
                    "rdir", "gdir" ->
                    TagVectors.opError(name, args);
            case "raycast", "raycastext" -> TagRaycast.opError(name, args);
            case "intercept", "give", "dropitem", "itemtag", "rgive", "ggive", "ritemtag",
                    "gitemtag" ->
                    TagItemOps.opError(name, args);
            case "summon" -> TagSummon.opError(name, args);
            case "gamerule", "rule", "timeset", "timeadd", "timeget" ->
                    TagWorldClock.opError(name, args);
            case "particles.line", "particles.l", "particles.line2d", "particles.point", "particles.players",
                    "particles.sphere", "particles.circle", "particles.circle2d", "particles.rectangle",
                    "particles.box", "particles.cube", "particles.block", "particles.cancel", "parts.line",
                    "parts.l", "parts.line2d", "parts.point", "parts.players", "parts.sphere", "parts.circle",
                    "parts.circle2d", "parts.rectangle", "parts.box", "parts.cube", "parts.block",
                    "parts.cancel" -> TagParticles.opError(name, args);
            default -> collectionsError(name, args);
        };
    }

    /** Collection, text, and cooldown tags behind {@link #tagError}. */
    public static Optional<String> collectionsError(String name, String args) {
        return switch (name) {
            case "range" -> rangeError(name, args);
            case "weighted-pick", "weighted-picks" -> TagWeighted.opError(name, args);
            case "list.append", "list.get", "list.set", "list.remove", "list.contains",
                    "list.clear", "list.pop", "len", "list.shuffle", "list.filter", "list.reverse",
                    "list.join", "list.slice", "list.first", "list.last" ->
                    TagLists.opError(name, args);
            case "l.append", "l.get", "l.set", "l.remove", "l.contains",
                    "l.clear", "l.pop", "l.shuffle", "l.filter", "l.reverse", "l.join",
                    "l.slice", "l.first", "l.last" ->
                    TagLists.opError(name, args);
            case "map.get", "map.add", "map.set", "map.remove", "map.contains",
                    "map.keys", "map.values", "map.size", "map.clear",
                    "m.get", "m.add", "m.set", "m.remove", "m.contains",
                    "m.keys", "m.values", "m.size", "m.clear" ->
                    TagMaps.opError(name, args);
            case "set.add", "set.remove", "set.contains", "set.size", "set.clear",
                    "set.tolist" ->
                    TagSets.opError(name, args);
            case "str.join", "str.split", "str.lower", "str.upper", "str.contains",
                    "str.slice", "str.matches" ->
                    TagStrings.opError(name, args);
            case "string.listformat", "string.lformat", "s.listformat", "s.lformat" ->
                    TagStrings.opError(name, args);
            case "pcooldown", "pcooldown.get", "pcooldown.reset", "gcooldown", "gcooldown.get",
                    "gcooldown.reset", "rcooldown", "rcooldown.get", "rcooldown.reset" ->
                    TagCooldowns.syntaxError(name, args);
            default -> tailError(name, args);
        };
    }

    /**
     * Player stat shape: a player plus a key plus an optional
     * qualifier. The player is strictly first, never defaulted.
     */
    private static Optional<String> playerStatError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs player,key[,qualifier].");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 2 || parts.size() > 3) {
            return Optional.of("Tag <" + name + "> needs player,key[,qualifier].");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return statKeyError(name, parts.get(1), parts.size() == 3 ? parts.get(2) : null);
    }

    /** Role stat shape: a role plus a key plus an optional qualifier. */
    private static Optional<String> roleStatError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs role,key[,qualifier].");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 2 || parts.size() > 3) {
            return Optional.of("Tag <" + name + "> needs role,key[,qualifier].");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        Optional<String> role = RoleTagSyntax.headError(name, parts.get(0));
        if (role.isPresent()) {
            return role;
        }
        return statKeyError(name, parts.get(1), parts.size() == 3 ? parts.get(2) : null);
    }

    /**
     * Player-universe key plus qualifier check shared by pstat and
     * rstat: existing keys take no qualifier, typed vanilla stats
     * require one, and anything else is an unknown key. A masked key
     * abstains; qualifier values stay runtime-checked.
     */
    private static Optional<String> statKeyError(String name, String keyPart,
            String qualifierPart) {
        String key = TagHeads.parsePickItem(keyPart).orElse("");
        if (key.contains("?")) {
            return Optional.empty();
        }
        String normalized = TagStats.normalizeKey(key);
        if (TagStats.isPlayerKey(normalized)) {
            if (qualifierPart != null) {
                return Optional.of("Tag <" + name + "> key '" + key.strip()
                        + "' takes no qualifier.");
            }
            return Optional.empty();
        }
        Optional<TagStats.BukkitStatType> type = TagStats.bukkitType(normalized);
        if (type.isEmpty()) {
            return Optional.of("Unknown <" + name + "> key '" + key.strip() + "'. Valid keys: "
                    + String.join(", ", TagStats.PSTAT_KEYS)
                    + ", plus any vanilla statistic as bukkit.<name>, b.<name>, or bare.");
        }
        if (type.get() == TagStats.BukkitStatType.UNTYPED) {
            if (qualifierPart != null) {
                return Optional.of("Tag <" + name + "> key '" + key.strip()
                        + "' takes no qualifier.");
            }
            return Optional.empty();
        }
        if (qualifierPart == null) {
            String bare = TagStats.stripBukkitPrefix(normalized);
            return Optional.of("Tag <" + name + "> stat '" + bare + "' needs "
                    + qualifierNoun(type.get()) + " like <" + name + ":" + exampleWho(name)
                    + "b." + bare + "," + qualifierSample(type.get()) + ">.");
        }
        return Optional.empty();
    }

    /** Qualifier noun behind the missing-qualifier error. */
    private static String qualifierNoun(TagStats.BukkitStatType type) {
        return switch (type) {
            case BLOCK -> "a block";
            case ITEM -> "an item";
            case ENTITY -> "an entity";
            case UNTYPED -> "a value";
        };
    }

    /** Sample qualifier behind the missing-qualifier error. */
    private static String qualifierSample(TagStats.BukkitStatType type) {
        return switch (type) {
            case BLOCK -> "stone";
            case ITEM -> "torch";
            case ENTITY -> "zombie";
            case UNTYPED -> "value";
        };
    }

    /** Example head behind the missing-qualifier error. */
    private static String exampleWho(String name) {
        return name.equals("rstat") ? "hunter," : "Steve,";
    }

    private static Optional<String> itemError(String name, String args) {
        return switch (name) {
            case "phasitem", "rhasitem", "ghasitem" -> TagItems.syntaxError(name, args);
            default -> TagItems.countSyntaxError(name, args);
        };
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
            case "rsound" -> RoleTagSyntax.soundError(name, args);
            case "phaseffect", "rhaseffect", "ghaseffect" -> TagEffects.effectError(name, args);
            case "gteleport" -> TagSinks.teleportError(name, args);
            case "pteleport" -> TagSinks.playerTeleportError(name, args);
            case "rteleport" -> RoleTagSyntax.teleportError(name, args);
            case "fill" -> TagFill.fillError(name, args);
            case "replace" -> TagReplace.replaceError(name, args);
            case "pstate", "pstandingon", "pblock", "pismoving", "premoveitem", "ptitle",
                    "pslot", "clearslot", "pmaxhp.set", "pmaxhp.modify", "pmaxhp.get",
                    "pmaxhp.clear", "pfx.set", "pfx.get", "pfx.clear", "pfx.duration",
                    "pisusinghanditem", "pdamage", "puuid" ->
                    TagPlayers.opError(name, args);
            case "rstate", "rstandingon", "rblock", "rismoving", "rremoveitem", "rtitle",
                    "rslot", "rclearslot", "rmaxhp.set", "rmaxhp.modify", "rmaxhp.get",
                    "rmaxhp.clear", "rfx.set", "rfx.get", "rfx.clear", "rfx.duration",
                    "risusinghanditem", "rdamage", "ruuid" ->
                    TagPlayers.opError(name, args);
            case "gstate", "gstandingon", "gblock", "gismoving", "gremoveitem", "gtitle",
                    "gslot", "gclearslot", "gmaxhp.set", "gmaxhp.modify", "gmaxhp.get",
                    "gmaxhp.clear", "gfx.set", "gfx.get", "gfx.clear", "gfx.duration",
                    "gisusinghanditem", "gdamage", "guuid" ->
                    TagPlayers.opError(name, args);
            case "plos", "rlos", "glos" -> TagPlos.opError(name, args);
            case "overlap-players", "nearby-players" -> proximityError(name, args);
            case "distance" -> topLevelArityError(name, args, 2, "<distance:loc1,loc2>");
            case "ismobhostile" -> TagEntities.opError(name, args);
            case "floor", "ceil", "round", "abs", "sign", "sqrt", "cbrt", "cos", "sin",
                    "tan", "arccos", "acos", "arcsin", "asin", "arctan", "sec", "csc",
                    "cot", "asec", "acsc", "acot", "log", "log10", "ln", "easein",
                    "easeout", "easeinout", "radtodeg", "r2d", "degtorad", "d2r" ->
                    mathUnaryError(name, args);
            case "root" -> topLevelArityError(name, args, 2, "<root:x,n>");
            case "while", "for" -> loopError(name, args);
            case "def" -> defError(name, args);
            case "format" -> TagFormat.syntaxError(name, args);
            case "runblock" -> TagRunBlock.opError(name, args);
            case "setting", "s" -> Optional.ofNullable(TagSettings.settingError(args));
            case "modcolor" -> noArgsError(name, args);
            default -> Optional.empty();
        };
    }

    public static Optional<String> defError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <def> needs a name and a body like <def:double,x+x,x>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 2 || parts.get(0).isBlank()) {
            return Optional.of("Tag <def> needs a name and a body like <def:double,x+x,x>.");
        }
        return Optional.empty();
    }

    /** Removed tags and their ledger replacements. */
    private static final Map<String, String> REMOVED_TAGS = Map.of(
            "peffect", "pfx.set", "peff", "pfx.set",
            "geffect", "gfx.set", "geff", "gfx.set",
            "reffect", "rfx.set", "reff", "rfx.set");

    public static void collectWarnings(String body, List<String> found,
            Collection<String> knownFunctions, TagRegistry registry) {
        String name = TagHeads.tagName(body);
        int separator = body.indexOf(':');
        String args = separator < 0 ? null : body.substring(separator + 1);
        if (REMOVED_TAGS.containsKey(name)) {
            found.add("Tag '<" + name + ">' was removed; use '<"
                    + REMOVED_TAGS.get(name) + ">' instead.");
            return;
        }
        if (!registry.isKnown(name) && !knownFunctions.contains(name)) {
            found.add("Unknown tag '<" + body.trim() + ">', left untouched at runtime.");
            return;
        }
        if ("random-pick".equals(name) && args != null) {
            List<String> items = TagPicks.pickItems(args);
            for (int index = 0; index < items.size(); index++) {
                if (TagHeads.parsePickItem(items.get(index)).isEmpty()) {
                    found.add("Skipping invalid <random-pick> item (" + items.get(index).trim()
                            + ") at index " + index + ".");
                }
            }
        } else if ("all-players".equals(name) && args != null && !LETTERS_ONLY.matcher(args.trim()).matches()) {
            found.add("Tag <all-players:" + args.trim() + "> filter should be letters only.");
        }
    }

    public static Optional<String> randomNumberError(String args) {
        if (args == null) {
            return Optional.of("Tag <random-num> needs two numbers like <random-num:1,6>.");
        }
        List<String> bounds = TagLists.splitTopLevel(args);
        if (bounds.size() != 2) {
            return Optional.of("Tag <random-num:" + args.trim() + "> needs two numbers like <random-num:1,6>.");
        }
        Optional<String> firstItem = TagHeads.parsePickItem(bounds.get(0));
        Optional<String> secondItem = TagHeads.parsePickItem(bounds.get(1));
        if (firstItem.isEmpty() || secondItem.isEmpty()) {
            return Optional.of("Tag <random-num:" + args.trim() + "> mixes quotes.");
        }
        // A masked nested tag resolves inside-out at runtime, so its
        // value cannot be judged here; literals still must parse below.
        if (firstItem.get().contains("?") || secondItem.get().contains("?")) {
            return Optional.empty();
        }
        try {
            long first = Long.parseLong(firstItem.get().strip());
            long second = Long.parseLong(secondItem.get().strip());
            long low = Math.min(first, second);
            long high = Math.max(first, second);
            if (high - low + 1 <= 0) {
                return Optional.of("Tag <random-num:" + args.trim() + "> range is too large.");
            }
            if (low < Integer.MIN_VALUE || high > Integer.MAX_VALUE) {
                return Optional.of("Tag <random-num:" + args.trim() + "> is outside the integer range.");
            }
        } catch (NumberFormatException unmatched) {
            return Optional.of("Tag <random-num:" + args.trim() + "> needs two numbers like <random-num:1,6>.");
        }
        return Optional.empty();
    }

    public static Optional<String> randomPickError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <random-pick> needs at least one item.");
        }
        for (String item : TagPicks.pickItems(args)) {
            if (TagHeads.parsePickItem(item).isPresent()) {
                return Optional.empty();
            }
        }
        return Optional.of("Tag <random-pick:" + args.trim() + "> has no valid item.");
    }

    public static Optional<String> noArgsError(String name, String args) {
        if (args != null && !args.isBlank()) {
            return Optional.of("Tag <" + name + "> takes no arguments.");
        }
        return Optional.empty();
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
     * Loseplayer tag shape: a required head player plus an optional
     * reason after the first comma.
     */
    public static Optional<String> loseplayerError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <loseplayer> needs a player like <loseplayer:Steve>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (TagHeads.parsePickItem(parts.get(0)).isEmpty()) {
            return Optional.of("Tag <loseplayer> needs a player like <loseplayer:Steve>.");
        }
        return Optional.empty();
    }

    /**
     * Win tag shape: a head role, HUNTER or SPEEDRUNNER, plus an
     * optional reason after the first comma.
     */
    public static Optional<String> winError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <win> needs HUNTER or SPEEDRUNNER.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        Optional<String> item = TagHeads.parsePickItem(parts.get(0));
        if (item.isEmpty()) {
            return Optional.of("Tag <win> mixes quotes.");
        }
        String role = item.get().strip().toUpperCase(Locale.ROOT);
        if (role.contains("?")) {
            return Optional.empty();
        }
        if (!role.equals("HUNTER") && !role.equals("SPEEDRUNNER")) {
            return Optional.of("Tag <win> needs HUNTER or SPEEDRUNNER.");
        }
        return Optional.empty();
    }

    /**
     * Pswitch tag shape: a required head player plus a required
     * role after the first comma.
     */
    public static Optional<String> pswitchError(String name, String args) {
        String usage = switch (name) {
            case "rswitch" -> "Tag <rswitch> needs a role and a new role like <rswitch:hunter,SPEEDRUNNER>.";
            case "gswitch" -> "Tag <gswitch> needs a new role like <gswitch:SPEEDRUNNER>.";
            default -> "Tag <pswitch> needs a player and a role like <pswitch:Steve,HUNTER>.";
        };
        if (args == null || args.isBlank()) {
            return Optional.of(usage);
        }
        List<String> parts = TagLists.splitTopLevel(args);
        int arity = name.equals("gswitch") ? 1 : 2;
        if (parts.size() != arity
                || TagHeads.parsePickItem(parts.get(0)).isEmpty()) {
            return Optional.of(usage);
        }
        Optional<String> target = TagHeads.parsePickItem(parts.get(parts.size() - 1));
        if (target.isPresent() && target.get().contains("?")) {
            return Optional.empty();
        }
        if (target.flatMap(Role::parse).isEmpty()) {
            return Optional.of("Tag <" + name + "> needs a valid role like " + usageExample(name) + ".");
        }
        return Optional.empty();
    }

    private static String usageExample(String name) {
        return switch (name) {
            case "rswitch" -> "<rswitch:hunter,SPEEDRUNNER>";
            case "gswitch" -> "<gswitch:SPEEDRUNNER>";
            default -> "<pswitch:Steve,HUNTER>";
        };
    }

    /**
     * Args tag shape: bare or one whole index. A masked nested
     * tag abstains (runtime resolves it); literals must parse.
     */
    public static Optional<String> argsError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.empty();
        }
        if (args.contains("?")) {
            return Optional.empty();
        }
        try {
            Integer.parseInt(args.strip());
        } catch (NumberFormatException unmatched) {
            return Optional.of("Tag <args> needs a whole index like <args:0>.");
        }
        return Optional.empty();
    }

    /**
     * Fixed-arity shape over top-level segments (list literals stay
     * whole), each quote-clean unless blank, mirroring the runtime
     * splitter. Serves every tag whose args may hold lists.
     */
    public static Optional<String> topLevelArityError(String name, String args, int arity, String example) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs " + arity + " args like " + example + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != arity) {
            return Optional.of("Tag <" + name + "> needs " + arity + " args like " + example + ".");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

    /** Single-number math tags share one example shape. */
    public static Optional<String> mathUnaryError(String name, String args) {
        return topLevelArityError(name, args, 1, "<" + name + ":x>");
    }

    /**
     * Active-players shape: one role, HUNTER, SPEEDRUNNER, or ALL.
     * A masked nested tag abstains (runtime resolves it).
     */
    /**
     * Range shape: one to three quote-clean bounds; values are
     * checked at runtime since they may be math or nested tags.
     */
    public static Optional<String> rangeError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <range> needs stop like <range:5>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.isEmpty() || parts.size() > 3) {
            return Optional.of("Tag <range> needs stop like <range:5>.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <range> mixes quotes.");
            }
        }
        return Optional.empty();
    }

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

    public static Optional<String> activePlayersError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <active-players> needs HUNTER, SPEEDRUNNER, or ALL.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of("Tag <active-players> needs HUNTER, SPEEDRUNNER, or ALL.");
        }
        Optional<String> item = TagHeads.parsePickItem(parts.get(0));
        if (item.isEmpty()) {
            return Optional.of("Tag <active-players> mixes quotes.");
        }
        String role = item.get().strip().toUpperCase(Locale.ROOT);
        if (role.contains("?")) {
            return Optional.empty();
        }
        if (!role.equals("HUNTER") && !role.equals("SPEEDRUNNER") && !role.equals("ALL")) {
            return Optional.of("Tag <active-players> needs HUNTER, SPEEDRUNNER, or ALL.");
        }
        return Optional.empty();
    }

    /**
     * Role-color shape for {@code <rcolor>}: one present,
     * quote-clean role. Literal roles must parse; nested tags
     * resolve at runtime, so the editor cannot see through them.
     */
    public static Optional<String> rcolorError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <rcolor> needs a role like <rcolor:HUNTER>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1 || TagHeads.parsePickItem(parts.get(0)).isEmpty()) {
            return Optional.of("Tag <rcolor> needs a role like <rcolor:HUNTER>.");
        }
        String head = parts.get(0);
        if (head.contains("?")) {
            return Optional.empty();
        }
        String role = TagHeads.parsePickItem(head).orElse("");
        if (Role.parse(role).isEmpty()) {
            return Optional.of("Tag <rcolor> needs a role like <rcolor:HUNTER>.");
        }
        return Optional.empty();
    }

    /**
     * Player-name shape for {@code <plocation>}, {@code <prole>}, and
     * {@code <peliminated>}:
     * one present, quote-clean name. Like {@code <loseplayer>} the
     * name may resolve from a nested tag at runtime.
     */
    public static Optional<String> playerNameError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs a player like <" + name + ":Steve>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1 || TagHeads.parsePickItem(parts.get(0)).isEmpty()) {
            return Optional.of("Tag <" + name + "> needs a player like <" + name + ":Steve>.");
        }
        return Optional.empty();
    }

    /**
     * Role-name shape for the {@code <r*>} twins of the
     * one-player reads: one present, quote-clean role like
     * {@code <rheld:hunter>}.
     */
    public static Optional<String> roleNameError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs a role like <" + name + ":hunter>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1 || TagHeads.parsePickItem(parts.get(0)).isEmpty()) {
            return Optional.of("Tag <" + name + "> needs a role like <" + name + ":hunter>.");
        }
        return Optional.empty();
    }

    /**
     * Argless shape for the {@code <g*>} twins of the one-player
     * reads: no args, like {@code <gheld>}.
     */
    public static Optional<String> arglessError(String name, String args) {
        if (args != null && !args.isBlank()) {
            return Optional.of("Tag <" + name + "> takes no args like <" + name + ">.");
        }
        return Optional.empty();
    }

    public static Optional<String> arityError(String name, String args, int arity, String what) {
        if (args == null || args.isBlank()
                || TagLists.splitTopLevel(args).size() != arity) {
            return Optional.of("Tag <" + name + "> needs " + what + ".");
        }
        for (String part : TagLists.splitTopLevel(args)) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

    public static Optional<String> arityAtLeastError(String name, String args, int minimum,
            String what) {
        if (args == null || args.isBlank()
                || TagLists.splitTopLevel(args).size() < minimum) {
            return Optional.of("Tag <" + name + "> needs " + what + ".");
        }
        for (String part : TagLists.splitTopLevel(args)) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

    /**
     * Stat tag shape: arity, quote hygiene, plus the key. A masked
     * nested key abstains (runtime resolves it); literals must parse.
     */
    public static Optional<String> statError(String name, String args, int keyIndex,
            List<String> validKeys) {
        int arity = keyIndex + 1;
        String what = arity == 1 ? "one key" : "a player plus a key";
        if (args == null || args.isBlank()
                || TagLists.splitTopLevel(args).size() != arity) {
            return Optional.of("Tag <" + name + "> needs " + what + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        String key = TagHeads.parsePickItem(parts.get(keyIndex)).orElse("");
        if (key.contains("?")) {
            return Optional.empty();
        }
        if (!validKeys.contains(TagStats.normalizeKey(key))) {
            return Optional.of("Unknown <" + name + "> key '" + key.strip() + "'. Valid keys: "
                    + String.join(", ", validKeys) + ".");
        }
        return Optional.empty();
    }

    /** Flag tag shape: a name plus an optional value, quotes parsed. */
    public static Optional<String> flagError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs a name plus an optional value.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 1 || parts.size() > 2) {
            return Optional.of("Tag <" + name + "> needs a name plus an optional value.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        if (FlagStore.parseName(parts.get(0)).isEmpty()) {
            return Optional.of("Tag <" + name + "> needs a name.");
        }
        return Optional.empty();
    }

    public static Optional<String> ifError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <if> needs a condition plus one or two branches.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 2 || parts.size() > 3) {
            return Optional.of("Tag <if> needs a condition plus one or two branches.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <if> mixes quotes.");
            }
        }
        String condition = TagHeads.parsePickItem(parts.get(0)).orElse("").strip();
        try {
            TagExpressions.validateConditionBrackets(condition);
        } catch (TagExpressions.ExprException spacing) {
            return Optional.of("Tag <if> " + spacing.getMessage() + ".");
        }
        if (!TagExpressions.hasComparison(condition)) {
            return Optional.of("Tag <if> condition needs a comparison (==, !=, lt, le, gt, ge).");
        }
        return Optional.empty();
    }
}
