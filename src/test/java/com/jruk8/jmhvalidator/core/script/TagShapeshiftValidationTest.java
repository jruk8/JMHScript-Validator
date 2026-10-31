package com.jruk8.jmhvalidator.core.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Shapeshift batch pins: free exp levels, living-entity reads, and
 * the paid prompt plus exit pair. Arity first, then quotes, then
 * the per-head shape, mirroring the runtime warns.
 */
class TagShapeshiftValidationTest {

    @Test
    void expArity() {
        assertTrue(TagPlayers.opError("pexp.get", null).isPresent());
        assertTrue(TagPlayers.opError("pexp.get", "").isPresent());
        assertTrue(TagPlayers.opError("pexp.get", "Steve").isEmpty());
        assertTrue(TagPlayers.opError("pexp.get", "?").isEmpty());
        assertTrue(TagPlayers.opError("pexp.get", "Steve,5").isPresent());
        assertTrue(TagPlayers.opError("pexp.add", "Steve,5").isEmpty());
        assertTrue(TagPlayers.opError("pexp.add", "Steve").isPresent());
        assertTrue(TagPlayers.opError("pexp.add", "Steve,5,6").isPresent());
        assertTrue(TagPlayers.opError("pexp.remove", "Steve,5").isEmpty());
        assertTrue(TagPlayers.opError("pexp.remove", "Steve").isPresent());
        assertTrue(TagPlayers.opError("pexp.add", "Steve,\"unclosed").isPresent());
        assertEquals("Tag <pexp.get> needs 1 arg like <pexp.get:Steve>.",
                TagPlayers.opError("pexp.get", "Steve,5").orElseThrow());
        assertEquals("Tag <pexp.add> needs 2 args like <pexp.add:Steve,5>.",
                TagPlayers.opError("pexp.add", "Steve").orElseThrow());
        assertEquals("Tag <pexp.remove> needs 2 args like <pexp.remove:Steve,5>.",
                TagPlayers.opError("pexp.remove", "Steve").orElseThrow());
    }

    @Test
    void expDispatches() {
        assertTrue(CommandSyntax.error("say <pexp.get:Steve>").isEmpty());
        assertTrue(CommandSyntax.error("say <pexp.add:Steve,5>").isEmpty());
        assertTrue(CommandSyntax.error("say <pexp.remove:Steve,5>").isEmpty());
        assertTrue(CommandSyntax.error("say <pexp.get:>").isPresent());
        assertTrue(CommandSyntax.error("say <pexp.get:Steve,5>").isPresent());
        assertTrue(CommandSyntax.error("say <pexp.add:Steve>").isPresent());
        assertTrue(CommandSyntax.error("say <pexp.remove:Steve>").isPresent());
    }

    @Test
    void livingEntityArity() {
        assertTrue(TagEntities.opError("islivingentity", null).isPresent());
        assertTrue(TagEntities.opError("islivingentity", "").isPresent());
        assertTrue(TagEntities.opError("islivingentity", "ZOMBIE").isEmpty());
        assertTrue(TagEntities.opError("islivingentity", "minecraft:cow").isEmpty());
        assertTrue(TagEntities.opError("islivingentity", "DRAGONLORD").isEmpty());
        assertTrue(TagEntities.opError("islivingentity", "ZOMBIE,SKELETON").isPresent());
        assertTrue(TagEntities.opError("islivingentity", "\"unclosed").isPresent());
        assertEquals("Tag <islivingentity> needs an entity type like <islivingentity:ZOMBIE>.",
                TagEntities.opError("islivingentity", "ZOMBIE,SKELETON").orElseThrow());
        assertTrue(TagEntities.opError("ismobhostile", "ZOMBIE").isEmpty());
        assertEquals("Tag <ismobhostile> needs an entity type like <ismobhostile:ZOMBIE>.",
                TagEntities.opError("ismobhostile", "ZOMBIE,SKELETON").orElseThrow());
    }

    @Test
    void livingEntityDispatches() {
        assertTrue(CommandSyntax.error("say <islivingentity:ZOMBIE>").isEmpty());
        assertTrue(CommandSyntax.error("say <islivingentity:>").isPresent());
        assertTrue(CommandSyntax.error("say <islivingentity:ZOMBIE,SKELETON>").isPresent());
        assertTrue(CommandSyntax.error("say <ismobhostile:ZOMBIE>").isEmpty());
    }

    @Test
    void mobArity() {
        assertTrue(TagShapeshift.opError("pshapeshift.mob", null).isPresent());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "").isPresent());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve").isEmpty());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve,true").isEmpty());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve,FALSE").isEmpty());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve,\"true\"").isEmpty());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve,maybe").isPresent());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve,").isPresent());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve,true,5").isEmpty());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve,true,5,3").isEmpty());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve,false,?,?").isEmpty());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve,true,fast").isPresent());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve,true,5,slow").isPresent());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve,true,5,3,1").isPresent());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", ",").isPresent());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve,\"unclosed").isPresent());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "?,true").isEmpty());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "Steve,?").isEmpty());
        assertTrue(TagShapeshift.opError("pshapeshift.mob", "?,?").isEmpty());
        assertEquals("Tag <pshapeshift.mob> needs 1 to 4 args like <pshapeshift.mob:Steve,true,5,3>.",
                TagShapeshift.opError("pshapeshift.mob", "Steve,true,5,3,1").orElseThrow());
        assertEquals("Tag <pshapeshift.mob> needs a player like <pshapeshift.mob:Steve>.",
                TagShapeshift.opError("pshapeshift.mob", ",").orElseThrow());
        assertEquals("Tag <pshapeshift.mob> flag \"maybe\" must be true or false.",
                TagShapeshift.opError("pshapeshift.mob", "Steve,maybe").orElseThrow());
        assertTrue(TagShapeshift.opError("something-else", "Steve").isEmpty());
    }

    @Test
    void exitArity() {
        assertTrue(TagShapeshift.opError("pshapeshift.exit", null).isPresent());
        assertTrue(TagShapeshift.opError("pshapeshift.exit", "").isPresent());
        assertTrue(TagShapeshift.opError("pshapeshift.exit", "Steve").isEmpty());
        assertTrue(TagShapeshift.opError("pshapeshift.exit", "?").isEmpty());
        assertTrue(TagShapeshift.opError("pshapeshift.exit", "Steve,Alex").isPresent());
        assertTrue(TagShapeshift.opError("pshapeshift.exit", "\"unclosed").isPresent());
        assertEquals("Tag <pshapeshift.exit> needs 1 arg like <pshapeshift.exit:Steve>.",
                TagShapeshift.opError("pshapeshift.exit", "Steve,Alex").orElseThrow());
        assertEquals("Tag <pshapeshift.exit> needs a player like <pshapeshift.exit:Steve>.",
                TagShapeshift.opError("pshapeshift.exit", "").orElseThrow());
    }

    @Test
    void shapeshiftDispatches() {
        assertTrue(CommandSyntax.error("say <pshapeshift.mob:Steve>").isEmpty());
        assertTrue(CommandSyntax.error("say <pshapeshift.mob:Steve,true>").isEmpty());
        assertTrue(CommandSyntax.error("say <pshapeshift.mob:Steve,true,5,3>").isEmpty());
        assertTrue(CommandSyntax.error("say <pshapeshift.mob:Steve,maybe>").isPresent());
        assertTrue(CommandSyntax.error("say <pshapeshift.mob:Steve,true,fast>").isPresent());
        assertTrue(CommandSyntax.error("say <pshapeshift.mob:>").isPresent());
        assertTrue(CommandSyntax.error("say <pshapeshift.exit:Steve>").isEmpty());
        assertTrue(CommandSyntax.error("say <pshapeshift.exit:>").isPresent());
        assertTrue(CommandSyntax.error("say <pshapeshift.exit:Steve,Alex>").isPresent());
    }
}
