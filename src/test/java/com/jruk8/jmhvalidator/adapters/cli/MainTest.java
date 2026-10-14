package com.jruk8.jmhvalidator.adapters.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
