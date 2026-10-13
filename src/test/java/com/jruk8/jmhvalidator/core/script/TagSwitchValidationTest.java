package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Switch arity pins, ported from the engine's
 * {@code TagSwitchTest.validationRejectsShortSwitch}. The rest
 * of that class exercises runtime dispatch and stays engine-side.
 */
class TagSwitchValidationTest {
    @Test
    void validationRejectsShortSwitch() {
        assertTrue(CommandSyntax.error("say <switch:a>").isPresent());
        assertTrue(CommandSyntax.error("say <switch>").isPresent());
        assertTrue(CommandSyntax.error("say <switch:a,b,c>").isEmpty());
        assertTrue(CommandSyntax.error("say <switch:a,\"b,c\",d>").isEmpty());
    }
}
