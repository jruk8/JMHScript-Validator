package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.model.SettingsTree;
import com.jruk8.jmhvalidator.core.model.SettingsTree.SettingsNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Settings model pins, ported from the tester fixture suite:
 * folder semantics, Serdes skips, and silent empty loads for
 * missing or misshapen settings. Never diagnoses.
 */
class SettingsYamlTest {
    @Test
    void valuePlusFolderReadsAsLeaf(@TempDir Path tmp) throws Exception {
        SettingsTree settings = read(tmp, "config:\n  settings:\n"
                + "    mixed:\n      value: 1\n      def:\n"
                + "        folder:\n          - a:\n              value: 1\n");

        assertEquals(List.of("mixed"), names(settings.roots()));
        assertFalse(settings.roots().get(0).folder());
        assertTrue(settings.roots().get(0).children().isEmpty());
    }

    @Test
    void nonListFolderReadsAsLeaf(@TempDir Path tmp) throws Exception {
        SettingsTree settings = read(tmp,
                "config:\n  settings:\n    odd:\n      def:\n        folder:\n          a: 1\n");

        assertEquals(1, settings.roots().size());
        assertFalse(settings.roots().get(0).folder());
    }

    @Test
    void nonMapEntriesAreSkipped(@TempDir Path tmp) throws Exception {
        SettingsTree settings = read(tmp, "config:\n  settings:\n"
                + "    scalar: 1\n    items: [1]\n    leaf:\n      value: 1\n");

        assertEquals(List.of("leaf"), names(settings.roots()));
    }

    @Test
    void multiKeyFolderItemsAreSkipped(@TempDir Path tmp) throws Exception {
        SettingsTree settings = read(tmp, "config:\n  settings:\n"
                + "    top:\n      def:\n        folder:\n"
                + "          - a:\n              value: 1\n"
                + "            b:\n              value: 2\n"
                + "          - ok:\n              value: 1\n");

        assertEquals(List.of("top"), names(settings.roots()));
        assertEquals(List.of("ok"), names(settings.roots().get(0).children()));
    }

    @Test
    void scalarFolderItemsAreSkipped(@TempDir Path tmp) throws Exception {
        SettingsTree settings = read(tmp, "config:\n  settings:\n"
                + "    top:\n      def:\n        folder:\n"
                + "          - just-a-string\n"
                + "          - ok:\n              value: 1\n");

        assertEquals(List.of("ok"), names(settings.roots().get(0).children()));
    }

    @Test
    void nonMapSettingsReadAsEmpty(@TempDir Path tmp) throws Exception {
        assertTrue(read(tmp, "config:\n  settings: [1]\n").roots().isEmpty());
        assertTrue(read(tmp, "config:\n  settings: nope\n").roots().isEmpty());
    }

    @Test
    void missingConfigReadsAsEmpty(@TempDir Path tmp) throws Exception {
        assertTrue(read(tmp, "meta:\n  name: Plain\n").roots().isEmpty());
        assertTrue(read(tmp, "config:\n  other: 1\n").roots().isEmpty());
        assertTrue(read(tmp, "config:\n  settings: {}\n").roots().isEmpty());
    }

    @Test
    void settingsRideAlongsideBehavior(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = ModifierYaml.read(write(tmp,
                "config:\n  settings:\n    alpha:\n      def:\n        folder:\n"
                        + "          - leaf_one:\n              value: 1\n"
                        + "behavior:\n  main:\n    commands:\n      player:\n"
                        + "        - 'say ok'\n"),
                "both");

        assertTrue(read.early().isEmpty());
        assertEquals(List.of("alpha"), names(read.settings().roots()));
        assertTrue(read.settings().roots().get(0).folder());
        assertEquals(List.of("leaf_one"),
                names(read.settings().roots().get(0).children()));
        assertEquals(List.of("say ok"),
                read.behavior().blocks().get("main").lists().get("player"));
    }

    private static SettingsTree read(Path tmp, String yaml) throws Exception {
        return ModifierYaml.read(write(tmp, yaml), "fixture").settings();
    }

    private static Path write(Path tmp, String yaml) throws Exception {
        Path file = tmp.resolve("fixture.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        return file;
    }

    private static List<String> names(List<SettingsNode> nodes) {
        List<String> names = new ArrayList<>();
        for (SettingsNode node : nodes) {
            names.add(node.name());
        }
        return names;
    }
}
