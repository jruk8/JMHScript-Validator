package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runblock arity pins: one behavior id plus optional args,
 * blank and blank-quoted ids refused, mixed quotes refused.
 */
class TagRunBlockValidationTest {
    @Test
    void opErrorMirrorsRuntimeShapes() {
        assertTrue(TagRunBlock.opError("runblock", "side").isEmpty());
        assertTrue(TagRunBlock.opError("runblock", "side,1,2").isEmpty());
        assertTrue(TagRunBlock.opError("runblock", "\"side\"").isEmpty());
        assertEquals("Tag <runblock> needs a behavior id like <runblock:broadcast>.",
                TagRunBlock.opError("runblock", null).orElseThrow());
        assertEquals("Tag <runblock> needs a behavior id like <runblock:broadcast>.",
                TagRunBlock.opError("runblock", "   ").orElseThrow());
        assertEquals("Tag <runblock> needs a behavior id like <runblock:broadcast>.",
                TagRunBlock.opError("runblock", "\" \"").orElseThrow());
        assertEquals("Tag <runblock> needs a behavior id like <runblock:broadcast>.",
                TagRunBlock.opError("runblock", ",foo").orElseThrow());
        assertEquals("Tag <runblock> mixes quotes.",
                TagRunBlock.opError("runblock", "'a\"b").orElseThrow());
    }
}
