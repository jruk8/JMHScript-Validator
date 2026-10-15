package com.jruk8.jmhvalidator.core;

import java.util.Locale;

/**
 * Setting value kinds. Names are upper-case on the wire;
 * parsing accepts any case. Single source of truth for the
 * engine setting types and the validator default checks.
 */
public enum SettingType {
    BOOLEAN,
    INTEGER,
    DOUBLE,
    STRING,
    ENUM,
    MATERIAL,
    BLOCK,
    SOLID_BLOCK,
    COLOR,
    LIST,
    EFFECTS,
    ITEMS,
    WEIGHTED_ITEM,
    MESSAGE,
    SOUND;

    /** Parses a type name case-insensitively, or null when unknown. */
    public static SettingType parse(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
