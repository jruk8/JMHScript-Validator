package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Actionbar shape pins: a message plus optional stay and fadeout
 * seconds for set, no arguments for clear.
 */
class TagActionbarValidationTest {
    @Test
    void setShape() {
        assertTrue(TagSinks.actionbarError("actionbar.set", null).isPresent());
        assertTrue(TagSinks.actionbarError("actionbar.set", "").isPresent());
        assertTrue(TagSinks.actionbarError("actionbar.set", "  ").isPresent());
        assertTrue(TagSinks.actionbarError("actionbar.set", "Go!").isEmpty());
        assertTrue(TagSinks.actionbarError("actionbar.set", "Go!,3").isEmpty());
        assertTrue(TagSinks.actionbarError("actionbar.set", "Go!,3,0.5").isEmpty());
        assertTrue(TagSinks.actionbarError("actionbar.set", "\"Go, now!\",3").isEmpty());
        assertTrue(TagSinks.actionbarError("actionbar.set", "Go!,3,0.5,extra").isPresent());
        assertTrue(TagSinks.actionbarError("actionbar.set", "Go\",\"oops").isPresent());
    }

    @Test
    void clearShape() {
        assertTrue(TagSinks.actionbarClearError("actionbar.clear", null).isEmpty());
        assertTrue(TagSinks.actionbarClearError("actionbar.clear", "").isEmpty());
        assertTrue(TagSinks.actionbarClearError("actionbar.clear", "extra").isPresent());
    }

    @Test
    void dispatches() {
        assertTrue(CommandSyntax.error("say <actionbar.set:Go!,3,0.5> done").isEmpty());
        assertTrue(CommandSyntax.error("say <actionbar.set:> done").isPresent());
        assertTrue(CommandSyntax.error("say <actionbar.clear> done").isEmpty());
        assertTrue(CommandSyntax.error("say <actionbar.clear:extra> done").isPresent());
    }
}
