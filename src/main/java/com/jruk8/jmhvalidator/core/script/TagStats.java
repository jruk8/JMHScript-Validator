package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Stat tag keys behind {@code <pstat>}, {@code <rstat>}, and
 * {@code <gstat>}: matching is case-blind, and unknown keys are tag
 * errors listing the valid ones. Player keys add every vanilla
 * statistic, reached as {@code bukkit.<name>}, {@code b.<name>}, or
 * bare. Runtime reads stay engine-side.
 */
public final class TagStats {

    /** Player keys: live vitals plus match counters. */
    public static final List<String> PSTAT_KEYS =
            List.of("health", "hunger", "max-health", "mobs-killed", "achievements-gained",
                    "exp-level", "damage-dealt", "crafts", "blocks-broken");

    /** Global keys: the match clock plus the world clock. */
    public static final List<String> GSTAT_KEYS = List.of("duration", "daytime");

    /** Full bukkit prefix on stat keys, case-blind after normalization. */
    public static final String BUKKIT_PREFIX = "bukkit.";

    /** Short bukkit prefix on stat keys, case-blind after normalization. */
    public static final String BUKKIT_ALIAS = "b.";

    /** Vanilla statistic kinds, mirroring {@code Statistic.Type}. */
    public enum BukkitStatType {
        UNTYPED,
        ITEM,
        BLOCK,
        ENTITY
    }

    /**
     * Vanilla statistics by lowercase name, dumped from Paper
     * 26.2.build.133 and regrouped by kind (typed first). Refresh
     * against the Paper jar when the engine retargets; typed stats
     * need a qualifier at both edit time and runtime.
     */
    private static final Map<String, BukkitStatType> BUKKIT_STAT_TYPES = Map.ofEntries(
            Map.entry("break_item", BukkitStatType.ITEM),
            Map.entry("craft_item", BukkitStatType.ITEM),
            Map.entry("drop", BukkitStatType.ITEM),
            Map.entry("pickup", BukkitStatType.ITEM),
            Map.entry("use_item", BukkitStatType.ITEM),
            Map.entry("mine_block", BukkitStatType.BLOCK),
            Map.entry("entity_killed_by", BukkitStatType.ENTITY),
            Map.entry("kill_entity", BukkitStatType.ENTITY),
            Map.entry("animals_bred", BukkitStatType.UNTYPED),
            Map.entry("armor_cleaned", BukkitStatType.UNTYPED),
            Map.entry("aviate_one_cm", BukkitStatType.UNTYPED),
            Map.entry("banner_cleaned", BukkitStatType.UNTYPED),
            Map.entry("beacon_interaction", BukkitStatType.UNTYPED),
            Map.entry("bell_ring", BukkitStatType.UNTYPED),
            Map.entry("boat_one_cm", BukkitStatType.UNTYPED),
            Map.entry("brewingstand_interaction", BukkitStatType.UNTYPED),
            Map.entry("cake_slices_eaten", BukkitStatType.UNTYPED),
            Map.entry("cauldron_filled", BukkitStatType.UNTYPED),
            Map.entry("cauldron_used", BukkitStatType.UNTYPED),
            Map.entry("chest_opened", BukkitStatType.UNTYPED),
            Map.entry("clean_shulker_box", BukkitStatType.UNTYPED),
            Map.entry("climb_one_cm", BukkitStatType.UNTYPED),
            Map.entry("crafting_table_interaction", BukkitStatType.UNTYPED),
            Map.entry("crouch_one_cm", BukkitStatType.UNTYPED),
            Map.entry("damage_absorbed", BukkitStatType.UNTYPED),
            Map.entry("damage_blocked_by_shield", BukkitStatType.UNTYPED),
            Map.entry("damage_dealt", BukkitStatType.UNTYPED),
            Map.entry("damage_dealt_absorbed", BukkitStatType.UNTYPED),
            Map.entry("damage_dealt_resisted", BukkitStatType.UNTYPED),
            Map.entry("damage_resisted", BukkitStatType.UNTYPED),
            Map.entry("damage_taken", BukkitStatType.UNTYPED),
            Map.entry("deaths", BukkitStatType.UNTYPED),
            Map.entry("dispenser_inspected", BukkitStatType.UNTYPED),
            Map.entry("drop_count", BukkitStatType.UNTYPED),
            Map.entry("dropper_inspected", BukkitStatType.UNTYPED),
            Map.entry("enderchest_opened", BukkitStatType.UNTYPED),
            Map.entry("fall_one_cm", BukkitStatType.UNTYPED),
            Map.entry("fish_caught", BukkitStatType.UNTYPED),
            Map.entry("flower_potted", BukkitStatType.UNTYPED),
            Map.entry("fly_one_cm", BukkitStatType.UNTYPED),
            Map.entry("furnace_interaction", BukkitStatType.UNTYPED),
            Map.entry("happy_ghast_one_cm", BukkitStatType.UNTYPED),
            Map.entry("hopper_inspected", BukkitStatType.UNTYPED),
            Map.entry("horse_one_cm", BukkitStatType.UNTYPED),
            Map.entry("interact_with_anvil", BukkitStatType.UNTYPED),
            Map.entry("interact_with_blast_furnace", BukkitStatType.UNTYPED),
            Map.entry("interact_with_campfire", BukkitStatType.UNTYPED),
            Map.entry("interact_with_cartography_table", BukkitStatType.UNTYPED),
            Map.entry("interact_with_grindstone", BukkitStatType.UNTYPED),
            Map.entry("interact_with_lectern", BukkitStatType.UNTYPED),
            Map.entry("interact_with_loom", BukkitStatType.UNTYPED),
            Map.entry("interact_with_smithing_table", BukkitStatType.UNTYPED),
            Map.entry("interact_with_smoker", BukkitStatType.UNTYPED),
            Map.entry("interact_with_stonecutter", BukkitStatType.UNTYPED),
            Map.entry("item_enchanted", BukkitStatType.UNTYPED),
            Map.entry("jump", BukkitStatType.UNTYPED),
            Map.entry("leave_game", BukkitStatType.UNTYPED),
            Map.entry("minecart_one_cm", BukkitStatType.UNTYPED),
            Map.entry("mob_kills", BukkitStatType.UNTYPED),
            Map.entry("nautilus_one_cm", BukkitStatType.UNTYPED),
            Map.entry("noteblock_played", BukkitStatType.UNTYPED),
            Map.entry("noteblock_tuned", BukkitStatType.UNTYPED),
            Map.entry("open_barrel", BukkitStatType.UNTYPED),
            Map.entry("pig_one_cm", BukkitStatType.UNTYPED),
            Map.entry("play_one_minute", BukkitStatType.UNTYPED),
            Map.entry("player_kills", BukkitStatType.UNTYPED),
            Map.entry("raid_trigger", BukkitStatType.UNTYPED),
            Map.entry("raid_win", BukkitStatType.UNTYPED),
            Map.entry("record_played", BukkitStatType.UNTYPED),
            Map.entry("shulker_box_opened", BukkitStatType.UNTYPED),
            Map.entry("sleep_in_bed", BukkitStatType.UNTYPED),
            Map.entry("sneak_time", BukkitStatType.UNTYPED),
            Map.entry("sprint_one_cm", BukkitStatType.UNTYPED),
            Map.entry("strider_one_cm", BukkitStatType.UNTYPED),
            Map.entry("swim_one_cm", BukkitStatType.UNTYPED),
            Map.entry("talked_to_villager", BukkitStatType.UNTYPED),
            Map.entry("target_hit", BukkitStatType.UNTYPED),
            Map.entry("time_since_death", BukkitStatType.UNTYPED),
            Map.entry("time_since_rest", BukkitStatType.UNTYPED),
            Map.entry("total_world_time", BukkitStatType.UNTYPED),
            Map.entry("traded_with_villager", BukkitStatType.UNTYPED),
            Map.entry("trapped_chest_triggered", BukkitStatType.UNTYPED),
            Map.entry("walk_on_water_one_cm", BukkitStatType.UNTYPED),
            Map.entry("walk_one_cm", BukkitStatType.UNTYPED),
            Map.entry("walk_under_water_one_cm", BukkitStatType.UNTYPED)
    );

    private TagStats() {
    }

    /** Normalizes a stat key: trimmed and case-blind. */
    public static String normalizeKey(String key) {
        return key.strip().toLowerCase(Locale.ROOT);
    }

    /** True when the normalized key names a player stat. */
    public static boolean isPlayerKey(String normalized) {
        return PSTAT_KEYS.contains(normalized);
    }

    /** True when the normalized key names a global stat. */
    public static boolean isGlobalKey(String normalized) {
        return GSTAT_KEYS.contains(normalized);
    }

    /** Strips a bukkit prefix from a normalized key, if any. */
    public static String stripBukkitPrefix(String normalized) {
        if (normalized.startsWith(BUKKIT_PREFIX)) {
            return normalized.substring(BUKKIT_PREFIX.length());
        }
        if (normalized.startsWith(BUKKIT_ALIAS)) {
            return normalized.substring(BUKKIT_ALIAS.length());
        }
        return normalized;
    }

    /** True when the normalized key names a vanilla statistic. */
    public static boolean isBukkitKey(String normalized) {
        return BUKKIT_STAT_TYPES.containsKey(stripBukkitPrefix(normalized));
    }

    /** The vanilla kind of a normalized key, or empty when not a stat. */
    public static Optional<BukkitStatType> bukkitType(String normalized) {
        return Optional.ofNullable(BUKKIT_STAT_TYPES.get(stripBukkitPrefix(normalized)));
    }

    /** Every vanilla statistic name, lowercase. */
    public static Set<String> bukkitStatNames() {
        return BUKKIT_STAT_TYPES.keySet();
    }
}
