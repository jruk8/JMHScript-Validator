package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paid-arg pins mirroring the engine load scan: Bukkit stat
 * keys, JMH tag/keep pairs, and static schematic targets.
 */
class TagPaidArgsTest {
    @Test
    void bukkitStatKeysArePaid() {
        assertTrue(TagPaidArgs.hasPaidArgs("pstat", "Steve,bukkit.mine_block,stone"));
        assertTrue(TagPaidArgs.hasPaidArgs("pstat", "Steve,b.animals_bred"));
        assertTrue(TagPaidArgs.hasPaidArgs("pstat", "Steve,animals_bred"));
        assertTrue(TagPaidArgs.hasPaidArgs("rstat", "HUNTER,mine_block,stone"));
    }

    @Test
    void matchUnknownDynamicAndGlobalKeysPass() {
        assertFalse(TagPaidArgs.hasPaidArgs("pstat", "Steve,health"));
        assertFalse(TagPaidArgs.hasPaidArgs("pstat", "Steve"));
        assertFalse(TagPaidArgs.hasPaidArgs("pstat", "Steve,BOGUSKEY"));
        assertFalse(TagPaidArgs.hasPaidArgs("pstat", "Steve,?"));
        assertFalse(TagPaidArgs.hasPaidArgs("gstat", "duration"));
        assertFalse(TagPaidArgs.hasPaidArgs("gstat", "jumps"));
        assertFalse(TagPaidArgs.hasPaidArgs("pstat", null));
    }

    @Test
    void itemAttrsPaidInsideItemHeads() {
        assertTrue(TagPaidArgs.hasPaidArgs("give", "Steve,[dirt,1,[tag,foo]]"));
        assertTrue(TagPaidArgs.hasPaidArgs("give", "Steve,[dirt,1,[keep,true]]"));
        assertTrue(TagPaidArgs.hasPaidArgs("dropitem",
                "[0,64,0,world],[dirt,1,[keep,true]]"));
        assertTrue(TagPaidArgs.hasPaidArgs("rgive", "HUNTER,[dirt,1,['tag',foo]]"));
        assertTrue(TagPaidArgs.hasPaidArgs("intercept", "[dirt,1,[tag,foo]]"));
    }

    @Test
    void itemAttrsPassOutsideItemHeadsOrWithoutEffect() {
        assertFalse(TagPaidArgs.hasPaidArgs("gmessage", "all,[tag,x]"));
        assertFalse(TagPaidArgs.hasPaidArgs("give", "Steve,[dirt,1,[tag,]]"));
        assertFalse(TagPaidArgs.hasPaidArgs("give", "Steve,[dirt,1,[keep,false]]"));
        assertFalse(TagPaidArgs.hasPaidArgs("give", "Steve,[dirt,1,[keep,yes]]"));
        assertFalse(TagPaidArgs.hasPaidArgs("give", "Steve,[dirt,1,[keep,]]"));
        assertFalse(TagPaidArgs.hasPaidArgs("give", "Steve,[dirt,1,[keep,?]]"));
        assertFalse(TagPaidArgs.hasPaidArgs("give", "Steve,[dirt,1,[name,Bob]]"));
        assertFalse(TagPaidArgs.hasPaidArgs("run", "say hi"));
    }

    @Test
    void staticSchematicTargetsArePaid() {
        assertTrue(TagPaidArgs.hasPaidArgs("place", "1 65 2,schematic:hut"));
        assertTrue(TagPaidArgs.hasPaidArgs("place", "1,65,2,Schematic:hut"));
        assertTrue(TagPaidArgs.hasPaidArgs("place", "?,schematic:hut"));
    }

    @Test
    void blockStructureAndDynamicSchematicTargetsPass() {
        assertFalse(TagPaidArgs.hasPaidArgs("place", "1 65 2,stone"));
        assertFalse(TagPaidArgs.hasPaidArgs("place", "1 65 2,structure:hut"));
        assertFalse(TagPaidArgs.hasPaidArgs("place", "1 65 2,schematic:"));
        assertFalse(TagPaidArgs.hasPaidArgs("place", "1 65 2,schematic:?"));
        assertFalse(TagPaidArgs.hasPaidArgs("place", "stone"));
    }
}
