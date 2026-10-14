package com.jruk8.jmhvalidator.core;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bundled-shadow pins: pristine bytes pass, customs and gaps warn. */
class BundledShadowTest {

    @Test
    void pristineBytesPass() {
        byte[] bytes = text("behavior:\n  main: {}\n");

        assertEquals(Optional.empty(),
                BundledShadow.check("modifier", "gear-dice", bytes, bytes.clone()));
    }

    @Test
    void customBytesWarn() {
        Optional<String> message = BundledShadow.check("modifier", "gear-dice",
                text("custom"), text("bundled"));

        assertEquals(Optional.of("Custom modifier 'gear-dice' shares an id with the "
                + "engine-bundled copy; where both files are present the first in load order wins."),
                message);
    }

    @Test
    void missingBundledCopyWarns() {
        assertTrue(BundledShadow.check("preset", "chaos-mode", text("custom"), null)
                .isPresent());
    }

    @Test
    void messageNamesKindAndId() {
        assertEquals("Custom preset 'chaos-mode' shares an id with the engine-bundled copy; "
                + "where both files are present the first in load order wins.",
                BundledShadow.message("preset", "chaos-mode"));
    }

    private static byte[] text(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
