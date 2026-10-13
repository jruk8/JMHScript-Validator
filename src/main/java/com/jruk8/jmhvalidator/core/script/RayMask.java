package com.jruk8.jmhvalidator.core.script;

import java.util.Locale;
import java.util.Optional;

/**
 * Bitwise raycast layer mask: block, player, and liquid layers in
 * one int. Users write single names ({@code block}, {@code player},
 * {@code liquid}), {@code +}-joined combos like
 * {@code block+liquid}, or the {@code all} shorthand.
 * Case-insensitive.
 */
public record RayMask(int bits) {

    /** Occluding-block layer. */
    public static final RayMask BLOCK = new RayMask(1);

    /** Player-box layer. */
    public static final RayMask PLAYER = new RayMask(2);

    /** Liquid-block layer. */
    public static final RayMask LIQUID = new RayMask(4);

    /** Every layer. */
    public static final RayMask ALL = new RayMask(1 | 2 | 4);

    /** True when the block layer is included. */
    public boolean blocks() {
        return (bits & BLOCK.bits) != 0;
    }

    /** True when the player layer is included. */
    public boolean players() {
        return (bits & PLAYER.bits) != 0;
    }

    /** True when the liquid layer is included. */
    public boolean liquids() {
        return (bits & LIQUID.bits) != 0;
    }

    /** Union of two masks. */
    public RayMask or(RayMask other) {
        return new RayMask(bits | other.bits);
    }

    /** User-friendly parse; empty when blank or any part is unknown. */
    public static Optional<RayMask> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        int bits = 0;
        for (String part : raw.split("[+|]", -1)) {
            Optional<RayMask> layer = single(part.strip());
            if (layer.isEmpty()) {
                return Optional.empty();
            }
            bits |= layer.get().bits;
        }
        return Optional.of(new RayMask(bits));
    }

    /** One mask name; empty when unknown. */
    private static Optional<RayMask> single(String name) {
        return switch (name.toUpperCase(Locale.ROOT)) {
            case "BLOCK" -> Optional.of(BLOCK);
            case "PLAYER" -> Optional.of(PLAYER);
            case "LIQUID" -> Optional.of(LIQUID);
            case "ALL" -> Optional.of(ALL);
            default -> Optional.empty();
        };
    }
}
