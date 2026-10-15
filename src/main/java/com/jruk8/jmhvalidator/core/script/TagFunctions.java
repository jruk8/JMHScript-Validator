package com.jruk8.jmhvalidator.core.script;

import java.util.Set;

/**
 * Builtin tag names a def must not shadow. Definitions stay
 * engine-side.
 */
public final class TagFunctions {


    /**
     * Builtin tag names a def must not shadow. Dispatch prefers
     * builtins anyway (the switch runs before the table lookup), so
     * drift here only costs the def-time warning, never a hijack.
     * Mirrors the resolveTag switch plus the pre-pass ops.
     */
    private static final Set<String> BUILTINS = Set.of(
            "p", "random-mob", "random-item", "random-block", "random-solid-block",
            "random-sound", "random-effect", "random-enchant", "random-biome",
            "random-hunter", "random-runner", "random-color",
            "random-num", "random-pick",
            "random-player", "all-players", "all-fanout", "duration", "id", "i",
            "pstat", "gstat", "rstat", "phasitem", "gflag", "gf", "pflag", "pf", "lflag", "lf", "rflag",
            "placeholder",
            "min", "max", "clamp", "root", "if", "switch",
            "gmessage", "gmsg", "pmessage", "pmsg", "rmessage", "rmsg", "gsound", "psound", "rsound",
            "lsound",
            "gerror", "gerrormessage", "perror", "perrormessage", "rerror",
            "rerrormessage",
            "loseplayer", "win", "pswitch", "args",
            "list.append", "list.get", "list.set", "list.remove", "list.contains",
            "list.clear", "list.pop", "len", "list.shuffle", "range",
            "l.append", "l.get", "l.set", "l.remove", "l.contains",
            "l.clear", "l.pop", "l.shuffle", "l.filter", "l.reverse", "l.join",
            "l.slice", "l.first", "l.last",
            "map.get", "map.add", "map.set", "map.remove", "map.contains",
            "map.keys", "map.values", "map.size", "map.clear",
            "m.get", "m.add", "m.set", "m.remove", "m.contains",
            "m.keys", "m.values", "m.size", "m.clear",
            "set.add", "set.remove", "set.contains", "set.size", "set.clear",
            "set.tolist",
            "weighted-pick", "weighted-picks",
            "string.listformat", "string.lformat", "s.listformat", "s.lformat",
            "active-players", "plocation", "prole", "rcolor", "distance",
            "floor", "ceil", "round", "abs", "sign", "sqrt", "cbrt",
            "cos", "sin", "tan", "arccos", "acos", "arcsin", "asin", "arctan",
            "sec", "csc", "cot", "asec", "acsc", "acot", "log", "log10", "ln",
            "easein", "easeout", "easeinout", "radtodeg", "r2d", "degtorad", "d2r",
            "atan2", "lerp", "smoothstep", "smootherstep", "sum", "average", "avg",
            "median", "pi", "e",
            "while", "for", "def", "run", "format");

    private TagFunctions() {
    }

    /** True when the lowercase name is a builtin a def must not shadow. */
    public static boolean isBuiltin(String lowerName) {
        return BUILTINS.contains(lowerName);
    }


}
