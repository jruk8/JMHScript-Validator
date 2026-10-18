package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Block tag shape pins: strict loc lists plus a material for
 * setblock, one loc list for getblock.
 */
class TagBlocksValidationTest {
    @Test
    void setblockArity() {
        assertTrue(TagBlocks.blocksError("setblock", null).isPresent());
        assertTrue(TagBlocks.blocksError("setblock", "").isPresent());
        assertTrue(TagBlocks.blocksError("setblock", "[0, 64, 0, world],stone").isEmpty());
        assertTrue(TagBlocks.blocksError("setblock", "[0, 64, 0, world, 0, 0],stone").isEmpty());
        assertTrue(TagBlocks.blocksError("setblock", "[0, 64, 0, world]").isPresent());
        assertTrue(
                TagBlocks.blocksError("setblock", "[0, 64, 0, world],stone,extra").isPresent());
    }

    @Test
    void setblockShapes() {
        assertTrue(TagBlocks.blocksError("setblock", "0 64 0,stone").isPresent());
        assertTrue(TagBlocks.blocksError("setblock", "[0, 64, 0],stone").isPresent());
        assertTrue(TagBlocks.blocksError("setblock", "[0, 64, 0, world],").isPresent());
        assertTrue(TagBlocks.blocksError("setblock", "[0, 64, 0, world],'stone").isPresent());
        assertTrue(TagBlocks.blocksError("setblock", "?,stone").isEmpty());
    }

    @Test
    void getblockArity() {
        assertTrue(TagBlocks.blocksError("getblock", null).isPresent());
        assertTrue(TagBlocks.blocksError("getblock", "").isPresent());
        assertTrue(TagBlocks.blocksError("getblock", "[0, 64, 0, world]").isEmpty());
        assertTrue(TagBlocks.blocksError("getblock", "[0, 64, 0, world, 0, 0]").isEmpty());
        assertTrue(TagBlocks.blocksError("getblock", "[0, 64, 0, world],stone").isPresent());
    }

    @Test
    void getblockShapes() {
        assertTrue(TagBlocks.blocksError("getblock", "0 64 0").isPresent());
        assertTrue(TagBlocks.blocksError("getblock", "[0, 64, 0]").isPresent());
        assertTrue(TagBlocks.blocksError("getblock", "?").isEmpty());
    }

    @Test
    void issolidArity() {
        assertTrue(TagBlocks.blocksError("issolid", null).isPresent());
        assertTrue(TagBlocks.blocksError("issolid", "").isPresent());
        assertTrue(TagBlocks.blocksError("issolid", "stone").isEmpty());
        assertTrue(TagBlocks.blocksError("issolid", "STONE").isEmpty());
        assertTrue(TagBlocks.blocksError("issolid", "minecraft:stone").isEmpty());
        assertTrue(TagBlocks.blocksError("issolid", "stone,extra").isPresent());
    }

    @Test
    void issolidShapes() {
        assertTrue(TagBlocks.blocksError("issolid", "   ").isPresent());
        assertTrue(TagBlocks.blocksError("issolid", "'stone").isPresent());
        assertTrue(TagBlocks.blocksError("issolid", "?").isEmpty());
    }
}
