package com.jruk8.jmhvalidator.core.script;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TagWeighted arity pins, ported from the engine's
 * {@code TagWeightedTest.opErrorMirrorsRuntimeShapes}. The rest
 * of that class exercises runtime dispatch and stays engine-side.
 */
class TagWeightedValidationTest {
    void opErrorMirrorsRuntimeShapes() {
        assertTrue(TagWeighted.opError("weighted-pick", "[[10.0, dirt]]").isEmpty());
        assertTrue(TagWeighted.opError("weighted-picks", "[[10.0, dirt]],2").isEmpty());
        assertTrue(TagWeighted.opError("weighted-picks", "[[10.0, dirt]],2,true").isEmpty());
        assertEquals("Tag <weighted-pick> needs 1 arg like "
                + "<weighted-pick:[[10.0, dirt], [2.0, diamond]]>.",
                TagWeighted.opError("weighted-pick", null).orElseThrow());
        assertEquals("Tag <weighted-picks> needs 2 to 3 args like "
                + "<weighted-picks:[[10.0, dirt], [2.0, diamond]],2,true>.",
                TagWeighted.opError("weighted-picks", "[[10.0, dirt]]").orElseThrow());
        assertEquals("Tag <weighted-pick> mixes quotes.",
                TagWeighted.opError("weighted-pick", "[[10.0, \"dirt\"]]").orElseThrow());
    }
}
