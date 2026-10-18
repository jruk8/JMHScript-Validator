package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Delay shape pins: seconds within 0..600 plus an optional id,
 * and exactly one id for the cancel tag.
 */
class TagDelayValidationTest {
    @Test
    void delayArity() {
        assertTrue(TagDelay.opError("delay", null).isPresent());
        assertTrue(TagDelay.opError("delay", "").isPresent());
        assertTrue(TagDelay.opError("delay", "1.2").isEmpty());
        assertTrue(TagDelay.opError("delay", "0").isEmpty());
        assertTrue(TagDelay.opError("delay", "600").isEmpty());
        assertTrue(TagDelay.opError("delay", "1.2,storm").isEmpty());
        assertTrue(TagDelay.opError("delay", "abc").isPresent());
        assertTrue(TagDelay.opError("delay", "-1").isPresent());
        assertTrue(TagDelay.opError("delay", "600.01").isPresent());
        assertTrue(TagDelay.opError("delay", "NaN").isPresent());
        assertTrue(TagDelay.opError("delay", "1,2,3").isPresent());
        assertTrue(TagDelay.opError("delay", "1.2,").isPresent());
        assertTrue(TagDelay.opError("delay", ",storm").isPresent());
        assertTrue(TagDelay.opError("delay", "?").isEmpty());
        assertTrue(TagDelay.opError("delay", "?,storm").isEmpty());
        assertTrue(TagDelay.opError("something-else", "1.2").isEmpty());
    }

    @Test
    void cancelArity() {
        assertTrue(TagDelay.opError("delaycancel", null).isPresent());
        assertTrue(TagDelay.opError("delaycancel", "").isPresent());
        assertTrue(TagDelay.opError("delaycancel", "storm").isEmpty());
        assertTrue(TagDelay.opError("delaycancel", "?").isEmpty());
        assertTrue(TagDelay.opError("delaycancel", "a,b").isPresent());
    }

    @Test
    void delayDispatches() {
        assertTrue(CommandSyntax.error("say <delay:1.2>").isEmpty());
        assertTrue(CommandSyntax.error("say <delay:1.2,storm>").isEmpty());
        assertTrue(CommandSyntax.error("say <delay:abc>").isPresent());
        assertTrue(CommandSyntax.error("say <delay:601>").isPresent());
        assertTrue(CommandSyntax.error("say <delaycancel:storm>").isEmpty());
        assertTrue(CommandSyntax.error("say <delaycancel:>").isPresent());
    }
}
