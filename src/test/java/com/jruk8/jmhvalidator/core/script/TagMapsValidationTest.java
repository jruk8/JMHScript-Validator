package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TagMaps arity pins, ported from the engine's
 * {@code TagMapsTest.opErrorMirrorsRuntimeShapes}. The rest
 * of that class exercises runtime dispatch and stays engine-side.
 */
class TagMapsValidationTest {
    @Test
    void opErrorMirrorsRuntimeShapes() {
        assertTrue(TagMaps.opError("map.get", "[[a, 1]],a").isEmpty());
        assertTrue(TagMaps.opError("m.add", "[[a, 1]],b,2").isEmpty());
        assertTrue(TagMaps.opError("map.keys", "[[a, 1]]").isEmpty());
        assertEquals("Tag <m.get> needs 2 args like <m.get:[[k, v]],k>.",
                TagMaps.opError("m.get", "[[a, 1]]").orElseThrow());
        assertEquals("Tag <map.add> needs 3 args like <map.add:[[k, v]],k,x>.",
                TagMaps.opError("map.add", null).orElseThrow());
        assertEquals("Tag <map.keys> mixes quotes.",
                TagMaps.opError("map.keys", "[[\"a\", 1]]").orElseThrow());
    }
}
