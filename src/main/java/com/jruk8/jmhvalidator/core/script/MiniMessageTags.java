package com.jruk8.jmhvalidator.core.script;

import java.util.Set;

/**
 * MiniMessage markup the engine passes through the tag layer
 * untouched at runtime, so colors in item names and lore just
 * work. The validator exempts these spans from
 * {@code W_UNKNOWN_TAG}: warning on docs-blessed markup fails
 * author gates on constructs that render fine.
 *
 * <p>Heads starting with {@code #} (hex colors) or {@code /}
 * (closers) can never dispatch: neither char opens a tag head,
 * so the engine always passes them through. Named markers are
 * the set the shipped Adventure build resolves, verified
 * empirically (bare and arg-form deserializes); exotic names
 * outside this set keep warning, which is the safe direction.
 * Adventure owns this list, not the engine, so it lives here
 * next to the {@code ?} MiniMessage precedent instead of
 * riding the tag registry port.
 *
 * <p>One accepted blind spot: a call to a missing def whose
 * name collides with a marker (a def named {@code red}) goes
 * silent instead of warning. Defined names still resolve
 * first, and marker-named defs are pathological.
 */
public final class MiniMessageTags {

    /** Marker names the shipped Adventure build resolves. */
    private static final Set<String> MARKERS = Set.of("black", "dark_blue", "dark_green",
            "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue",
            "green", "aqua", "red", "light_purple", "yellow", "white", "obfuscated", "bold",
            "strikethrough", "underlined", "italic", "b", "i", "u", "st", "obf", "reset",
            "newline", "gradient", "rainbow", "transition", "tr", "color", "c", "click",
            "hover", "font", "selector", "score", "insert", "shadow", "lang");

    private MiniMessageTags() {
    }

    /**
     * True when this lowercased tag name is MiniMessage markup:
     * a closer, a hex color, or a resolved marker name. Null
     * reads false.
     */
    public static boolean isMarker(String name) {
        return name != null && (name.startsWith("/") || name.startsWith("#")
                || MARKERS.contains(name));
    }
}
