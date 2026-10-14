package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.SettingsNormalize;
import com.jruk8.jmhvalidator.core.model.OptionsNode;
import com.jruk8.jmhvalidator.core.model.SettingsTree;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Absent-field fallback pins: every key the engine
 * defaults reads silently with the documented fallback.
 * One row per entry of the catalog defaults table.
 */
class AbsentFieldsTest {

    private ModifierYaml.ReadResult read(Path tmp, String name, String yaml)
            throws Exception {
        Path file = tmp.resolve(name);
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        return ModifierYaml.read(file, name.replace(".yml", ""));
    }

    private void assertSilent(ModifierYaml.ReadResult read) {
        assertTrue(read.early().isEmpty());
        assertTrue(read.keys().isEmpty());
        assertTrue(read.shapes().isEmpty());
    }

    @Test
    void missingBehaviorReadsEmpty(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read =
                read(tmp, "fog.yml", "meta:\n  name: Fog\n");

        assertSilent(read);
        assertTrue(read.behavior().blocks().isEmpty());
        assertTrue(read.presetMembers().isEmpty());
    }

    @Test
    void missingRunsOnReadsEmpty(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "fog.yml",
                "behavior:\n  main:\n    commands:\n      player: [say hi]\n");

        assertSilent(read);
        assertTrue(read.behavior().blocks().get("main").runsOn().isEmpty());
    }

    @Test
    void emptyRunsOnReadsEmpty(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "fog.yml",
                "behavior:\n  main:\n    runs-on: []\n");

        assertSilent(read);
        assertTrue(read.behavior().blocks().get("main").runsOn().isEmpty());
    }

    @Test
    void missingCommandsReadsEmpty(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "fog.yml",
                "behavior:\n  main:\n    runs-on: [ON_START]\n");

        assertSilent(read);
        assertTrue(read.behavior().blocks().get("main").lists().isEmpty());
    }

    @Test
    void emptyCommandsMapReadsEmpty(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "fog.yml",
                "behavior:\n  main:\n    runs-on: [ON_START]\n    commands: {}\n");

        assertSilent(read);
        assertTrue(read.behavior().blocks().get("main").lists().isEmpty());
    }

    @Test
    void missingOptionsReadsEmpty(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "fog.yml",
                "behavior:\n  main:\n    runs-on: [ON_START]\n");

        assertSilent(read);
        assertEquals(OptionsNode.empty(),
                read.behavior().blocks().get("main").options());
    }

    @Test
    void missingRootsReadEmpty(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "fog.yml",
                "behavior:\n  main:\n    runs-on: [ON_START]\n");

        assertSilent(read);
        assertTrue(read.settings().roots().isEmpty());
        assertTrue(read.presetMembers().isEmpty());
    }

    @Test
    void bareLeafFallsThrough(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "fog.yml",
                "behavior:\n  main:\n    runs-on: [ON_START]\nconfig:\n"
                        + "  settings:\n    rate: {}\n");

        assertSilent(read);
        SettingsNormalize.Result normalized =
                SettingsNormalize.check("fog", read.settings(), null);
        assertTrue(normalized.findings().isEmpty());
        assertNull(read.settings().roots().get(0).spec());
    }

    @Test
    void valuelessTypelessLeafStaysSilent(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "fog.yml",
                "behavior:\n  main:\n    runs-on: [ON_START]\nconfig:\n"
                        + "  settings:\n    rate:\n      def:\n        value:\n"
                        + "          default: 5\n");

        assertSilent(read);
        SettingsNormalize.Result normalized =
                SettingsNormalize.check("fog", read.settings(), null);
        assertTrue(normalized.findings().isEmpty());
    }

    @Test
    void typelessValuePassesThrough(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "fog.yml",
                "behavior:\n  main:\n    runs-on: [ON_START]\nconfig:\n"
                        + "  settings:\n    rate:\n      value: 5\n");

        assertSilent(read);
        SettingsNormalize.Result normalized =
                SettingsNormalize.check("fog", read.settings(), null);
        assertTrue(normalized.findings().isEmpty());
    }

    @Test
    void emptyStringValueKept(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "fog.yml",
                "behavior:\n  main:\n    runs-on: [ON_START]\nconfig:\n"
                        + "  settings:\n    motto:\n      value: \"\"\n"
                        + "      def:\n        value:\n"
                        + "          type: STRING\n"
                        + "          default: hi\n");

        assertSilent(read);
        assertEquals("", read.settings().roots().get(0).spec().liveValue());
        SettingsNormalize.Result normalized =
                SettingsNormalize.check("fog", read.settings(), null);
        assertTrue(normalized.findings().isEmpty());
    }

    @Test
    void unconstrainedTypedDefaultPasses(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "fog.yml",
                "behavior:\n  main:\n    runs-on: [ON_START]\nconfig:\n"
                        + "  settings:\n    rate:\n      value: 5\n      def:\n"
                        + "        value:\n          type: INTEGER\n"
                        + "          default: 5\n");

        assertSilent(read);
        SettingsNormalize.Result normalized =
                SettingsNormalize.check("fog", read.settings(), null);
        assertTrue(normalized.findings().isEmpty());
    }

    @Test
    void emptyFolderReadsEmpty(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "fog.yml",
                "behavior:\n  main:\n    runs-on: [ON_START]\nconfig:\n"
                        + "  settings:\n    grp:\n      def:\n"
                        + "        meta:\n          icon: STONE\n"
                        + "        folder: []\n");

        assertSilent(read);
        assertTrue(read.settings().roots().get(0).folder());
        assertTrue(read.settings().roots().get(0).children().isEmpty());
        SettingsNormalize.Result normalized =
                SettingsNormalize.check("fog", read.settings(), null);
        assertTrue(normalized.findings().isEmpty());
    }

    @Test
    void iconCaptureMirrorsText(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "fog.yml",
                "behavior:\n  main:\n    runs-on: [ON_START]\nconfig:\n"
                        + "  settings:\n    grp:\n      def:\n"
                        + "        meta:\n          icon: 5\n"
                        + "        folder: []\n");

        assertSilent(read);
        assertEquals("5", read.settings().roots().get(0).icon());
        SettingsNormalize.Result normalized =
                SettingsNormalize.check("fog", read.settings(), null);
        assertTrue(normalized.findings().isEmpty());
    }

    @Test
    void presetWithoutMembersReadsEmpty(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read =
                read(tmp, "pack.yml", "meta:\n  name: Pack\nmodifiers: []\n");

        assertSilent(read);
        assertTrue(read.presetMembers().isEmpty());
        assertTrue(read.behavior().blocks().isEmpty());
    }

}
