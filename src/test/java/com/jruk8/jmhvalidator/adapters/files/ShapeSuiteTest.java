package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shape-failure pins, one per enumerated position: engine-throw
 * shapes error (with the engine text where the engine has one),
 * tolerated shapes warn with the effective read, and faithful
 * positions stay silent.
 */
class ShapeSuiteTest {

    private ModifierYaml.ReadResult read(Path tmp, String yaml) throws Exception {
        Path file = tmp.resolve("mod.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        return ModifierYaml.read(file, "mod");
    }

    private String early(ModifierYaml.ReadResult read) {
        return read.early().stream().map(Diagnostic::message)
                .reduce("", (left, right) -> left + "\n" + right);
    }

    private String shapes(ModifierYaml.ReadResult read) {
        return read.shapes().stream().map(Diagnostic::message)
                .reduce("", (left, right) -> left + "\n" + right);
    }

    @Test
    void badEnabledErrorsWithEngineText(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "enabled: 5\nbehavior:\n  main:\n    runs-on: [ON_START]\n");

        assertEquals(1, read.early().size());
        assertEquals(DiagnosticCode.E_YAML_SHAPE, read.early().get(0).code());
        assertEquals("enabled must be true or false", read.early().get(0).message());
    }

    @Test
    void yamlElevenBooleansPassEnabled(@TempDir Path tmp) throws Exception {
        for (String spelling : List.of("yes", "YES", "Yes", "no", "n", "N",
                "on", "Off", "true", "True", "FALSE")) {
            ModifierYaml.ReadResult read = read(tmp,
                    "enabled: " + spelling + "\nbehavior:\n  main:\n"
                            + "    runs-on: [ON_START]\n");
            assertTrue(read.early().isEmpty(), spelling);
        }
    }

    @Test
    void quotedTrueFailsEnabled(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "enabled: \"true\"\nbehavior:\n  main:\n    runs-on: [ON_START]\n");

        assertEquals(1, read.early().size());
        assertEquals("enabled must be true or false", read.early().get(0).message());
    }

    @Test
    void scalarMetaAndConfigError(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "enabled: true\nmeta: 5\nconfig: 5\nbehavior:\n  main:\n"
                        + "    runs-on: [ON_START]\n");

        assertTrue(early(read).contains("meta must be a mapping"), early(read));
        assertTrue(early(read).contains("config must be a mapping"), early(read));
    }

    @Test
    void bothSignalsErrorAmbiguous(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    runs-on: [ON_START]\nmodifiers: [gear-dice]\n");

        assertEquals(1, read.early().size());
        assertEquals("ambiguous file: has both modifier and preset content",
                read.early().get(0).message());
    }

    @Test
    void nullSignalsStaySilent(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\nmodifiers: [gear-dice]\n");

        assertTrue(read.early().isEmpty(), early(read));
        assertEquals(List.of("gear-dice"), read.presetMembers());
    }

    @Test
    void scalarMembersListErrors(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "modifiers: 5\n");

        assertEquals(1, read.early().size());
        assertEquals("modifiers must be a list of ids", read.early().get(0).message());
    }

    @Test
    void scalarNestedListErrors(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "presets: 5\n");

        assertEquals(1, read.early().size());
        assertEquals("presets must be a list of ids", read.early().get(0).message());
    }

    @Test
    void scalarMemberStringifies(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "modifiers: [gear-dice, 5]\n");

        assertEquals(List.of("gear-dice", "5"), read.presetMembers());
        assertTrue(read.early().isEmpty());
        assertTrue(read.shapes().isEmpty());
    }

    @Test
    void mapMemberErrors(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "modifiers: [{a: b}]\n");

        assertEquals(1, read.early().size());
        assertEquals("preset member must be a string", read.early().get(0).message());
        assertEquals("modifiers", read.early().get(0).provenance().listName());
    }

    @Test
    void nullMemberWarns(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "modifiers: [~]\n");

        assertTrue(read.early().isEmpty());
        assertEquals(1, read.shapes().size());
        assertEquals(DiagnosticCode.W_SHAPE, read.shapes().get(0).code());
        assertTrue(shapes(read).contains("it matches no modifier"), shapes(read));
    }

    @Test
    void nestedMemberShapesOnly(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult dropped = read(tmp, "presets: [5]\n");
        assertTrue(dropped.early().isEmpty());
        assertTrue(dropped.shapes().isEmpty());

        ModifierYaml.ReadResult bad = read(tmp, "presets: [{a: b}]\n");
        assertEquals(1, bad.early().size());
        assertEquals("preset member must be a string", bad.early().get(0).message());

        ModifierYaml.ReadResult empty = read(tmp, "presets: [~]\n");
        assertEquals(1, empty.shapes().size());
        assertTrue(shapes(empty).contains("it matches no preset"), shapes(empty));
    }

    @Test
    void nullBlockWarnsAndSkips(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp, "behavior:\n  main:\n");

        assertTrue(read.early().isEmpty());
        assertEquals(1, read.shapes().size());
        assertTrue(shapes(read).contains("behavior 'main' has no value"),
                shapes(read));
        assertTrue(read.behavior().blocks().isEmpty());
    }

    @Test
    void scalarTriggerStringifies(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    runs-on: [ON_START, 5]\n");

        assertEquals(List.of("ON_START", "5"),
                read.behavior().blocks().get("main").runsOn());
        assertTrue(read.early().isEmpty());
        assertTrue(read.shapes().isEmpty());
    }

    @Test
    void mapTriggerErrors(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    runs-on: [{a: b}]\n");

        assertEquals(1, read.early().size());
        assertEquals("runs-on entry must be a string", read.early().get(0).message());
        assertEquals("runs-on", read.early().get(0).provenance().listName());
    }

    @Test
    void nullTriggerWarns(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    runs-on: [~]\n");

        assertTrue(read.early().isEmpty());
        assertEquals(1, read.shapes().size());
        assertTrue(shapes(read).contains("it matches nothing"), shapes(read));
        assertTrue(read.behavior().blocks().get("main").runsOn().isEmpty());
    }

    @Test
    void scalarCommandsWarnAndEmpty(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    runs-on: [ON_START]\n    commands: say hi\n");

        assertTrue(read.early().isEmpty());
        assertEquals(1, read.shapes().size());
        assertEquals("commands in behavior 'main' must be a map; ignoring it.",
                read.shapes().get(0).message());
        assertTrue(read.behavior().blocks().get("main").lists().isEmpty());
    }

    @Test
    void listCommandsModelSyntheticList(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    runs-on: [ON_START]\n"
                        + "    commands: [say hi, 5]\n");

        assertTrue(read.early().isEmpty());
        assertEquals(2, read.shapes().size());
        assertTrue(shapes(read).contains("reading the entries as one list"),
                shapes(read));
        assertTrue(shapes(read).contains("reading it as '5'"), shapes(read));
        assertEquals(List.of("say hi", "5"), read.behavior().blocks().get("main")
                .lists().get("$$__value__$$"));
    }

    @Test
    void scalarListWarnsAndSkips(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    commands:\n      player: say hi\n");

        assertTrue(read.early().isEmpty());
        assertEquals(1, read.shapes().size());
        assertEquals("commands list 'player' in behavior 'main' must be a list;"
                + " ignoring it.", read.shapes().get(0).message());
        assertTrue(read.behavior().blocks().get("main").lists().isEmpty());
    }

    @Test
    void nullListWarnsAndSkips(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    commands:\n      player:\n"
                        + "      console: [say hi]\n");

        assertTrue(read.early().isEmpty());
        assertEquals(1, read.shapes().size());
        assertTrue(shapes(read).contains("has no value"), shapes(read));
        assertEquals(List.of("console"),
                List.copyOf(read.behavior().blocks().get("main").lists().keySet()));
    }

    @Test
    void emptyListWarnsAndStays(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    commands:\n      player: []\n");

        assertTrue(read.early().isEmpty());
        assertEquals(1, read.shapes().size());
        assertTrue(shapes(read).contains("it adds no lines"), shapes(read));
        assertTrue(read.behavior().blocks().get("main").lists()
                .get("player").isEmpty());
    }

    @Test
    void nullLineWarnsAndNulls(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    commands:\n      player: [~]\n");

        assertTrue(read.early().isEmpty());
        assertEquals(1, read.shapes().size());
        assertEquals(java.util.Arrays.asList((String) null), read.behavior()
                .blocks().get("main").lists().get("player"));
    }

    @Test
    void subtreeScalarsError(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    runs-on: [ON_START]\n    on-start: 5\n"
                        + "    options:\n      interval-settings: 5\n"
                        + "      success-chance: 5\n      execution: 5\n"
                        + "      delay: banana\n");

        assertTrue(early(read).contains("'behavior.main.on-start' must be a map"),
                early(read));
        assertTrue(early(read).contains(
                "'behavior.main.options.interval-settings' must be a map"), early(read));
        assertTrue(early(read).contains(
                "'behavior.main.options.success-chance' must be a map"), early(read));
        assertTrue(early(read).contains(
                "'behavior.main.options.execution' must be a map"), early(read));
        assertTrue(early(read).contains(
                "'behavior.main.options.delay' must be an integer"), early(read));
    }

    @Test
    void optionKindsError(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    runs-on: [ON_START]\n    options:\n"
                        + "      interval-settings:\n        interval: banana\n"
                        + "        behavior: {a: b}\n"
                        + "      execution:\n        selection: [x]\n"
                        + "        pick-random:\n          count: 2.5\n"
                        + "    on-start:\n      pre-start-order: [x]\n");

        assertTrue(early(read).contains(
                "'behavior.main.options.interval-settings.interval' must be a number"),
                early(read));
        assertTrue(early(read).contains(
                "'behavior.main.options.interval-settings.behavior' must be a string"),
                early(read));
        assertTrue(early(read).contains(
                "'behavior.main.options.execution.selection' must be a string"),
                early(read));
        assertTrue(early(read).contains(
                "'behavior.main.options.execution.pick-random.count' must be an integer"),
                early(read));
        assertTrue(early(read).contains(
                "'behavior.main.on-start.pre-start-order' must be a string"),
                early(read));
    }

    @Test
    void metaShapesError(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "meta:\n  name: [x]\n  visibility: 5\nbehavior:\n  main:\n"
                        + "    runs-on: [ON_START]\n");

        assertTrue(early(read).contains("'meta.name' must be a string"),
                early(read));
        assertTrue(early(read).contains("'meta.visibility' must be a map"),
                early(read));
    }

    @Test
    void scalarMetaStaysSilent(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "meta:\n  name: 5\nbehavior:\n  main:\n    runs-on: [ON_START]\n");

        assertTrue(read.early().isEmpty(), early(read));
        assertTrue(read.shapes().isEmpty(), shapes(read));
    }

    @Test
    void settingsShapesWarn(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    runs-on: [ON_START]\nconfig:\n"
                        + "  advanced: 5\n  settings:\n    scalar: 5\n"
                        + "    baddef:\n      def: 5\n"
                        + "    badspec:\n      def:\n        value: 5\n"
                        + "    badfolder:\n      def:\n        folder: 5\n"
                        + "    badmeta:\n      def:\n        meta: 5\n"
                        + "    grp:\n      def:\n        folder:\n"
                        + "          - 5\n          - {a: 1, b: 2}\n"
                        + "          - kid: 5\n");

        assertTrue(shapes(read).contains("'config.advanced' must be a map"),
                shapes(read));
        assertTrue(shapes(read).contains("'config.settings.scalar' must be a map"),
                shapes(read));
        assertTrue(shapes(read).contains("'config.settings.baddef.def' must be a map"),
                shapes(read));
        assertTrue(shapes(read).contains(
                "'config.settings.badspec.def.value' must be a map"), shapes(read));
        assertTrue(shapes(read).contains(
                "'config.settings.badfolder.def.folder' must be a list"), shapes(read));
        assertTrue(shapes(read).contains(
                "'config.settings.badmeta.def.meta' must be a map"), shapes(read));
        assertEquals(3, shapes(read).split("folder entry", -1).length - 1
                + shapes(read).split("'config.settings.grp.kid' must be a map",
                        -1).length - 1, shapes(read));
    }

    @Test
    void scalarSettingsWarns(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    runs-on: [ON_START]\nconfig:\n"
                        + "  settings: 5\n");

        assertEquals(1, read.shapes().size());
        assertEquals("'config.settings' must be a map; ignoring it.",
                read.shapes().get(0).message());
        assertTrue(read.settings().roots().isEmpty());
    }

    @Test
    void nullSettingsStaySilent(@TempDir Path tmp) throws Exception {
        ModifierYaml.ReadResult read = read(tmp,
                "behavior:\n  main:\n    runs-on: [ON_START]\nconfig:\n"
                        + "  advanced:\n  settings:\n    rate:\n      def:\n");

        assertTrue(read.early().isEmpty(), early(read));
        assertTrue(read.shapes().isEmpty(), shapes(read));
    }
}
