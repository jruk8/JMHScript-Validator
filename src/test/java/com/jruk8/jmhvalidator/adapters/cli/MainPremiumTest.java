package com.jruk8.jmhvalidator.adapters.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CLI premium marker: premium files print their path plus {@code (premium)}. */
class MainPremiumTest {
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
    void premiumMarkerFollowsPremiumFiles(@TempDir Path tmp) throws Exception {
        Path registry = tmp.resolve("registry.json");
        Files.writeString(registry,
                "{\"format\": 1, \"tags\": [\"p\", \"win\", \"place\"],"
                        + " \"paidTags\": [\"win\"]}",
                StandardCharsets.UTF_8);
        Path premium = tmp.resolve("premium.yml");
        Files.writeString(premium,
                "behavior:\n  main:\n    runs-on:\n      - ON_START\n    commands:\n"
                        + "      console:\n        - \"say <win:HUNTER,done>\"\n",
                StandardCharsets.UTF_8);
        Path argPremium = tmp.resolve("arg-premium.yml");
        Files.writeString(argPremium,
                "behavior:\n  main:\n    runs-on:\n      - ON_START\n    commands:\n"
                        + "      console:\n        - \"<place:1 65 2,schematic:hut>\"\n",
                StandardCharsets.UTF_8);
        Path free = tmp.resolve("free.yml");
        Files.writeString(free,
                "behavior:\n  main:\n    runs-on:\n      - ON_START\n    commands:\n"
                        + "      console:\n        - \"say hi\"\n",
                StandardCharsets.UTF_8);

        String output = run(0, "--registry", registry.toString(), tmp.toString());

        assertTrue(output.contains("premium.yml (premium)"), output);
        assertTrue(output.contains("arg-premium.yml (premium)"), output);
        assertTrue(!output.contains("free.yml (premium)"), output);
        assertTrue(output.contains("3 files: 3 clean"), output);
    }
}
