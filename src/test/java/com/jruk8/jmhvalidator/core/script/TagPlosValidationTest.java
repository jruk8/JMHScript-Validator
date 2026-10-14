package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TagPlos arity pins, ported from the engine's
 * {@code TagPlosTest.editTimeValidationMirrorsRuntime}. The rest
 * of that class exercises runtime dispatch and stays engine-side.
 */
class TagPlosValidationTest {
    @Test
    void editTimeValidationMirrorsRuntime() {
        assertTrue(TagPlos.opError("plos", null).isPresent());
        assertTrue(TagPlos.opError("plos", "Steve").isPresent());
        assertTrue(TagPlos.opError("plos", "Steve,Alex,180,90,extra").isPresent());
        assertTrue(TagPlos.opError("plos", "Steve,Alex,-5").isPresent());
        assertTrue(TagPlos.opError("plos", "Steve,Alex,180,200").isPresent());
        assertTrue(TagPlos.opError("plos", "Steve,Alex").isEmpty());
        assertTrue(TagPlos.opError("plos", "Steve,Alex,180,90").isEmpty());
        assertTrue(TagPlos.opError("plos", "Steve,Alex,<args:0>,90").isEmpty());
    }
}
