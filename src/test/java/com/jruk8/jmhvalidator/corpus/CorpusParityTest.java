package com.jruk8.jmhvalidator.corpus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.jruk8.jmhvalidator.adapters.cli.ReportJson;
import com.jruk8.jmhvalidator.adapters.files.ModifierYaml;
import com.jruk8.jmhvalidator.adapters.files.TagRegistryFiles;
import com.jruk8.jmhvalidator.core.JmhValidator;
import com.jruk8.jmhvalidator.core.SettingsStyle;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Parity gate: replays every corpus input through the moved core
 * and diffs against goldens generated from unmodified engine
 * code. A mismatch is a fidelity bug. Only validatorVersion is
 * excluded (informational); codes, messages, locations, and the
 * registry engine version must all match.
 */
class CorpusParityTest {
    private static final Path INPUTS = Paths.get("corpus/inputs");
    private static final Path GOLDENS = Paths.get("corpus/goldens");

    @Test
    void movedCoreReproducesGoldens() throws Exception {
        TagRegistryFiles.LoadedRegistry registry = TagRegistryFiles.read(
                Paths.get("src/test/resources/tag-registry.json"));
        List<Path> inputs = inputs();
        assertFalse(inputs.isEmpty(), "corpus inputs present");
        for (Path input : inputs) {
            checkOne(input, registry);
        }
    }

    private List<Path> inputs() throws Exception {
        List<Path> found = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(INPUTS)) {
            for (Path path : walk.filter(Files::isRegularFile).sorted().toList()) {
                if (path.toString().endsWith(".yml")) {
                    found.add(path);
                }
            }
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    private void checkOne(Path input, TagRegistryFiles.LoadedRegistry registry)
            throws Exception {
        String label = labelOf(input);
        String stem = input.getFileName().toString().replace(".yml", "");
        ModifierYaml.ReadResult read = ModifierYaml.read(input, stem);
        List<Diagnostic> found = new ArrayList<>(read.early());
        found.addAll(JmhValidator.validateBehavior(stem, read.behavior(), registry.registry()));
        found.addAll(SettingsStyle.check(stem, read.settings()));
        String actual = ReportJson.report("test", registry.engine(),
                registry.engineVersion(),
                List.of(new ReportJson.FileReport(label + ".yml", found)));
        Path golden = GOLDENS.resolve(label + ".json");
        Yaml yaml = new Yaml();
        Map<String, Object> want =
                (Map<String, Object>) yaml.load(Files.readString(golden, StandardCharsets.UTF_8));
        Map<String, Object> got = (Map<String, Object>) yaml.load(actual);
        want.remove("validatorVersion");
        got.remove("validatorVersion");
        assertEquals(want, got, "golden " + label);
    }

    private String labelOf(Path input) {
        String text = INPUTS.relativize(input).toString().replace('\\', '/');
        return text.substring(0, text.length() - ".yml".length());
    }
}
