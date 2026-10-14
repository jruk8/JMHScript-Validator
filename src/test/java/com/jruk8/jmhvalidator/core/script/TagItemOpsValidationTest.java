package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TagItemOps shape pins: give and itemtag take two args, dropitem
 * takes two or three, and literal tag-length failures fire on the
 * item payloads. Dynamic payloads abstain for runtime.
 */
class TagItemOpsValidationTest {
    @Test
    void giveArity() {
        assertTrue(TagItemOps.opError("give", null).isPresent());
        assertTrue(TagItemOps.opError("give", "Steve").isPresent());
        assertTrue(TagItemOps.opError("give", "Steve,[dirt, 1],extra").isPresent());
        assertTrue(TagItemOps.opError("give", "Steve,[dirt, 1]").isEmpty());
        assertTrue(TagItemOps.opError("give", "Steve,[[dirt, 1], [stone, 2]]").isEmpty());
        assertEquals("<give:player,item>", TagItemOps.example("give"));
    }

    @Test
    void dropitemArity() {
        assertTrue(TagItemOps.opError("dropitem", null).isPresent());
        assertTrue(TagItemOps.opError("dropitem", "[0,64,0,world,0,0]").isPresent());
        assertTrue(TagItemOps.opError("dropitem", "a,b,c,d").isPresent());
        assertTrue(TagItemOps.opError("dropitem", "[0,64,0,world,0,0],[dirt, 1]").isEmpty());
        assertTrue(TagItemOps.opError("dropitem", "[0,64,0,world,0,0],[dirt, 1],0.5")
                .isEmpty());
        assertEquals("<dropitem:location,item,cooldown>", TagItemOps.example("dropitem"));
    }

    @Test
    void itemtagArity() {
        assertTrue(TagItemOps.opError("itemtag", null).isPresent());
        assertTrue(TagItemOps.opError("itemtag", "Steve").isPresent());
        assertTrue(TagItemOps.opError("itemtag", "Steve,mainhand,extra").isPresent());
        assertTrue(TagItemOps.opError("itemtag", "Steve,mainhand").isEmpty());
        assertEquals("<itemtag:player,slot>", TagItemOps.example("itemtag"));
    }

    @Test
    void literalTagLengthFailsOnPayloads() {
        String overlong = "[tag, " + "x".repeat(101) + "]";
        assertEquals("Tag <give> tag value allows 100 chars at most.",
                TagItemOps.opError("give", "Steve,[dirt, 1, " + overlong + "]").orElseThrow());
        assertEquals("Tag <dropitem> tag value allows 100 chars at most.",
                TagItemOps.opError("dropitem", "[0,64,0,world,0,0],[dirt, 1, " + overlong
                        + "]").orElseThrow());
        assertTrue(TagItemOps.opError("give", "Steve,[dirt, 1, [tag, <setting:x>]]")
                .isEmpty());
    }
}
