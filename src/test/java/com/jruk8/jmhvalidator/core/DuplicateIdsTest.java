package com.jruk8.jmhvalidator.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Duplicate-id message pins: byte-identical to the engine
 * load warnings for both file kinds.
 */
class DuplicateIdsTest {

    @Test
    void modifierMessageNamesWinnerAndSkipped() {
        assertEquals("Duplicate modifier id 'fog': 'modifiers/fog.yml' wins,"
                        + " skipping 'modifiers/deep/fog.yml'.",
                DuplicateIds.message("modifier", "fog",
                        "modifiers/fog.yml", "modifiers/deep/fog.yml"));
    }

    @Test
    void presetMessageNamesWinnerAndSkipped() {
        assertEquals("Duplicate preset id 'chaos': 'presets/chaos.yml' wins,"
                        + " skipping 'presets/old/chaos.yml'.",
                DuplicateIds.message("preset", "chaos",
                        "presets/chaos.yml", "presets/old/chaos.yml"));
    }
}
