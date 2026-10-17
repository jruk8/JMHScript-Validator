package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * File adapter pins: unreadable, unparseable, and misshapen
 * inputs become file diagnostics (never exceptions), presets
 * read as empty, and bad registries fail loudly.
 */
class FilesAdapterTest {
    @Test
    void missingFileIsFileDiagnostic(@TempDir Path tmp) {
        ModifierYaml.ReadResult read =
                ModifierYaml.read(tmp.resolve("absent.yml"), "absent");
        assertEquals(1, read.early().size());
        assertEquals(DiagnosticCode.E_FILE_READ, read.early().get(0).code());
        assertTrue(read.behavior().blocks().isEmpty());
    }

    @Test
    void brokenYamlIsParseDiagnostic(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("bad.yml");
        Files.writeString(file, "key: [unclosed\n  bad indent:\n x", StandardCharsets.UTF_8);
        ModifierYaml.ReadResult read = ModifierYaml.read(file, "bad");
        assertEquals(DiagnosticCode.E_YAML_PARSE, read.early().get(0).code());
    }

    @Test
    void presetReadsEmpty(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("pack.yml");
        Files.writeString(file, "meta:\n  name: Pack\nmodifiers:\n  - bingo\n",
                StandardCharsets.UTF_8);
        ModifierYaml.ReadResult read = ModifierYaml.read(file, "pack");
        assertTrue(read.early().isEmpty());
        assertTrue(read.behavior().blocks().isEmpty());
    }

    @Test
    void scalarLineStringifiesWithWarning(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("mod.yml");
        Files.writeString(file,
                "behavior:\n  main:\n    commands:\n      player:\n        - say ok\n        - 42\n",
                StandardCharsets.UTF_8);
        ModifierYaml.ReadResult read = ModifierYaml.read(file, "mod");
        assertTrue(read.early().isEmpty());
        assertEquals(1, read.shapes().size());
        assertEquals(DiagnosticCode.W_SHAPE, read.shapes().get(0).code());
        assertEquals(1, read.shapes().get(0).provenance().lineIndex());
        assertEquals(List.of("say ok", "42"),
                read.behavior().blocks().get("main").lists().get("player"));
    }

    @Test
    void mapLineKeepsErrorAndNullSlot(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("mod.yml");
        Files.writeString(file,
                "behavior:\n  main:\n    commands:\n      player:\n        - say ok\n        - {a: b}\n",
                StandardCharsets.UTF_8);
        ModifierYaml.ReadResult read = ModifierYaml.read(file, "mod");
        assertEquals(1, read.early().size());
        assertEquals(DiagnosticCode.E_YAML_LINE_TYPE, read.early().get(0).code());
        assertEquals(1, read.early().get(0).provenance().lineIndex());
        assertEquals(java.util.Arrays.asList("say ok", null),
                read.behavior().blocks().get("main").lists().get("player"));
    }

    @Test
    void misshapenBlocksAreShapeDiagnostics(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("mod.yml");
        Files.writeString(file, "behavior:\n  main: 42\n", StandardCharsets.UTF_8);
        ModifierYaml.ReadResult read = ModifierYaml.read(file, "mod");
        assertEquals(DiagnosticCode.E_YAML_SHAPE, read.early().get(0).code());
    }

    @Test
    void runsOnReadsTriggerNames(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("mod.yml");
        Files.writeString(file,
                "behavior:\n  main:\n    runs-on:\n      - ON_START\n      - INTERVAL\n",
                StandardCharsets.UTF_8);
        ModifierYaml.ReadResult read = ModifierYaml.read(file, "mod");
        assertTrue(read.early().isEmpty());
        assertEquals(List.of("ON_START", "INTERVAL"),
                read.behavior().blocks().get("main").runsOn());
    }

    @Test
    void scalarRunsOnIsShapeDiagnostic(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("mod.yml");
        Files.writeString(file,
                "behavior:\n  main:\n    runs-on: ON_START\n",
                StandardCharsets.UTF_8);
        ModifierYaml.ReadResult read = ModifierYaml.read(file, "mod");
        assertEquals(1, read.early().size());
        assertEquals(DiagnosticCode.E_YAML_SHAPE, read.early().get(0).code());
        assertEquals("'behavior.main.runs-on' must be a list",
                read.early().get(0).message());
    }

    @Test
    void typoKeysFlaggedAtRootAndBlock(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("mod.yml");
        Files.writeString(file,
                "conflg:\n  settings: {}\nbehavior:\n  main:\n    runs-onn:\n      - ON_START\n",
                StandardCharsets.UTF_8);
        ModifierYaml.ReadResult read = ModifierYaml.read(file, "mod");
        assertEquals(2, read.keys().size());
        assertEquals(DiagnosticCode.W_UNKNOWN_KEY, read.keys().get(0).code());
        assertEquals("Unknown key 'conflg' in the file root; ignoring it.",
                read.keys().get(0).message());
        assertEquals("Unknown key 'runs-onn' in behavior 'main'; ignoring it.",
                read.keys().get(1).message());
    }

    @Test
    void registryWithoutTriggersReadsNull(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("registry.json");
        Files.writeString(file, "{\"format\": 1, \"tags\": [\"p\"]}", StandardCharsets.UTF_8);
        TagRegistryFiles.LoadedRegistry loaded = TagRegistryFiles.read(file);
        assertEquals(null, loaded.triggers());
        assertEquals(null, loaded.interceptable());
        assertTrue(loaded.registry().isKnown("p"));
    }

    @Test
    void registryWithInterceptableReadsSet(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("registry.json");
        Files.writeString(file,
                "{\"format\": 1, \"tags\": [\"p\"], \"interceptable\": [\"ON_MOB_KILLED\"]}",
                StandardCharsets.UTF_8);
        TagRegistryFiles.LoadedRegistry loaded = TagRegistryFiles.read(file);
        assertTrue(loaded.interceptable().isInterceptable("on_mob_killed"));
        assertTrue(!loaded.interceptable().isInterceptable("INTERVAL"));
    }

    @Test
    void registryRejectsWrongFormat(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("registry.json");
        Files.writeString(file, "{\"format\": 99, \"tags\": []}", StandardCharsets.UTF_8);
        assertThrows(Exception.class, () -> TagRegistryFiles.read(file));
    }

    @Test
    void registryRejectsNonStringTags(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("registry.json");
        Files.writeString(file, "{\"format\": 1, \"tags\": [42]}", StandardCharsets.UTF_8);
        assertThrows(Exception.class, () -> TagRegistryFiles.read(file));
    }

    @Test
    void registryReadsPaidTags(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("registry.json");
        Files.writeString(file,
                "{\"format\": 1, \"tags\": [\"p\", \"win\"], \"paidTags\": [\"win\"]}",
                StandardCharsets.UTF_8);
        TagRegistryFiles.LoadedRegistry loaded = TagRegistryFiles.read(file);
        assertTrue(loaded.registry().isPaid("win"));
        assertTrue(!loaded.registry().isPaid("p"));
    }

    @Test
    void registryWithoutPaidTagsReadsEmpty(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("registry.json");
        Files.writeString(file, "{\"format\": 1, \"tags\": [\"p\"]}", StandardCharsets.UTF_8);
        TagRegistryFiles.LoadedRegistry loaded = TagRegistryFiles.read(file);
        assertTrue(loaded.registry().paidTags().isEmpty());
        assertTrue(!loaded.registry().isPaid("p"));
    }

    @Test
    void registryRejectsNonStringPaidTags(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("registry.json");
        Files.writeString(file,
                "{\"format\": 1, \"tags\": [\"p\"], \"paidTags\": [42]}", StandardCharsets.UTF_8);
        assertThrows(Exception.class, () -> TagRegistryFiles.read(file));
    }
}
