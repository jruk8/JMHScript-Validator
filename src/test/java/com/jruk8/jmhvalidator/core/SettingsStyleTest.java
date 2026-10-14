package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.SettingsTree;
import com.jruk8.jmhvalidator.core.model.SettingsTree.SettingsNode;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Settings style pins, ported 1:1 from the JMH-Mods settings
 * tester (R1/R2/R3 there). Trees mirror the tester fixtures;
 * codes, dotted paths, and message text must match the old
 * reports exactly, only the rule code changes (R1 to
 * W_SETTINGS_FOLDERS and so on).
 */
class SettingsStyleTest {
    @Test
    void bountyRootsFlagsTenFolders() {
        SettingsTree settings = tree(
                folder("targeting", leaf("interval")),
                folder("rules", leaf("eliminate")),
                folder("team_groceries", leaf("count")),
                folder("team_effects", leaf("count")),
                folder("team_stats", leaf("chance")),
                folder("killer_groceries", leaf("count")),
                folder("killer_effects", leaf("count")),
                folder("killer_stats", leaf("chance")),
                folder("sounds", leaf("team_kill")),
                folder("messages", leaf("normal")));

        List<Diagnostic> found = SettingsStyle.check("bounty", settings);

        assertEquals(1, found.size());
        Diagnostic only = found.get(0);
        assertEquals(DiagnosticCode.W_SETTINGS_FOLDERS, only.code());
        assertEquals(Severity.WARNING, only.severity());
        assertEquals("<root>", only.provenance().listName());
        assertEquals("", only.provenance().behaviorId());
        assertEquals(-1, only.provenance().lineIndex());
        assertEquals(-1, only.charOneBased());
        assertEquals("10 folders (max 5): targeting, rules, team_groceries, "
                + "team_effects, team_stats, killer_groceries, killer_effects, "
                + "killer_stats, sounds, messages", only.message());
    }

    @Test
    void nestedLevelFlagsSixFolders() {
        SettingsTree settings = tree(folder("outer",
                folder("group_a", leaf("leaf_one")),
                folder("group_b", leaf("leaf_one")),
                folder("group_c", leaf("leaf_one")),
                folder("group_d", leaf("leaf_one")),
                folder("group_e", leaf("leaf_one")),
                folder("group_f", leaf("leaf_one"))));

        List<Diagnostic> found = SettingsStyle.check("nested", settings);

        assertEquals(List.of("W_SETTINGS_FOLDERS:outer"), codesAndPaths(found));
        assertEquals("6 folders (max 5): group_a, group_b, group_c, "
                + "group_d, group_e, group_f", found.get(0).message());
    }

    @Test
    void flatRootFlagsEveryBareLeaf() {
        SettingsTree settings = tree(
                leaf("effect_list"), leaf("inflict_message"), leaf("inflict_sound"));

        List<Diagnostic> found = SettingsStyle.check("fog", settings);

        assertEquals(List.of("W_SETTINGS_STRAY_LEAF:effect_list",
                "W_SETTINGS_STRAY_LEAF:inflict_message",
                "W_SETTINGS_STRAY_LEAF:inflict_sound"), codesAndPaths(found));
        assertEquals("flat settings root holds bare leaf 'effect_list' "
                + "with zero folders; group leafs into folders",
                found.get(0).message());
    }

    @Test
    void flatCursedFlagsSixLeafsInOrder() {
        SettingsTree settings = tree(leaf("info_message"), leaf("info_sound"),
                leaf("effect_list"), leaf("neutralize_water_breathing"),
                leaf("warn_message"), leaf("warn_sound"));

        List<Diagnostic> found = SettingsStyle.check("cursed", settings);

        assertEquals(6, found.size());
        assertEquals(List.of("W_SETTINGS_STRAY_LEAF:info_message",
                "W_SETTINGS_STRAY_LEAF:info_sound",
                "W_SETTINGS_STRAY_LEAF:effect_list",
                "W_SETTINGS_STRAY_LEAF:neutralize_water_breathing",
                "W_SETTINGS_STRAY_LEAF:warn_message",
                "W_SETTINGS_STRAY_LEAF:warn_sound"), codesAndPaths(found));
    }

    @Test
    void strayLeafsBesideFoldersFlaggedEnabledExempt() {
        SettingsTree settings = tree(folder("alpha", leaf("leaf_one")),
                folder("bravo", leaf("leaf_one")),
                leaf("stray_one"), leaf("stray_two"), leaf("enabled"));

        List<Diagnostic> found = SettingsStyle.check("stray", settings);

        assertEquals(List.of("W_SETTINGS_STRAY_LEAF:stray_one",
                "W_SETTINGS_STRAY_LEAF:stray_two"), codesAndPaths(found));
        assertEquals("stray leaf 'stray_one' beside 2 folder(s); "
                + "only 'enabled' may sit beside folders", found.get(0).message());
    }

    @Test
    void messagesFlagsNineLeafs() {
        SettingsTree settings = tree(folder("messages", leaf("message"), leaf("sound"),
                leaf("empty_message"), leaf("empty_sound"), leaf("full_message"),
                leaf("full_sound"), leaf("recharge_sound"), leaf("shot_sound"),
                leaf("move_message")));

        List<Diagnostic> found = SettingsStyle.check("sneaky", settings);

        assertEquals(List.of("W_SETTINGS_LEAFS:messages"), codesAndPaths(found));
        assertEquals("9 leafs (max 7): message, sound, empty_message, empty_sound, "
                + "full_message, full_sound, recharge_sound, shot_sound, move_message",
                found.get(0).message());
    }

    @Test
    void sniperFlagsEightLeafs() {
        SettingsTree settings = tree(folder("sniper", leaf("lock_time"),
                leaf("min_distance"), leaf("leniency"), leaf("outcome"),
                leaf("damage_amount"), leaf("warn_target"), leaf("show_sniper_name"),
                leaf("glow")));

        List<Diagnostic> found = SettingsStyle.check("sniper", settings);

        assertEquals(List.of("W_SETTINGS_LEAFS:sniper"), codesAndPaths(found));
        assertEquals("8 leafs (max 7): lock_time, min_distance, leniency, outcome, "
                + "damage_amount, warn_target, show_sniper_name, glow",
                found.get(0).message());
    }

    @Test
    void soundsFlagsEightLeafs() {
        SettingsTree settings = tree(folder("sounds", leaf("team_kill"),
                leaf("killer_kill"), leaf("friendly"), leaf("expire"), leaf("warn"),
                leaf("offer"), leaf("assigned"), leaf("slain")));

        List<Diagnostic> found = SettingsStyle.check("bounty", settings);

        assertEquals(List.of("W_SETTINGS_LEAFS:sounds"), codesAndPaths(found));
    }

    @Test
    void atLimitRootsPass() {
        SettingsTree settings = tree(folder("alpha", leaf("leaf_one")),
                folder("bravo", leaf("leaf_one")), folder("charlie", leaf("leaf_one")),
                folder("delta", leaf("leaf_one")), folder("echo", leaf("leaf_one")));

        assertTrue(SettingsStyle.check("limits", settings).isEmpty());
    }

    @Test
    void atLimitCardPasses() {
        SettingsTree settings = tree(
                folder("match", leaf("mode"), leaf("pct"), leaf("gap")),
                folder("messages", leaf("normal")),
                folder("card", leaf("easycount"), leaf("midcount"), leaf("hardcount"),
                        leaf("actioncount"), leaf("specialcount"), leaf("dynacount"),
                        leaf("sharedcount")));

        assertTrue(SettingsStyle.check("bingo", settings).isEmpty());
    }

    @Test
    void enabledExemptAtRootAndNested() {
        SettingsTree settings = tree(folder("alpha", leaf("leaf_one")),
                folder("nested", folder("sub", leaf("leaf_one")), leaf("Enabled")),
                leaf("enabled"));

        assertTrue(SettingsStyle.check("exempt", settings).isEmpty());
    }

    @Test
    void nestedCleanPasses() {
        SettingsTree settings = tree(
                folder("outer", folder("group_a", leaf("leaf_one"), leaf("leaf_two")),
                        folder("group_b", leaf("leaf_one")), leaf("enabled")),
                folder("side", leaf("leaf_one")));

        assertTrue(SettingsStyle.check("nested", settings).isEmpty());
    }

    @Test
    void emptySettingsPass() {
        assertTrue(SettingsStyle.check("empty", SettingsTree.empty()).isEmpty());
        assertTrue(SettingsStyle.check("empty", new SettingsTree(List.of())).isEmpty());
    }

    @Test
    void enabledMatchingIgnoresCaseAndSeparators() {
        assertTrue(SettingsStyle.isEnabledName("enabled"));
        assertTrue(SettingsStyle.isEnabledName("Enabled"));
        assertTrue(SettingsStyle.isEnabledName("ENABLED"));
        assertFalse(SettingsStyle.isEnabledName("en-abled"));
        assertFalse(SettingsStyle.isEnabledName("en_abled"));
        assertFalse(SettingsStyle.isEnabledName("enable"));
        assertFalse(SettingsStyle.isEnabledName("disabled"));
    }

    @Test
    void normalizeUnifiesSeparators() {
        assertEquals(SettingsStyle.normalizeSegment("Start-With-Full"),
                SettingsStyle.normalizeSegment("start_with_full"));
    }

    @Test
    void flatRootExemptsNothing() {
        List<Diagnostic> found =
                SettingsStyle.check("flat", tree(leaf("enabled")));

        assertEquals(List.of("W_SETTINGS_STRAY_LEAF:enabled"), codesAndPaths(found));
    }

    @Test
    void nestedStrayCarriesDottedPath() {
        SettingsTree settings =
                tree(folder("outer", folder("sub", leaf("leaf_one")), leaf("stray")));

        List<Diagnostic> found = SettingsStyle.check("nested", settings);

        assertEquals(List.of("W_SETTINGS_STRAY_LEAF:outer.stray"),
                codesAndPaths(found));
    }

    @Test
    void nullSettingsStaySilent() {
        assertTrue(SettingsStyle.check("nulls", null).isEmpty());
        assertTrue(SettingsStyle.check("nulls", new SettingsTree(null)).isEmpty());
    }

    private static List<String> codesAndPaths(List<Diagnostic> found) {
        List<String> pairs = new ArrayList<>();
        for (Diagnostic item : found) {
            pairs.add(item.code().name() + ":" + item.provenance().listName());
        }
        return pairs;
    }

    private static SettingsNode leaf(String name) {
        return new SettingsNode(name, false, List.of());
    }

    private static SettingsNode folder(String name, SettingsNode... children) {
        return new SettingsNode(name, true, List.of(children));
    }

    private static SettingsTree tree(SettingsNode... roots) {
        return new SettingsTree(List.of(roots));
    }
}
