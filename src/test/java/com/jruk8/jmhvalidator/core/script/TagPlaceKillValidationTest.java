package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Place and kill shape pins: loc plus target, bare comma
 * triples, structures, and the kill force flag.
 */
class TagPlaceKillValidationTest {
    @Test
    void placeArity() {
        assertTrue(TagPlaceKill.opError("place", null).isPresent());
        assertTrue(TagPlaceKill.opError("place", "0 64 0").isPresent());
        assertTrue(TagPlaceKill.opError("place", "0 64 0,stone").isEmpty());
        assertTrue(TagPlaceKill.opError("place", "0,64,0,stone").isEmpty());
        assertTrue(TagPlaceKill.opError("place", "[0,64,0],stone").isEmpty());
        assertTrue(TagPlaceKill.opError("place", "~ ~ ~,structure:xyz").isEmpty());
    }

    @Test
    void placeShapes() {
        assertTrue(TagPlaceKill.opError("place", ",stone").isPresent());
        assertTrue(TagPlaceKill.opError("place", "0 64 0,").isPresent());
        assertTrue(TagPlaceKill.opError("place", "0,64,stone").isPresent());
        assertTrue(TagPlaceKill.opError("place", "0,64,stone,dirt").isPresent());
        assertTrue(TagPlaceKill.opError("place", "a,b,stone").isPresent());
    }

    @Test
    void killArity() {
        assertTrue(TagPlaceKill.opError("kill", null).isPresent());
        assertTrue(TagPlaceKill.opError("kill", "").isPresent());
        assertTrue(TagPlaceKill.opError("kill", "Steve").isEmpty());
        assertTrue(TagPlaceKill.opError("kill", "Steve,Alex").isEmpty());
        assertTrue(TagPlaceKill.opError("kill", "Steve,Alex,true").isEmpty());
        assertTrue(TagPlaceKill.opError("kill", "Steve,Alex,false").isEmpty());
        assertTrue(TagPlaceKill.opError("kill", "Steve,Alex,true,extra").isPresent());
    }

    @Test
    void killShapes() {
        assertTrue(TagPlaceKill.opError("kill", ",Alex").isPresent());
        assertTrue(TagPlaceKill.opError("kill", "Steve,").isPresent());
        assertTrue(TagPlaceKill.opError("kill", "Steve,Alex,maybe").isPresent());
        assertTrue(TagPlaceKill.opError("kill", "Steve,Alex,?").isEmpty());
    }
}
