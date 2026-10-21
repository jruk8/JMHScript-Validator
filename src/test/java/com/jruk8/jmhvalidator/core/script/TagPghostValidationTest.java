package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ghost shape pins: enter takes player plus id plus loc, exit and
 * pisghost take one arg each, control takes id plus movement plus an
 * optional 0..1 smoothness.
 */
class TagPghostValidationTest {
    @Test
    void enterArity() {
        assertTrue(TagPghost.opError("pghost.enter", null).isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "").isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "Steve").isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone").isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone,[0, 64, 0, world]").isEmpty());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone,[0, 64, 0, world, 0, 90]")
                .isEmpty());
        assertTrue(TagPghost.opError("pghost.enter", ",drone,[0, 64, 0, world]").isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,,[0, 64, 0, world]").isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone,").isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone,nowhere").isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone,[0, 64, world]").isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone,[a, 64, 0, world]").isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone,[0, 64, 0, ]").isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "?,?,?").isEmpty());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone,[0, 64, 0, world],CAMERA")
                .isEmpty());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone,[0, 64, 0, world],camera")
                .isEmpty());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone,[0, 64, 0, world],WORLD")
                .isEmpty());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone,[0, 64, 0, world],UP").isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone,[0, 64, 0, world],").isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "Steve,drone,[0, 64, 0, world],CAMERA,x")
                .isPresent());
        assertTrue(TagPghost.opError("pghost.enter", "?,?,?,?").isEmpty());
        assertTrue(TagPghost.opError("something-else", "Steve,drone,[0, 64, 0, world]").isEmpty());
    }

    @Test
    void exitArity() {
        assertTrue(TagPghost.opError("pghost.exit", null).isPresent());
        assertTrue(TagPghost.opError("pghost.exit", "").isPresent());
        assertTrue(TagPghost.opError("pghost.exit", "drone").isEmpty());
        assertTrue(TagPghost.opError("pghost.exit", "?").isEmpty());
        assertTrue(TagPghost.opError("pghost.exit", "a,b").isPresent());
    }

    @Test
    void controlArity() {
        assertTrue(TagPghost.opError("pghost.control", null).isPresent());
        assertTrue(TagPghost.opError("pghost.control", "").isPresent());
        assertTrue(TagPghost.opError("pghost.control", "drone").isPresent());
        assertTrue(TagPghost.opError("pghost.control", "drone,[0, 1, 0]").isEmpty());
        assertTrue(TagPghost.opError("pghost.control", "drone,[0, 1, 0],0.3").isEmpty());
        assertTrue(TagPghost.opError("pghost.control", "drone,[0, 1, 0],0").isEmpty());
        assertTrue(TagPghost.opError("pghost.control", "drone,[0, 1, 0],1").isEmpty());
        assertTrue(TagPghost.opError("pghost.control", ",[0, 1, 0]").isPresent());
        assertTrue(TagPghost.opError("pghost.control", "drone,up").isPresent());
        assertTrue(TagPghost.opError("pghost.control", "drone,[0, 1]").isPresent());
        assertTrue(TagPghost.opError("pghost.control", "drone,[0, a, 0]").isPresent());
        assertTrue(TagPghost.opError("pghost.control", "drone,[0, 1, 0],2").isPresent());
        assertTrue(TagPghost.opError("pghost.control", "drone,[0, 1, 0],-0.1").isPresent());
        assertTrue(TagPghost.opError("pghost.control", "drone,[0, 1, 0],NaN").isPresent());
        assertTrue(TagPghost.opError("pghost.control", "drone,[0, 1, 0],smooth").isPresent());
        assertTrue(TagPghost.opError("pghost.control", "?,?,?").isEmpty());
    }

    @Test
    void isGhostArity() {
        assertTrue(TagPghost.opError("pisghost", null).isPresent());
        assertTrue(TagPghost.opError("pisghost", "").isPresent());
        assertTrue(TagPghost.opError("pisghost", "Steve").isEmpty());
        assertTrue(TagPghost.opError("pisghost", "?").isEmpty());
        assertTrue(TagPghost.opError("pisghost", "a,b").isPresent());
    }

    @Test
    void ghostDispatches() {
        assertTrue(CommandSyntax.error("say <pghost.enter:Steve,drone,[0, 64, 0, world]>")
                .isEmpty());
        assertTrue(CommandSyntax.error("say <pghost.enter:Steve>").isPresent());
        assertTrue(CommandSyntax.error("say <pghost.enter:Steve,drone,[0, 64, 0, world],CAMERA>")
                .isEmpty());
        assertTrue(CommandSyntax.error("say <pghost.enter:Steve,drone,[0, 64, 0, world],UP>")
                .isPresent());
        assertTrue(CommandSyntax.error("say <pghost.exit:drone>").isEmpty());
        assertTrue(CommandSyntax.error("say <pghost.exit:>").isPresent());
        assertTrue(CommandSyntax.error("say <pghost.control:drone,[0, 1, 0]>").isEmpty());
        assertTrue(CommandSyntax.error("say <pghost.control:drone,[0, 1, 0],9>").isPresent());
        assertTrue(CommandSyntax.error("say <pisghost:Steve>").isEmpty());
        assertTrue(CommandSyntax.error("say <pisghost:>").isPresent());
    }
}
