package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TagRaycast arity pins, ported from the engine's
 * {@code TagRaycastTest.editTimeValidationMirrorsRuntime}. The rest
 * of that class exercises runtime dispatch and stays engine-side.
 */
class TagRaycastValidationTest {
    @Test
    void editTimeValidationMirrorsRuntime() {
        assertTrue(TagRaycast.opError("raycast", null).isPresent());
        assertTrue(TagRaycast.opError("raycast", "[0,64,0,world,0,0],[1,0,0]").isPresent());
        assertTrue(TagRaycast.opError("raycast", "[0,64,0],[1,0,0],10").isPresent());
        assertTrue(TagRaycast.opError("raycast",
                "[0,64,0,world,0,0],[1,0,0],10,stone").isPresent());
        assertTrue(TagRaycast.opError("raycast", "[0,64,0,world,0,0],[1,0,0],10").isEmpty());
        assertTrue(TagRaycast.opError("raycast",
                "[0,64,0,world,0,0],[1,0,0],10,block+liquid").isEmpty());
        assertTrue(TagRaycast.opError("raycast", "<ploc:Steve>,[1,0,0],10").isEmpty());
    }

    @Test
    void raycastMessagesStayByteIdentical() {
        assertEquals("Tag <raycast> needs 3 or 4 args like "
                        + "<raycast:point,dir,reach,mask>.",
                TagRaycast.opError("raycast", "[0,64,0,world,0,0],[1,0,0]").orElseThrow());
        assertEquals("<raycast:point,dir,reach,mask>", TagRaycast.example());
        assertEquals("<raycast:point,dir,reach,mask>", TagRaycast.example("raycast"));
    }

    @Test
    void raycastextSharesShapeWithOptionalIgnore() {
        assertTrue(TagRaycast.opError("raycastext", null).isPresent());
        assertTrue(TagRaycast.opError("raycastext", "[0,64,0,world,0,0],[1,0,0]").isPresent());
        assertTrue(TagRaycast.opError("raycastext",
                "[0,64,0,world,0,0],[1,0,0],10,player,Steve,1,extra").isPresent());
        assertTrue(TagRaycast.opError("raycastext", "[0,64,0,world,0,0],[1,0,0],10").isEmpty());
        assertTrue(TagRaycast.opError("raycastext",
                "[0,64,0,world,0,0],[1,0,0],10,block+player,Steve").isEmpty());
        assertTrue(TagRaycast.opError("raycastext",
                "[0,64,0,world,0,0],[1,0,0],10,block+player,Steve,1").isEmpty());
        assertTrue(TagRaycast.opError("raycastext",
                "[0,64,0,world,0,0],[1,0,0],10,stone").isPresent());
        assertTrue(TagRaycast.opError("raycast",
                "[0,64,0,world,0,0],[1,0,0],10,player,Steve").isPresent());
        assertEquals("Tag <raycastext> needs 3, 4, 5, or 6 args like "
                        + "<raycastext:point,dir,reach,mask,ignore,spread>.",
                TagRaycast.opError("raycastext", "[0,64,0,world,0,0],[1,0,0]").orElseThrow());
        assertEquals("<raycastext:point,dir,reach,mask,ignore,spread>",
                TagRaycast.example("raycastext"));
    }
}
