package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TagSets arity pins, ported from the engine's
 * {@code TagSetsTest.opErrorMirrorsRuntimeShapes}. The rest
 * of that class exercises runtime dispatch and stays engine-side.
 */
class TagSetsValidationTest {
    @Test
    void opErrorMirrorsRuntimeShapes() {
        assertTrue(TagSets.opError("set.add", "[a, b],c").isEmpty());
        assertTrue(TagSets.opError("set.tolist", "[a, b]").isEmpty());
        assertEquals("Tag <set.add> needs 2 args like <set.add:[a, b],a>.",
                TagSets.opError("set.add", "[a, b]").orElseThrow());
        assertEquals("Tag <set.size> needs 1 args like <set.size:[a, b]>.",
                TagSets.opError("set.size", null).orElseThrow());
        assertEquals("Tag <set.contains> mixes quotes.",
                TagSets.opError("set.contains", "[\"a\"],a").orElseThrow());
    }
}
