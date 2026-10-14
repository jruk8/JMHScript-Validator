package com.jruk8.jmhvalidator.core.script;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vanilla statistic table behind the player stat tags: full
 * coverage, typed kinds, plus prefix and case handling.
 */
class TagStatsTest {

    @Test
    void bukkitTableCoversEveryStatistic() {
        assertEquals(85, TagStats.bukkitStatNames().size());
    }

    @Test
    void typedStatsCarryTheirKind() {
        assertEquals(Optional.of(TagStats.BukkitStatType.ITEM),
                TagStats.bukkitType("break_item"));
        assertEquals(Optional.of(TagStats.BukkitStatType.ITEM),
                TagStats.bukkitType("craft_item"));
        assertEquals(Optional.of(TagStats.BukkitStatType.ITEM),
                TagStats.bukkitType("drop"));
        assertEquals(Optional.of(TagStats.BukkitStatType.ITEM),
                TagStats.bukkitType("pickup"));
        assertEquals(Optional.of(TagStats.BukkitStatType.ITEM),
                TagStats.bukkitType("use_item"));
        assertEquals(Optional.of(TagStats.BukkitStatType.BLOCK),
                TagStats.bukkitType("mine_block"));
        assertEquals(Optional.of(TagStats.BukkitStatType.ENTITY),
                TagStats.bukkitType("kill_entity"));
        assertEquals(Optional.of(TagStats.BukkitStatType.ENTITY),
                TagStats.bukkitType("entity_killed_by"));
        assertEquals(Optional.of(TagStats.BukkitStatType.UNTYPED),
                TagStats.bukkitType("damage_taken"));
        assertEquals(Optional.of(TagStats.BukkitStatType.UNTYPED),
                TagStats.bukkitType("play_one_minute"));
        assertEquals(Optional.of(TagStats.BukkitStatType.UNTYPED),
                TagStats.bukkitType("leave_game"));
        assertTrue(TagStats.bukkitType("bogus").isEmpty());
    }

    @Test
    void prefixesStripBeforeLookup() {
        assertEquals("damage_taken", TagStats.stripBukkitPrefix("bukkit.damage_taken"));
        assertEquals("damage_taken", TagStats.stripBukkitPrefix("b.damage_taken"));
        assertEquals("damage_taken", TagStats.stripBukkitPrefix("damage_taken"));
        assertTrue(TagStats.isBukkitKey("bukkit.damage_taken"));
        assertTrue(TagStats.isBukkitKey("b.damage_taken"));
        assertTrue(TagStats.isBukkitKey("damage_taken"));
        assertFalse(TagStats.isBukkitKey("b.bogus"));
        assertFalse(TagStats.isBukkitKey("health"));
    }

    @Test
    void keysMatchCaseBlindly() {
        assertTrue(TagStats.isBukkitKey(TagStats.normalizeKey("BUKKIT.DAMAGE_TAKEN")));
        assertTrue(TagStats.isBukkitKey(TagStats.normalizeKey("B.MINE_BLOCK")));
        assertTrue(TagStats.isBukkitKey(TagStats.normalizeKey("DaMAge_tAkEn")));
        assertTrue(TagStats.isPlayerKey(TagStats.normalizeKey("Max-Health")));
    }

    @Test
    void existingKeysNeverCollideWithStats() {
        for (String key : TagStats.PSTAT_KEYS) {
            assertFalse(TagStats.isBukkitKey(key), key);
        }
    }
}
