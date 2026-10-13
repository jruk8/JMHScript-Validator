package com.jruk8.jmhvalidator.core.script;

import java.util.Locale;
import java.util.Optional;

/**
 * Role port for validation: the five engine roles plus the
 * exact parse semantics. Display colors and match behavior
 * stay engine-side; validation only parses names.
 */
public enum Role {
    NONE,
    HUNTER,
    SPEEDRUNNER,
    AFK,
    SPECTATOR;

    /** Parses a role name case-insensitively, or empty when it matches nothing. */
    public static Optional<Role> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Role.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
