package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TagVectors magnitude pins: mag and mag2d are unary ops with
 * per-head examples, beside the existing arities.
 */
class TagVectorsValidationTest {
    @Test
    void magnitudeAritiesAndExamples() {
        assertEquals(1, TagVectors.arity("vec.mag"));
        assertEquals(1, TagVectors.arity("vec.mag2d"));
        assertEquals(1, TagVectors.arity("vec.normalize"));
        assertEquals(2, TagVectors.arity("vec.dist"));
        assertEquals(3, TagVectors.arity("loc.shift"));
        assertEquals("<vec.mag:vec>", TagVectors.example("vec.mag"));
        assertEquals("<vec.mag2d:vec>", TagVectors.example("vec.mag2d"));
    }

    @Test
    void magnitudeShapes() {
        assertTrue(TagVectors.opError("vec.mag", null).isPresent());
        assertTrue(TagVectors.opError("vec.mag", "[1, 2, 3],[4, 5, 6]").isPresent());
        assertTrue(TagVectors.opError("vec.mag", "[1, 2, 3]").isEmpty());
        assertTrue(TagVectors.opError("vec.mag2d", "[1, 2]").isEmpty());
    }
}
