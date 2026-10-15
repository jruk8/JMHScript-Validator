package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fill shape pins: five args, or six with a trailing hollow.
 */
class TagFillValidationTest {
    @Test
    void fillArity() {
        assertTrue(TagFill.fillError("fill", null).isPresent());
        assertTrue(TagFill.fillError("fill", "").isPresent());
        assertTrue(TagFill.fillError("fill",
                "world,[0,60,0],[2,62,2],lava,set").isEmpty());
        assertTrue(TagFill.fillError("fill",
                "world,[0,60,0],[2,62,2],lava,set,hollow").isEmpty());
        assertTrue(TagFill.fillError("fill",
                "world,[0,60,0],[2,62,2],lava,set,HOLLOW").isEmpty());
        assertTrue(TagFill.fillError("fill",
                "world,[0,60,0],[2,62,2],lava,set,solid").isPresent());
        assertTrue(TagFill.fillError("fill",
                "world,[0,60,0],[2,62,2],lava").isPresent());
        assertTrue(TagFill.fillError("fill",
                "world,[0,60,0],[2,62,2],lava,set,hollow,extra").isPresent());
    }

    @Test
    void fillDispatches() {
        assertTrue(CommandSyntax.error(
                "say <fill:world,[0,60,0],[2,62,2],lava,set,hollow>").isEmpty());
        assertTrue(CommandSyntax.error(
                "say <fill:world,[0,60,0],[2,62,2],lava,set,solid>").isPresent());
    }
}
