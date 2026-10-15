package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Located-sound shape pins: a 4- or 6-element point plus an id,
 * pitch, and volume, or a point plus an enabled-gate array.
 */
class TagSinksValidationTest {
    @Test
    void lsoundFlatShape() {
        assertTrue(TagSinks.locatedSoundError("lsound", null).isPresent());
        assertTrue(TagSinks.locatedSoundError("lsound", "").isPresent());
        assertTrue(TagSinks.locatedSoundError("lsound",
                "[10,64,0,world],block.glass.break").isEmpty());
        assertTrue(TagSinks.locatedSoundError("lsound",
                "[10,64,0,world],block.glass.break,1.2,0.7").isEmpty());
        assertTrue(TagSinks.locatedSoundError("lsound",
                "[10,64,0,world,90,180],block.glass.break").isEmpty());
        assertTrue(TagSinks.locatedSoundError("lsound", "?,block.glass.break").isEmpty());
        assertTrue(TagSinks.locatedSoundError("lsound",
                "[10,64,0,world]").isPresent());
        assertTrue(TagSinks.locatedSoundError("lsound",
                "nowhere,block.glass.break").isPresent());
        assertTrue(TagSinks.locatedSoundError("lsound",
                "[10,64],block.glass.break").isPresent());
        assertTrue(TagSinks.locatedSoundError("lsound",
                "[10,64,0,world],block.glass.break,1,2,3").isPresent());
    }

    @Test
    void lsoundArrayShape() {
        assertTrue(TagSinks.locatedSoundError("lsound",
                "[10,64,0,world],[true,block.glass.break,1.2,0.7]").isEmpty());
        assertTrue(TagSinks.locatedSoundError("lsound",
                "[10,64,0,world],[true]").isPresent());
        assertTrue(TagSinks.locatedSoundError("lsound",
                "nowhere,[true,block.glass.break]").isPresent());
    }

    @Test
    void lsoundDispatches() {
        assertTrue(CommandSyntax.error(
                "say <lsound:[10,64,0,world],block.glass.break> done").isEmpty());
        assertTrue(CommandSyntax.error("say <lsound:nowhere> done").isPresent());
    }
}
