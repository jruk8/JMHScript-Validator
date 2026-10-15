package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Summon shape pins: two or three args, a non-blank loc and mob,
 * balanced appended summon data, and bare comma triples.
 */
class TagSummonValidationTest {
    @Test
    void summonArity() {
        assertTrue(TagSummon.opError("summon", null).isPresent());
        assertTrue(TagSummon.opError("summon", "0 100 0").isPresent());
        assertTrue(TagSummon.opError("summon", "a,b,c,d").isPresent());
        assertTrue(TagSummon.opError("summon", "0 100 0,creeper").isEmpty());
        assertTrue(TagSummon.opError("summon", "[0,100,0,world,0,0],creeper").isEmpty());
        assertTrue(TagSummon.opError("summon", "~ ~1 ~,<random-mob>").isEmpty());
        assertTrue(TagSummon.opError("summon", "0 100 0,creeper,Glowing:1b").isEmpty());
    }

    @Test
    void summonMobShape() {
        assertTrue(TagSummon.opError("summon", ",creeper").isPresent());
        assertTrue(TagSummon.opError("summon", "0 100 0,").isPresent());
        assertTrue(TagSummon.opError("summon", "0 100 0,[[name,X]]").isPresent());
        assertTrue(TagSummon.opError("summon", "0 100 0,creeper[[name,X]").isPresent());
        assertTrue(TagSummon.opError("summon",
                "0 100 0,creeper[[name,Boom],[glow,false]]").isEmpty());
    }

    @Test
    void summonBareCommaTriple() {
        assertTrue(TagSummon.opError("summon", "0,100,0,creeper").isEmpty());
        assertTrue(TagSummon.opError("summon", "~,~1,~,creeper").isEmpty());
        assertTrue(TagSummon.opError("summon", "0,100,0,creeper,Glowing:1b").isEmpty());
        assertTrue(TagSummon.opError("summon", "0,100,creeper").isEmpty());
        assertTrue(TagSummon.opError("summon", "0,100,creeper,zombie").isPresent());
    }
}
