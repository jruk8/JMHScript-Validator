package com.jruk8.jmhvalidator.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hex color pins, moved 1:1 with the class from the engine:
 * accepted shapes, normalization, brand fallback.
 */
class HexColorsTest {

    @Test
    void acceptedShapesNormalizeToLowerSixDigits() {
        assertEquals("ffffff", HexColors.normalize("#ffffff").orElseThrow());
        assertEquals("ffffff", HexColors.normalize("ffffff").orElseThrow());
        assertEquals("ffffff", HexColors.normalize("0xffffff").orElseThrow());
        assertEquals("ffffff", HexColors.normalize("fff").orElseThrow());
        assertEquals("274bd9", HexColors.normalize("  #274BD9 ").orElseThrow());
    }

    @Test
    void invalidShapesReadEmpty() {
        assertTrue(HexColors.normalize(null).isEmpty());
        assertTrue(HexColors.normalize("").isEmpty());
        assertTrue(HexColors.normalize("gggggg").isEmpty());
        assertTrue(HexColors.normalize("#fffff").isEmpty());
        assertTrue(HexColors.normalize("fffffff").isEmpty());
        assertTrue(HexColors.normalize("red").isEmpty());
    }

    @Test
    void validAndBrandFallback() {
        assertTrue(HexColors.valid("ff00aa"));
        assertFalse(HexColors.valid("nope"));
        assertEquals(HexColors.BRAND, HexColors.orBrand(null));
        assertEquals(HexColors.BRAND, HexColors.orBrand("nope"));
        assertEquals("ff00aa", HexColors.orBrand("#FF00AA"));
    }
}
