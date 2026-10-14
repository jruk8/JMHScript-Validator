package com.jruk8.jmhvalidator.adapters.cli;

import com.jruk8.jmhvalidator.adapters.files.EngineDefaults;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        try {
            System.setOut(capture);
            System.setErr(capture);
            assertEquals(expected, Main.run(args));
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
