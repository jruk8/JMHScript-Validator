package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.ports.KnownMaterials;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coercion pins, moved 1:1 with the logic from the engine:
 * zero values, default clamping, per-type coercion, and raw
 * comparison. Material names resolve through the port; a
 * null port leaves names unfiltered.
 */
class SettingCoerceTest {

    private static final KnownMaterials STONE =
            KnownMaterials.of(List.of("STONE", "DIAMOND_SWORD"));

    @Test
    void zerosMatchTypes() {
        assertEquals(Boolean.FALSE, SettingCoerce.zero(SettingType.BOOLEAN));
        assertEquals(0L, SettingCoerce.zero(SettingType.INTEGER));
        assertEquals(0.0, SettingCoerce.zero(SettingType.DOUBLE));
        assertEquals("", SettingCoerce.zero(SettingType.STRING));
        assertEquals(List.of(), SettingCoerce.zero(SettingType.LIST));
        assertEquals(List.of(false, ""), SettingCoerce.zero(SettingType.MESSAGE));
        assertEquals(List.of(false, "", 1.0, 1.0), SettingCoerce.zero(SettingType.SOUND));
    }

    @Test
    void defaultsClampToNearestBoundary() {
        assertEquals(100L, SettingCoerce.clampDefault(
                SettingType.INTEGER, 500L, Map.of("min", 0, "max", 100), null));
        assertEquals("FULL", SettingCoerce.clampDefault(
                SettingType.ENUM, "half", List.of("FULL", "BROADCAST"), null));
        assertEquals(0L, SettingCoerce.clampDefault(SettingType.INTEGER, null, null, null));
    }

    @Test
    void numbersClampAndRejectGarbage() {
        assertEquals(69L, SettingCoerce.coerce(
                SettingType.INTEGER, 69L, Map.of("min", 0, "max", 100), null));
        assertNull(SettingCoerce.coerce(SettingType.INTEGER, "many", null, null));
        assertEquals(1.5, SettingCoerce.coerce(SettingType.DOUBLE, "1.5", null, null));
        assertNull(SettingCoerce.coerce(SettingType.DOUBLE, "NaN", null, null));
        assertNull(SettingCoerce.coerce(SettingType.DOUBLE, "Infinity", null, null));
    }

    @Test
    void stringsHonorCaps() {
        assertEquals("ab", SettingCoerce.coerce(
                SettingType.STRING, "abc", Map.of("maxLength", 2), null));
        assertEquals("abc", SettingCoerce.coerce(SettingType.STRING, "abc", null, null));
    }

    @Test
    void materialsResolveThroughPort() {
        assertEquals("STONE", SettingCoerce.coerce(
                SettingType.MATERIAL, "stone", null, STONE));
        assertNull(SettingCoerce.coerce(SettingType.MATERIAL, "nope", null, STONE));
        assertEquals("NOPE", SettingCoerce.coerce(
                SettingType.MATERIAL, "nope", null, null));
        assertEquals("MINECRAFT:DIAMOND SWORD", SettingCoerce.coerce(
                SettingType.MATERIAL, "minecraft:diamond sword", null, STONE));
    }

    @Test
    void colorsNormalize() {
        assertEquals("ff00aa", SettingCoerce.coerce(
                SettingType.COLOR, "#FF00AA", null, null));
        assertNull(SettingCoerce.coerce(SettingType.COLOR, "red", null, null));
    }

    @Test
    void listsKeepNestedRows() {
        List<Object> nested = List.of(
                List.of(true, "alpha", 0.6, 0.9), List.of(false, "beta", 1.0, 1.0));
        assertEquals(nested, SettingCoerce.coerce(SettingType.LIST, nested, null, null));
        assertEquals(List.of("1", "true"), SettingCoerce.coerce(
                SettingType.LIST, List.of(1L, true), null, null));
        assertEquals(List.of("a"), SettingCoerce.coerce(
                SettingType.LIST, List.of(List.of("a"), "a"), List.of("a"), null));
    }

    @Test
    void triplesValidateShapeAndMaterials() {
        assertEquals(List.of(List.of("SPEED", 1L, 30L)), SettingCoerce.coerce(
                SettingType.EFFECTS, List.of(List.of("SPEED", 1, 30)), null, null));
        assertEquals(List.of(), SettingCoerce.coerce(
                SettingType.EFFECTS, List.of(List.of("SPEED", 1)), null, null));
        assertEquals(List.of(List.of("stone", 1L, 2L)), SettingCoerce.coerce(
                SettingType.ITEMS, List.of(List.of("stone", 1, 2)), null, STONE));
        assertEquals(List.of(), SettingCoerce.coerce(
                SettingType.ITEMS, List.of(List.of("nope", 1, 2)), null, STONE));
        assertEquals(List.of(List.of("nope", 1L, 2L)), SettingCoerce.coerce(
                SettingType.ITEMS, List.of(List.of("nope", 1, 2)), null, null));
    }

    @Test
    void weightedPairsCoerce() {
        assertEquals(List.of(List.of(10.0, "dirt"), List.of(0.0, "diamond"),
                        List.of(1.0, "not_a_material"), List.of(5.0, "[diamond, 3, 5]")),
                SettingCoerce.coerce(SettingType.WEIGHTED_ITEM,
                        List.of(List.of(10.0, "dirt"), List.of(0.0, "diamond"),
                                List.of("oops"), List.of(1.0, "not_a_material"),
                                List.of(5.0, List.of("diamond", 3, 5)),
                                List.of(2.0, ""), List.of("heavy", "dirt")),
                        null, null));
    }

    @Test
    void tuplesCoerceStructurally() {
        assertEquals(List.of(true, "hi"), SettingCoerce.coerce(
                SettingType.MESSAGE, List.of(true, "hi"), null, null));
        assertNull(SettingCoerce.coerce(SettingType.MESSAGE, List.of("hi"), null, null));
        assertEquals(List.of(true, "x", 1.0, 2.0), SettingCoerce.coerce(
                SettingType.SOUND, List.of(true, "x", 1, 2), null, null));
        assertNull(SettingCoerce.coerce(
                SettingType.SOUND, List.of(true, "x", 1), null, null));
    }

    @Test
    void enumsFallBackToFirstMember() {
        assertEquals("FULL", SettingCoerce.coerce(
                SettingType.ENUM, "zzz", List.of("FULL", "BROADCAST"), null));
        assertEquals("zzz", SettingCoerce.coerce(
                SettingType.ENUM, "zzz", null, null));
        assertEquals("zzz", SettingCoerce.coerce(
                SettingType.ENUM, "zzz", List.of(), null));
    }

    @Test
    void booleansRejectGarbage() {
        assertNull(SettingCoerce.coerce(SettingType.BOOLEAN, "maybe", null, null));
        assertEquals(Boolean.TRUE,
                SettingCoerce.coerce(SettingType.BOOLEAN, " true ", null, null));
    }

    @Test
    void sameRawComparesNumbersAndLists() {
        assertTrue(SettingCoerce.sameRaw(5L, 5));
        assertTrue(SettingCoerce.sameRaw(null, null));
        assertFalse(SettingCoerce.sameRaw(0L, null));
        assertFalse(SettingCoerce.sameRaw(5L, 6L));
        assertTrue(SettingCoerce.sameRaw(List.of(1L, "a"), List.of(1, "a")));
        assertFalse(SettingCoerce.sameRaw(List.of(1L), List.of(1L, 2L)));
    }
}
