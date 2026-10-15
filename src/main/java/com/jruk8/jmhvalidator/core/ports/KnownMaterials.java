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

    /**
     * Uppercase names the engine counts as blocks (placeable,
     * air excluded). Empty on registries that predate the key;
     * coercion falls back to known-name checks then.
     */
    default Set<String> blocks() {
        return Set.of();
    }

    /**
     * Uppercase names the engine counts as solid blocks
     * (collidable). Empty on registries that predate the key;
     * coercion falls back to known-name checks then.
     */
    default Set<String> solidBlocks() {
        return Set.of();
    }

    /** True when the engine accepts this normalized name. */
    default boolean isKnown(String normalized) {
        return normalized != null && materials().contains(normalized);
    }

    /** True when the normalized name is a block on this registry. */
    default boolean isBlock(String normalized) {
        return normalized != null && blocks().contains(normalized);
    }

    /** True when the normalized name is a solid block on this registry. */
    default boolean isSolidBlock(String normalized) {
        return normalized != null && solidBlocks().contains(normalized);
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

    /** Fixed registry with block and solid-block sets. */
    static KnownMaterials of(Collection<String> materials, Collection<String> blocks,
            Collection<String> solidBlocks) {
        Set<String> names = Set.copyOf(materials);
        Set<String> blockNames = Set.copyOf(blocks);
        Set<String> solidNames = Set.copyOf(solidBlocks);
        return new KnownMaterials() {
            @Override
            public Set<String> materials() {
                return names;
            }

            @Override
            public Set<String> blocks() {
                return blockNames;
            }

            @Override
            public Set<String> solidBlocks() {
                return solidNames;
            }
        };
    }
}
