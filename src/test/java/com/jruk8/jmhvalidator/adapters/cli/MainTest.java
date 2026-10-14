package com.jruk8.jmhvalidator.adapters.cli;

import com.jruk8.jmhvalidator.adapters.files.EngineDefaults;
import com.jruk8.jmhvalidator.core.ports.ShareMaterials;
import com.jruk8.jmhvalidator.core.share.ShareCodec;
import com.jruk8.jmhvalidator.core.share.ShareModel;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CLI smoke pins: usage errors exit 2, real diagnostics exit
 * 1 with codes, clean files exit 0. The corpus parity test
 * owns message fidelity; this owns the wiring.
 */
class MainTest {
    private String run(int expected, String... args) {
        return capture(() -> Main.run(args), expected);
    }

    private String capture(Supplier<Integer> call, int expected) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        try {
            System.setOut(capture);
            System.setErr(capture);
            assertEquals(expected, call.get());
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void missingRegistryIsUsageError() {
        String output = run(2, "corpus/inputs/crafted/sweep-coverage.yml");
        assertTrue(output.contains("missing --registry"), output);
    }

    @Test
    void unknownOptionIsUsageError() {
        String output = run(2, "--bogus");
        assertTrue(output.contains("unknown option"), output);
    }

    @Test
    void craftedSweepReportsCodesAndExitsOne() {
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                "corpus/inputs/crafted/sweep-coverage.yml");
        assertTrue(output.contains("E_SYNTAX"), output);
        assertTrue(output.contains("W_UNKNOWN_TAG"), output);
        assertTrue(output.contains("with diagnostics"), output);
    }

    @Test
    void cleanFileExitsZero() {
        String output = run(0, "--registry", "src/test/resources/tag-registry.json",
                "corpus/inputs/shipped/diamond-on-advancement.yml");
        assertTrue(output.contains("1 clean"), output);
    }

    @Test
    void styleOnlyFileExitsOne() {
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                "corpus/inputs/shipped/vip-escort.yml");
        assertTrue(output.contains("0 clean"), output);
        assertTrue(output.contains("W_SETTINGS_STRAY_LEAF"), output);
        assertTrue(output.contains("vip-escort.yml effects warning"), output);
    }

    @Test
    void engineMismatchFailsClosed() {
        String output = run(2, "--registry", "src/test/resources/tag-registry.json",
                "--expect-engine-version", "rev-nope",
                "corpus/inputs/crafted/sweep-coverage.yml");
        assertTrue(output.contains("does not match"), output);
    }

    @Test
    void docsEmitsTagList() {
        String output = run(0, "docs", "--registry", "src/test/resources/tag-registry.json");
        assertTrue(output.contains("- `p`"), output);
        assertTrue(output.contains("GENERATED"), output);
    }

    @Test
    void versionAndHelpExitZero() {
        run(0, "--version");
        String output = run(0, "--help");
        assertTrue(output.contains("Usage"), output);
    }

    @Test
    void fetchDefaultsNeedsRevAndOut() {
        String missingRev = run(2, "fetch-defaults", "--out", "defaults");
        assertTrue(missingRev.contains("--engine-rev"), missingRev);
        String missingOut = run(2, "fetch-defaults", "--engine-rev", "rev-abc");
        assertTrue(missingOut.contains("--out"), missingOut);
    }

    @Test
    void presetUnknownMemberWarnsWithDefaults(@TempDir Path tmp) throws Exception {
        Fixture fixture = new Fixture(tmp, List.of("fog-of-war", "infection", "no-such-mod"));
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                "--defaults", fixture.manifest().toString(),
                fixture.mods().toString(), fixture.preset().toString());
        assertTrue(output.contains("W_UNKNOWN_MODIFIER"), output);
        assertTrue(output.contains(
                "Preset 'horror' references unknown modifier 'no-such-mod'; skipping."),
                output);
        assertFalse(output.contains("unknown modifier 'infection'"), output);
        assertFalse(output.contains("unknown modifier 'fog-of-war'"), output);
    }

    @Test
    void presetResolvesAgainstDefaults(@TempDir Path tmp) throws Exception {
        Fixture fixture = new Fixture(tmp, List.of("fog-of-war", "infection"));
        String output = run(0, "--registry", "src/test/resources/tag-registry.json",
                "--defaults", fixture.manifest().toString(),
                fixture.mods().toString(), fixture.preset().toString());
        assertTrue(output.contains("2 clean"), output);
    }

    @Test
    void presetStaysSilentWithoutDefaults(@TempDir Path tmp) throws Exception {
        Fixture fixture = new Fixture(tmp, List.of("fog-of-war", "no-such-mod"));
        String output = run(0, "--registry", "src/test/resources/tag-registry.json",
                fixture.mods().toString(), fixture.preset().toString());
        assertTrue(output.contains("2 clean"), output);
    }

    @Test
    void customShadowWarns(@TempDir Path tmp) throws Exception {
        Path defaults = shadowDefaults(tmp, "gear-dice", "bundled");
        Path mods = tmp.resolve("mods");
        Files.createDirectories(mods);
        Files.writeString(mods.resolve("gear-dice.yml"),
                "behavior:\n  main:\n    runs-on:\n      - ON_START\n"
                        + "    commands:\n      player:\n        - say custom\n",
                StandardCharsets.UTF_8);

        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                "--defaults", defaults.toString(), mods.toString());

        assertTrue(output.contains("W_SHADOWED_ID"), output);
        assertTrue(output.contains("Custom modifier 'gear-dice' shares an id with the "
                + "engine-bundled copy; where both files are present the first in load order wins."),
                output);
    }

    @Test
    void pristineCopyPasses(@TempDir Path tmp) throws Exception {
        String body = "behavior:\n  main:\n    runs-on:\n      - ON_START\n"
                + "    commands:\n      player:\n        - say hi\n";
        Path defaults = shadowDefaults(tmp, "gear-dice", body);
        Path mods = tmp.resolve("mods");
        Files.createDirectories(mods);
        Files.writeString(mods.resolve("gear-dice.yml"), body, StandardCharsets.UTF_8);

        String output = run(0, "--registry", "src/test/resources/tag-registry.json",
                "--defaults", defaults.toString(), mods.toString());

        assertFalse(output.contains("W_SHADOWED_ID"), output);
    }

    @Test
    void shadowSkipsWithoutDefaults(@TempDir Path tmp) throws Exception {
        Path mods = tmp.resolve("mods");
        Files.createDirectories(mods);
        Files.writeString(mods.resolve("gear-dice.yml"),
                "behavior:\n  main:\n    runs-on:\n      - ON_START\n"
                        + "    commands:\n      player:\n        - say custom\n",
                StandardCharsets.UTF_8);

        String output = run(0, "--registry", "src/test/resources/tag-registry.json",
                mods.toString());

        assertFalse(output.contains("W_SHADOWED_ID"), output);
    }

    @Test
    void presetShadowWarns(@TempDir Path tmp) throws Exception {
        Path defaults = tmp.resolve("defaults");
        Files.createDirectories(defaults.resolve("presets"));
        EngineDefaults.write(defaults.resolve("engine-defaults.json"), "JManhunt", "rev-test",
                List.of(), List.of(new EngineDefaults.Preset("chaos-mode", List.of("beef"))));
        Files.writeString(defaults.resolve("presets/chaos-mode.yml"),
                "meta:\n  name: Bundled\nmodifiers:\n  - beef\n", StandardCharsets.UTF_8);
        Files.writeString(tmp.resolve("chaos-mode.yml"),
                "meta:\n  name: Custom\nmodifiers:\n  - beef\n", StandardCharsets.UTF_8);

        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                "--defaults", defaults.toString(), tmp.resolve("chaos-mode.yml").toString());

        assertTrue(output.contains("W_SHADOWED_ID"), output);
        assertTrue(output.contains("Custom preset 'chaos-mode' shares an id"),
                output);
    }

    private static Path shadowDefaults(Path tmp, String id, String body) throws Exception {
        Path defaults = tmp.resolve("defaults");
        Files.createDirectories(defaults.resolve("modifiers"));
        EngineDefaults.write(defaults.resolve("engine-defaults.json"), "JManhunt", "rev-test",
                List.of(id), List.of());
        Files.writeString(defaults.resolve("modifiers/" + id + ".yml"), body,
                StandardCharsets.UTF_8);
        return defaults;
    }

    @Test
    void exportWritesStampedFolder(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("in");
        Files.createDirectories(in);
        Files.writeString(in.resolve("beef.yml"),
                "meta:\n  name: Beef\nbehavior:\n  main:\n    commands:\n      player:\n        - say hi\n",
                StandardCharsets.UTF_8);
        Files.writeString(in.resolve("pack.yml"), "meta:\n  name: Pack\nmodifiers:\n  - beef\n",
                StandardCharsets.UTF_8);

        String output = capture(
                () -> Main.exportCommand(List.of(in.toString()), tmp.resolve("export")), 0);

        Path stamped = tmp.resolve("export").resolve(LocalDate.now() + "-1-export");
        Path mod = stamped.resolve("beef.txt");
        Path preset = stamped.resolve("pack.txt");
        assertTrue(Files.isRegularFile(mod), output);
        assertTrue(Files.isRegularFile(preset), output);
        ShareCodec.Imported back = ShareCodec.decode(
                Files.readString(mod, StandardCharsets.UTF_8), ShareMaterials.lenient())
                .orElseThrow();
        assertEquals(ShareCodec.Kind.MODIFIER, back.kind());
        assertEquals("beef", back.id());
        assertEquals("Beef", back.modifier().meta().name());
        ShareCodec.Imported pack = ShareCodec.decode(
                Files.readString(preset, StandardCharsets.UTF_8), ShareMaterials.lenient())
                .orElseThrow();
        assertEquals(ShareCodec.Kind.PRESET, pack.kind());
        assertEquals(List.of("beef"), pack.preset().modifiers());
    }

    @Test
    void exportIncrementsStamp(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("in");
        Files.createDirectories(in);
        Files.writeString(in.resolve("beef.yml"), "meta:\n  name: Beef\n",
                StandardCharsets.UTF_8);
        Path base = tmp.resolve("export");

        capture(() -> Main.exportCommand(List.of(in.toString()), base), 0);
        String output = capture(() -> Main.exportCommand(List.of(in.toString()), base), 0);

        assertTrue(output.contains("-2-export"), output);
        assertTrue(Files.isDirectory(base.resolve(LocalDate.now() + "-1-export")));
        assertTrue(Files.isDirectory(base.resolve(LocalDate.now() + "-2-export")));
    }

    @Test
    void exportInvalidFileFails(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("in");
        Files.createDirectories(in);
        Files.writeString(in.resolve("bad.yml"), "behavior: [unclosed\n",
                StandardCharsets.UTF_8);

        String output = capture(
                () -> Main.exportCommand(List.of(in.toString()), tmp.resolve("export")), 1);

        assertTrue(output.contains("bad.yml"), output);
    }

    @Test
    void exportNoFilesIsSetupError(@TempDir Path tmp) throws Exception {
        Path empty = tmp.resolve("empty");
        Files.createDirectories(empty);

        String output = capture(() -> Main.exportCommand(
                List.of(empty.toString()), tmp.resolve("export")), 2);

        assertTrue(output.contains("no .yml/.yaml files found"), output);
    }

    @Test
    void exportNeedsPaths() {
        String output = capture(() -> Main.exportCommand(List.of(), Path.of("export")), 2);

        assertTrue(output.contains("export needs PATH"), output);
    }

    @Test
    void importInlineRoundTrip(@TempDir Path tmp) throws Exception {
        ShareModel.Modifier modifier = new ShareModel.Modifier(
                new ShareModel.Meta("Beef", null, "TNT", null, null), null, null, null);
        String payload = ShareCodec.exportModifier("beef", modifier);

        String output = capture(
                () -> Main.importCommand(List.of(payload), tmp.resolve("imports")), 0);

        Path file = tmp.resolve("imports").resolve(LocalDate.now() + "-1-import")
                .resolve("beef.yml");
        assertTrue(Files.isRegularFile(file), output);
        String yaml = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(yaml.contains("Beef"), yaml);
        assertTrue(yaml.contains("TNT"), yaml);
        assertTrue(yaml.contains("enabled: false"), yaml);
    }

    @Test
    void importFromFileIgnoresExtension(@TempDir Path tmp) throws Exception {
        ShareModel.Modifier modifier = new ShareModel.Modifier(
                new ShareModel.Meta("Beef", null, null, null, null), null, null, null);
        Path holder = tmp.resolve("share.dat");
        Files.writeString(holder, ShareCodec.exportModifier("beef", modifier),
                StandardCharsets.UTF_8);

        String output = capture(() -> Main.importCommand(
                List.of(holder.toString()), tmp.resolve("imports")), 0);

        assertTrue(output.contains("imported beef"), output);
    }

    @Test
    void importInvalidStringFails(@TempDir Path tmp) {
        String output = capture(() -> Main.importCommand(
                List.of("JMH1R:nope"), tmp.resolve("imports")), 1);

        assertTrue(output.contains("invalid share string"), output);
    }

    @Test
    void importNeedsInput() {
        String output = capture(() -> Main.importCommand(List.of(), Path.of("imports")), 2);

        assertTrue(output.contains("import needs INPUT"), output);
    }

    @Test
    void stampedFoldersIncrementFromOne(@TempDir Path tmp) throws Exception {
        Path base = tmp.resolve("export");
        LocalDate day = LocalDate.of(2026, 10, 14);

        assertEquals(base.resolve("2026-10-14-1-export"),
                Main.nextStampedDir(base, day, "export"));
        assertEquals(base.resolve("2026-10-14-2-export"),
                Main.nextStampedDir(base, day, "export"));
    }

    @Test
    void nestedDuplicateModifierWarnsOnSkipped(@TempDir Path tmp) throws Exception {
        Path root = tmp.resolve("mods");
        Files.createDirectories(root.resolve("deep"));
        writeBehavior(root.resolve("fog.yml"), "say one");
        writeBehavior(root.resolve("deep/fog.yml"), "say two");
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                root.toString());
        assertTrue(output.contains("W_DUPLICATE_ID"), output);
        assertTrue(output.contains(
                "Duplicate modifier id 'fog': '" + root.resolve("deep/fog.yml")
                        + "' wins, skipping '" + root.resolve("fog.yml") + "'."),
                output);
    }

    @Test
    void naturalOrderPicksDuplicateWinner(@TempDir Path tmp) throws Exception {
        Path root = tmp.resolve("mods");
        Files.createDirectories(root.resolve("mod10"));
        Files.createDirectories(root.resolve("mod2"));
        writeBehavior(root.resolve("mod10/dup.yml"), "say ten");
        writeBehavior(root.resolve("mod2/dup.yml"), "say two");
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                root.toString());
        assertTrue(output.contains(
                "' wins, skipping '" + root.resolve("mod10/dup.yml") + "'."),
                output);
    }

    @Test
    void modifierAndPresetShareIdSilently(@TempDir Path tmp) throws Exception {
        Path root = tmp.resolve("mods");
        Files.createDirectories(root.resolve("deep"));
        writeBehavior(root.resolve("fog.yml"), "say one");
        Files.writeString(root.resolve("deep/fog.yml"), "meta:\n  name: Fog\nmodifiers:\n  - fog\n",
                StandardCharsets.UTF_8);
        String output = run(0, "--registry", "src/test/resources/tag-registry.json",
                root.toString());
        assertTrue(output.contains("2 clean"), output);
    }

    @Test
    void yamlSuffixNeverDuplicatesYml(@TempDir Path tmp) throws Exception {
        Path root = tmp.resolve("mods");
        Files.createDirectories(root);
        writeBehavior(root.resolve("fog.yml"), "say one");
        writeBehavior(root.resolve("fog.yaml"), "say two");
        String output = run(0, "--registry", "src/test/resources/tag-registry.json",
                root.toString());
        assertFalse(output.contains("W_DUPLICATE_ID"), output);
    }

    @Test
    void duplicateSettingNamesDropStyle(@TempDir Path tmp) throws Exception {
        StringBuilder yaml = new StringBuilder("behavior:\n  main:\n    commands:\n"
                + "      player:\n        - say hi\nconfig:\n  settings:\n");
        for (int index = 0; index < 6; index++) {
            yaml.append("    folder").append(index).append(":\n      def:\n        folder:\n");
            yaml.append("          - leaf:\n              value: 1\n");
            if (index == 0) {
                yaml.append("          - leaf:\n              value: 2\n");
            }
        }
        Path file = tmp.resolve("fog.yml");
        Files.writeString(file, yaml.toString(), StandardCharsets.UTF_8);
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                file.toString());
        assertTrue(output.contains("W_DUPLICATE_SETTING"), output);
        assertTrue(output.contains(
                "Modifier 'fog' has duplicate setting names at 'folder0'."), output);
        assertTrue(output.contains("dropping its config.settings block."), output);
        assertFalse(output.contains("W_SETTINGS_FOLDERS"), output);
    }

    @Test
    void undefinedSettingRefWarns(@TempDir Path tmp) throws Exception {
        String yaml = "behavior:\n  main:\n    commands:\n      player:\n"
                + "        - say <setting:rate>\n        - say <setting:nope>\n"
                + "config:\n  settings:\n    rate:\n      value: 1\n";
        Path file = tmp.resolve("fog.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                file.toString());
        assertTrue(output.contains("W_UNKNOWN_SETTING"), output);
        assertTrue(output.contains(
                "Unknown setting 'nope', resolves to null at runtime."), output);
        assertFalse(output.contains("Unknown setting 'rate'"), output);
    }

    @Test
    void unknownTriggerWarns(@TempDir Path tmp) throws Exception {
        String yaml = "behavior:\n  main:\n    runs-on:\n      - ON_START\n"
                + "      - ULTIMATE_INTERVAL\n    commands:\n      player:\n"
                + "        - say hi\n";
        Path file = tmp.resolve("fog.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                file.toString());
        assertTrue(output.contains("W_UNKNOWN_TRIGGER"), output);
        assertTrue(output.contains(
                "Unknown trigger 'ULTIMATE_INTERVAL', it never fires."), output);
    }

    @Test
    void typoKeysWarn(@TempDir Path tmp) throws Exception {
        String yaml = "behavio:\n  main: {}\nbehavior:\n  main:\n    commands:\n"
                + "      player:\n        - say hi\n";
        Path file = tmp.resolve("fog.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                file.toString());
        assertTrue(output.contains("W_UNKNOWN_KEY"), output);
        assertTrue(output.contains(
                "Unknown key 'behavio' in the file root; ignoring it."), output);
    }

    @Test
    void defaultOutsideAllowedWarns(@TempDir Path tmp) throws Exception {
        String yaml = "behavior:\n  main:\n    commands:\n      player:\n"
                + "        - say hi\nconfig:\n  settings:\n    rate:\n"
                + "      value: 1\n      def:\n        value:\n"
                + "          type: INTEGER\n          default: 500\n"
                + "          allowed:\n            min: 0\n            max: 100\n";
        Path file = tmp.resolve("fog.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                file.toString());
        assertTrue(output.contains("W_DEFAULT_OUTSIDE_ALLOWED"), output);
        assertTrue(output.contains(
                "Modifier 'fog' setting 'rate' default is outside allowed; clamped."),
                output);
    }

    @Test
    void valueInvalidWarns(@TempDir Path tmp) throws Exception {
        String yaml = "behavior:\n  main:\n    commands:\n      player:\n"
                + "        - say hi\nconfig:\n  settings:\n    rate:\n"
                + "      value: 500\n      def:\n        value:\n"
                + "          type: INTEGER\n          default: 50\n"
                + "          allowed:\n            min: 0\n            max: 100\n";
        Path file = tmp.resolve("fog.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                file.toString());
        assertTrue(output.contains("W_VALUE_INVALID"), output);
        assertTrue(output.contains(
                "Modifier 'fog' setting 'rate' value is invalid; reset to default."),
                output);
    }

    @Test
    void duplicateKeyWarnsAndKeepsLast(@TempDir Path tmp) throws Exception {
        String yaml = "config:\n  settings:\n    rate:\n      value: 1\n"
                + "      def:\n        value:\n          type: INTEGER\n"
                + "          default: 1\nconfig:\n  settings:\n    rate:\n"
                + "      value: 2\n      def:\n        value:\n"
                + "          type: INTEGER\n          default: 2\n"
                + "behavior:\n  main:\n    commands:\n      player:\n"
                + "        - say hi\n";
        Path file = tmp.resolve("fog.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                file.toString());
        assertTrue(output.contains("W_DUPLICATE_KEY"), output);
        assertTrue(output.contains(
                "Duplicate YAML key 'config' in the file root (line 9); keeping the last."),
                output);
    }

    @Test
    void unknownBehaviorWarns(@TempDir Path tmp) throws Exception {
        String yaml = "behavior:\n  main:\n    commands:\n      player:\n"
                + "        - say <runblock:nope>\n  side:\n    commands:\n"
                + "      player:\n        - say hi\n";
        Path file = tmp.resolve("fog.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                file.toString());
        assertTrue(output.contains("W_UNKNOWN_BEHAVIOR"), output);
        assertTrue(output.contains(
                "Tag <runblock> hit unknown behavior 'nope'; skipping."), output);
    }

    @Test
    void ambiguousSettingWarnsAndDropsFolder(@TempDir Path tmp) throws Exception {
        String yaml = "behavior:\n  main:\n    commands:\n      player:\n"
                + "        - say hi\nconfig:\n  settings:\n    thing:\n"
                + "      value: 1\n      def:\n        value:\n"
                + "          type: INTEGER\n          default: 1\n"
                + "        folder:\n          - buried:\n"
                + "              value: 999\n              def:\n"
                + "                value:\n"
                + "                  type: INTEGER\n"
                + "                  default: 1\n"
                + "                  allowed:\n"
                + "                    min: 0\n"
                + "                    max: 100\n";
        Path file = tmp.resolve("fog.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                file.toString());
        assertTrue(output.contains("W_AMBIGUOUS_SETTING"), output);
        assertTrue(output.contains("defines both value and folder; using the value."),
                output);
        assertFalse(output.contains("buried"), output);
    }

    @Test
    void uncalledDefinitionsWarn(@TempDir Path tmp) throws Exception {
        String yaml = "behavior:\n  main:\n    runs-on:\n      - ON_START\n"
                + "    commands:\n      player:\n        - say <setting:rate>\n"
                + "  side:\n    commands:\n      player:\n        - say hi\n"
                + "config:\n  settings:\n    rate:\n      value: 1\n"
                + "      def:\n        value:\n          type: INTEGER\n"
                + "          default: 1\n    lonely:\n      value: 2\n";
        Path file = tmp.resolve("fog.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                file.toString());
        assertTrue(output.contains("W_UNCALLED_SETTING"), output);
        assertTrue(output.contains("Setting 'lonely' is never read."), output);
        assertTrue(output.contains("W_UNCALLED_BEHAVIOR"), output);
        assertTrue(output.contains("Behavior 'side' is never called."), output);
        assertFalse(output.contains("rate' is never read"), output);
    }

    @Test
    void materialDefaultWarns(@TempDir Path tmp) throws Exception {
        String yaml = "behavior:\n  main:\n    commands:\n      player:\n"
                + "        - say hi\nconfig:\n  settings:\n    blade:\n"
                + "      value: STONE\n      def:\n        value:\n"
                + "          type: MATERIAL\n          default: NOT_A_MATERIAL\n";
        Path file = tmp.resolve("fog.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                file.toString());
        assertTrue(output.contains("W_DEFAULT_OUTSIDE_ALLOWED"), output);
    }

    @Test
    void failedFileNeverOccupiesId(@TempDir Path tmp) throws Exception {
        Path root = tmp.resolve("mods");
        Files.createDirectories(root.resolve("deep"));
        writeBehavior(root.resolve("fog.yml"), "say one");
        Files.writeString(root.resolve("deep/fog.yml"), "behavior: [oops\n",
                StandardCharsets.UTF_8);
        String output = run(1, "--registry", "src/test/resources/tag-registry.json",
                root.toString());
        assertTrue(output.contains("E_YAML_PARSE"), output);
        assertFalse(output.contains("W_DUPLICATE_ID"), output);
    }


    private static void writeBehavior(Path file, String line) throws Exception {
        Files.writeString(file, "behavior:\n  main:\n    runs-on:\n      - ON_START\n"
                + "    commands:\n      player:\n        - "
                + line + "\n", StandardCharsets.UTF_8);
    }

    /** One local modifier plus one preset plus a defaults manifest. */
    private static final class Fixture {
        private final Path mods;
        private final Path preset;
        private final Path manifest;

        Fixture(Path tmp, List<String> members) throws Exception {
            mods = tmp.resolve("mods");
            Files.createDirectories(mods);
            Files.writeString(mods.resolve("fog-of-war.yml"),
                    "behavior:\n  main:\n    runs-on:\n      - ON_START\n"
                            + "    commands:\n      player:\n        - say spooky\n",
                    StandardCharsets.UTF_8);
            preset = tmp.resolve("horror.yml");
            StringBuilder yaml = new StringBuilder("meta:\n  name: Horror\nmodifiers:\n");
            for (String member : members) {
                yaml.append("  - ").append(member).append("\n");
            }
            Files.writeString(preset, yaml.toString(), StandardCharsets.UTF_8);
            manifest = tmp.resolve("engine-defaults.json");
            EngineDefaults.write(manifest, "JManhunt", "rev-test",
                    List.of("infection", "perma-night"), List.of());
        }

        Path mods() {
            return mods;
        }

        Path preset() {
            return preset;
        }

        Path manifest() {
            return manifest;
        }
    }
}
