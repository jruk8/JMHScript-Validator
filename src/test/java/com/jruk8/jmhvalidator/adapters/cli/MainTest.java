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

    /** One local modifier plus one preset plus a defaults manifest. */
    private static final class Fixture {
        private final Path mods;
        private final Path preset;
        private final Path manifest;

        Fixture(Path tmp, List<String> members) throws Exception {
            mods = tmp.resolve("mods");
            Files.createDirectories(mods);
            Files.writeString(mods.resolve("fog-of-war.yml"),
                    "behavior:\n  main:\n    commands:\n      player:\n        - say spooky\n",
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
