package com.jruk8.jmhvalidator.core.ports;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * Material-name port. The engine owns the material list (it
 * defines validity through the Bukkit registry); the
 * validator only reads it, in process from the engine
 * adapter or out of process from the exported registry. Name
 * normalization is the single source of truth for coercion
 * on both sides. The core never hardcodes material names.
 */
public interface KnownMaterials {
    /** Uppercase material names the engine accepts. */
    Set<String> materials();

    /** True when the engine accepts this normalized name. */
    default boolean isKnown(String normalized) {
        return normalized != null && materials().contains(normalized);
    }

    /**
     * Normalizes one raw material name: trimmed, uppercased,
     * spaces to underscores, the {@code MINECRAFT:} prefix
     * stripped. Mirrors the engine item resolver exactly.
     */
    static String normalize(String raw) {
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (normalized.startsWith("MINECRAFT:")) {
            normalized = normalized.substring("MINECRAFT:".length());
        }
        return normalized;
    }

    /** Fixed registry for tests and adapters. */
    static KnownMaterials of(Collection<String> materials) {
        Set<String> copy = Set.copyOf(materials);
        return () -> copy;
    }
}
