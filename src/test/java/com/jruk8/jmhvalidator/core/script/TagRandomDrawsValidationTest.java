package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Random-draw shape pins: arg counts, single-or-pair ranges,
 * whole numbers for amps/seconds/levels, clamp notes for
 * literals outside the runtime bounds, and masked nested
 * tags passing through unjudged.
 */
class TagRandomDrawsValidationTest {
    @Test
    void soundShapes() {
        assertTrue(TagRandomDraws.randomDrawError("random-sound", null).isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-sound", "").isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-sound", "1.0,0.5").isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-sound", "[0.8,1.2],[1.0]").isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-sound", "[1.2,0.8]").isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-sound", "?").isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-sound", "1.0,1.0,1.0").isPresent());
        assertTrue(TagRandomDraws.randomDrawError("random-sound", "loud").isPresent());
        assertTrue(TagRandomDraws.randomDrawError("random-sound", "[0.5,1.5,2.0]").isPresent());
        assertEquals("Tag <random-sound> pitch clamps to 0..2.",
                TagRandomDraws.randomDrawError("random-sound", "[9,9]").orElseThrow());
        assertEquals("Tag <random-sound> volume bottoms at 0.",
                TagRandomDraws.randomDrawError("random-sound", "1.0,[-3]").orElseThrow());
        assertTrue(TagRandomDraws.randomDrawError("random-sound", "[0,5]").isEmpty());
    }

    @Test
    void effectShapes() {
        assertTrue(TagRandomDraws.randomDrawError("random-effect", null).isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-effect", "2,10").isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-effect", "[0,2],[30]").isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-effect", "?,?").isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-effect", "1,2,3").isPresent());
        assertTrue(TagRandomDraws.randomDrawError("random-effect", "1.5,10").isPresent());
        assertTrue(TagRandomDraws.randomDrawError("random-effect", "2,loud").isPresent());
        assertEquals("Tag <random-effect> amplifier clamps to 0..255.",
                TagRandomDraws.randomDrawError("random-effect", "999").orElseThrow());
        assertEquals("Tag <random-effect> seconds bottom at -1.",
                TagRandomDraws.randomDrawError("random-effect", "0,[-99]").orElseThrow());
        assertTrue(TagRandomDraws.randomDrawError("random-effect", "0,-1").isEmpty());
    }

    @Test
    void enchantShapes() {
        assertTrue(TagRandomDraws.randomDrawError("random-enchant", null).isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-enchant", "3").isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-enchant", "[1,3]").isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-enchant", "?").isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-enchant", "1,2").isPresent());
        assertTrue(TagRandomDraws.randomDrawError("random-enchant", "high").isPresent());
        assertTrue(TagRandomDraws.randomDrawError("random-enchant", "1.5").isPresent());
        assertEquals("Tag <random-enchant> level clamps to 0..255.",
                TagRandomDraws.randomDrawError("random-enchant", "300").orElseThrow());
    }

    @Test
    void biomeHunterAndRunnerShapes() {
        assertTrue(TagRandomDraws.randomDrawError("random-biome", null).isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-biome", "world").isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-biome", "\"my world\"").isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-biome", "a,b").isPresent());
        assertTrue(TagRandomDraws.randomDrawError("random-hunter", null).isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-runner", null).isEmpty());
        assertTrue(TagRandomDraws.randomDrawError("random-hunter", "x").isPresent());
        assertTrue(TagRandomDraws.randomDrawError("random-runner", "x").isPresent());
    }
}
