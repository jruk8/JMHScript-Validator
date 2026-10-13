package com.jruk8.jmhvalidator.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Core purity gate: core sources import java.* and fellow core
 * types only. A framework import here breaks the ports-and-adapters
 * split (adapters own SnakeYAML, the engine owns Bukkit/Okaeri).
 */
class ArchTest {
    @Test
    void coreImportsJavaAndCoreOnly() throws IOException {
        Path core = Paths.get("src/main/java/com/jruk8/jmhvalidator/core");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(core)) {
            for (Path file : files.filter(ArchTest::isJava).toList()) {
                checkFile(file, violations);
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    private static boolean isJava(Path file) {
        return file.toString().endsWith(".java");
    }

    private static void checkFile(Path file, List<String> violations) throws IOException {
        for (String line : Files.readAllLines(file)) {
            String stripped = line.strip();
            if (!stripped.startsWith("import ")) {
                continue;
            }
            if (!stripped.startsWith("import java.")
                    && !stripped.startsWith("import com.jruk8.jmhvalidator.core.")) {
                violations.add(file.getFileName() + ": " + stripped);
            }
        }
    }
}
