package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Format shape pins, ported from the engine's
 * {@code TagFormatTest.malformedShapeWarnsPlusNull}. The runtime
 * warn-plus-null half of that test stays engine-side.
 */
class TagFormatValidationTest {
    @Test
    void editTimeCheckMirrorsRuntimeShapes() {
        assertTrue(TagFormat.syntaxError("format", "\"{0} found {1}\",[Alex,gold]").isEmpty());
        assertTrue(TagFormat.syntaxError("format", "\"{0}\"").isPresent());
    }
}
