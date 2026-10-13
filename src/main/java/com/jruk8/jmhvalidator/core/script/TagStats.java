package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Locale;

/**
 * Stat tag keys behind {@code <pstat>} and {@code <gstat>}: matching
 * is case-blind, and unknown keys are tag errors listing the valid
 * ones. Runtime reads stay engine-side.
 */
public final class TagStats {

    /** Player keys: live vitals plus match counters. */
    public static final List<String> PSTAT_KEYS =
            List.of("health", "hunger", "max-health", "mobs-killed", "achievements-gained",
                    "exp-level", "damage-dealt", "crafts", "blocks-broken");

    /** Global keys: the match clock plus the world clock. */
    public static final List<String> GSTAT_KEYS = List.of("duration", "daytime");

    private TagStats() {
    }

    /** Normalizes a stat key: trimmed and case-blind. */
    public static String normalizeKey(String key) {
        return key.strip().toLowerCase(Locale.ROOT);
    }

    /** True when the normalized key names a player stat. */
    public static boolean isPlayerKey(String normalized) {
        return PSTAT_KEYS.contains(normalized);
    }

    /** True when the normalized key names a global stat. */
    public static boolean isGlobalKey(String normalized) {
        return GSTAT_KEYS.contains(normalized);
    }


}
