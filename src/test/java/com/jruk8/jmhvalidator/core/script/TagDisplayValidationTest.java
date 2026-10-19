package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Display shape pins: item and block take id plus loc plus payload,
 * head takes id plus loc plus optional texture, cancel takes one id,
 * teleport takes id plus loc.
 */
class TagDisplayValidationTest {
    @Test
    void itemArity() {
        assertTrue(TagDisplay.opError("display.item", null).isPresent());
        assertTrue(TagDisplay.opError("display.item", "").isPresent());
        assertTrue(TagDisplay.opError("display.item", "steve,[0, 64, 0, world]").isPresent());
        assertTrue(TagDisplay.opError("display.item", "steve,[0, 64, 0, world],[dirt, 1]")
                .isEmpty());
        assertTrue(TagDisplay.opError("display.item", ",[0, 64, 0, world],[dirt, 1]").isPresent());
        assertTrue(TagDisplay.opError("display.item", "steve,,[dirt, 1]").isPresent());
        assertTrue(TagDisplay.opError("display.item", "steve,nowhere,[dirt, 1]").isPresent());
        assertTrue(TagDisplay.opError("display.item", "steve,[0, 64, 0, world],").isPresent());
        assertTrue(TagDisplay.opError("display.item", "?,?,?").isEmpty());
        assertTrue(TagDisplay.opError("something-else", "steve,[0, 64, 0, world],[dirt, 1]")
                .isEmpty());
    }

    @Test
    void blockArity() {
        assertTrue(TagDisplay.opError("display.block", null).isPresent());
        assertTrue(TagDisplay.opError("display.block", "").isPresent());
        assertTrue(TagDisplay.opError("display.block", "steve,[0, 64, 0, world],stone").isEmpty());
        assertTrue(TagDisplay.opError("display.block", "steve,[0, 64, 0, world, 0, 90],stone")
                .isEmpty());
        assertTrue(TagDisplay.opError("display.block", "steve,[0, 64, 0, world],").isPresent());
        assertTrue(TagDisplay.opError("display.block", "steve,nowhere,stone").isPresent());
        assertTrue(TagDisplay.opError("display.block", "?,?,?").isEmpty());
    }

    @Test
    void headArity() {
        assertTrue(TagDisplay.opError("display.head", null).isPresent());
        assertTrue(TagDisplay.opError("display.head", "").isPresent());
        assertTrue(TagDisplay.opError("display.head", "steve,[0, 64, 0, world]").isEmpty());
        assertTrue(TagDisplay.opError("display.head", "steve,[0, 64, 0, world],red").isEmpty());
        assertTrue(TagDisplay.opError("display.head", "steve,[0, 64, 0, world],").isEmpty());
        assertTrue(TagDisplay.opError("display.head", "steve").isPresent());
        assertTrue(TagDisplay.opError("display.head", ",[0, 64, 0, world]").isPresent());
        assertTrue(TagDisplay.opError("display.head", "steve,nowhere").isPresent());
        assertTrue(TagDisplay.opError("display.head", "?,?,?").isEmpty());
    }

    @Test
    void cancelArity() {
        assertTrue(TagDisplay.opError("display.cancel", null).isPresent());
        assertTrue(TagDisplay.opError("display.cancel", "").isPresent());
        assertTrue(TagDisplay.opError("display.cancel", "steve").isEmpty());
        assertTrue(TagDisplay.opError("display.cancel", "?").isEmpty());
        assertTrue(TagDisplay.opError("display.cancel", "a,b").isPresent());
    }

    @Test
    void teleportArity() {
        assertTrue(TagDisplay.opError("display.teleport", null).isPresent());
        assertTrue(TagDisplay.opError("display.teleport", "").isPresent());
        assertTrue(TagDisplay.opError("display.teleport", "steve,[0, 64, 0, world]").isEmpty());
        assertTrue(TagDisplay.opError("display.teleport", "steve").isPresent());
        assertTrue(TagDisplay.opError("display.teleport", ",[0, 64, 0, world]").isPresent());
        assertTrue(TagDisplay.opError("display.teleport", "steve,nowhere").isPresent());
        assertTrue(TagDisplay.opError("display.teleport", "?,?").isEmpty());
    }

    @Test
    void displayDispatches() {
        assertTrue(CommandSyntax.error("say <display.item:steve,[0, 64, 0, world],[dirt, 1]>")
                .isEmpty());
        assertTrue(CommandSyntax.error("say <display.item:steve>").isPresent());
        assertTrue(CommandSyntax.error("say <display.block:steve,[0, 64, 0, world],stone>")
                .isEmpty());
        assertTrue(CommandSyntax.error("say <display.head:steve,[0, 64, 0, world]>").isEmpty());
        assertTrue(CommandSyntax.error("say <display.head:steve>").isPresent());
        assertTrue(CommandSyntax.error("say <display.cancel:steve>").isEmpty());
        assertTrue(CommandSyntax.error("say <display.cancel:>").isPresent());
        assertTrue(CommandSyntax.error("say <display.teleport:steve,[0, 64, 0, world]>")
                .isEmpty());
        assertTrue(CommandSyntax.error("say <display.teleport:steve,nowhere>").isPresent());
    }
}
