package com.jruk8.jmhvalidator.core.script;

import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.SettingsTree;
import com.jruk8.jmhvalidator.core.model.SettingsTree.SettingsNode;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uncalled-sweep pins: unread leafs and uncalled blocks warn;
 * reads, triggers, and runblock edges exempt; dynamic ids skip
 * their sweep; folders and dropped trees never flag.
 */
class UncalledSweepTest {

    @Test
    void uncalledSettingWarns() {
        SettingsTree settings = tree(leaf("rate"));
        BehaviorFile file = blocks(Map.of("main", List.of("say hi")));

        List<Diagnostic> found = UncalledSweep.check("fog", file, settings);

        assertEquals(1, found.size());
        Diagnostic only = found.get(0);
        assertEquals(DiagnosticCode.W_UNCALLED_SETTING, only.code());
        assertEquals(Severity.WARNING, only.severity());
        assertEquals("rate", only.provenance().listName());
        assertEquals("Setting 'rate' is never read.", only.message());
    }

    @Test
    void typoCallSiteWarnsTheSetting() {
        SettingsTree settings = tree(leaf("rate"));
        BehaviorFile file = blocks(Map.of("main", List.of("say <setting:reta>")));

        List<Diagnostic> found = UncalledSweep.check("fog", file, settings);

        assertEquals(1, found.size());
        assertEquals("Setting 'rate' is never read.", found.get(0).message());
    }

    @Test
    void readSettingPasses() {
        SettingsTree settings = tree(leaf("rate"));
        BehaviorFile file = blocks(Map.of("main", List.of("say <setting:rate>")));

        assertTrue(UncalledSweep.check("fog", file, settings).isEmpty());
    }

    @Test
    void nestedLeafResolves() {
        SettingsTree settings = tree(folder("rewards", leaf("list")));
        BehaviorFile file = blocks(Map.of("main", List.of("say <setting:rewards.list>")));

        assertTrue(UncalledSweep.check("fog", file, settings).isEmpty());
    }

    @Test
    void foldersNeverFlagged() {
        SettingsTree settings = tree(folder("group", leaf("rate")));
        BehaviorFile file = blocks(Map.of("main", List.of("say hi")));

        List<Diagnostic> found = UncalledSweep.check("fog", file, settings);

        assertEquals(1, found.size());
        assertEquals("Setting 'group.rate' is never read.", found.get(0).message());
    }

    @Test
    void triggeredBehaviorPasses() {
        BehaviorFile file = blocks(Map.of("main", List.of("say hi")));

        assertTrue(UncalledSweep.check("fog", file, SettingsTree.empty()).isEmpty());
    }

    @Test
    void runblockCalledPasses() {
        BehaviorFile file = new BehaviorFile(Map.of(
                "main", new BehaviorFile.BehaviorBlock(
                        Map.of("player", List.of("say <runblock:side>")), List.of("ON_START")),
                "side", new BehaviorFile.BehaviorBlock(
                        Map.of("player", List.of("say hi")), List.of())));

        assertTrue(UncalledSweep.check("fog", file, SettingsTree.empty()).isEmpty());
    }

    @Test
    void uncalledBehaviorWarns() {
        BehaviorFile file = new BehaviorFile(Map.of(
                "main", new BehaviorFile.BehaviorBlock(
                        Map.of("player", List.of("say hi")), List.of("ON_START")),
                "side", new BehaviorFile.BehaviorBlock(
                        Map.of("player", List.of("say hi")), List.of())));

        List<Diagnostic> found = UncalledSweep.check("fog", file, SettingsTree.empty());

        assertEquals(1, found.size());
        Diagnostic only = found.get(0);
        assertEquals(DiagnosticCode.W_UNCALLED_BEHAVIOR, only.code());
        assertEquals("side", only.provenance().behaviorId());
        assertEquals("Behavior 'side' is never called.", only.message());
    }

    @Test
    void dynamicSettingIdSkipsSettingsSweep() {
        SettingsTree settings = tree(leaf("rate"));
        BehaviorFile file = blocks(Map.of("main",
                List.of("say <setting:rate-<setting:other>>")));

        assertTrue(UncalledSweep.check("fog", file, settings).isEmpty());
    }

    @Test
    void dynamicRunblockIdSkipsBehaviorsSweep() {
        BehaviorFile file = new BehaviorFile(Map.of(
                "main", new BehaviorFile.BehaviorBlock(
                        Map.of("player", List.of("say <runblock:<s:x>>")), List.of("ON_START")),
                "side", new BehaviorFile.BehaviorBlock(
                        Map.of("player", List.of("say hi")), List.of())));

        assertTrue(UncalledSweep.check("fog", file, SettingsTree.empty()).isEmpty());
    }

    @Test
    void droppedTreePasses() {
        BehaviorFile file = blocks(Map.of("main", List.of("say <setting:rate>")));

        assertTrue(UncalledSweep.check("fog", file, SettingsTree.empty()).isEmpty());
    }

    private static BehaviorFile blocks(Map<String, List<String>> byBlock) {
        Map<String, BehaviorFile.BehaviorBlock> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : byBlock.entrySet()) {
            out.put(entry.getKey(), new BehaviorFile.BehaviorBlock(
                    Map.of("player", entry.getValue()), List.of("ON_START")));
        }
        return new BehaviorFile(out);
    }

    private static SettingsTree tree(SettingsNode... roots) {
        return new SettingsTree(List.of(roots));
    }

    private static SettingsNode leaf(String name) {
        return new SettingsNode(name, false, List.of(), null, false);
    }

    private static SettingsNode folder(String name, SettingsNode... children) {
        return new SettingsNode(name, true, List.of(children), null, false);
    }
}
