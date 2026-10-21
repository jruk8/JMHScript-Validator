package com.jruk8.jmhvalidator.adapters.files;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Defaults pins: the manifest round-trips through read and
 * write, bad manifests fail loudly, preset members extract in
 * file order, and the fetch assembles files plus manifest from
 * a stub API without touching the network.
 */
class EngineDefaultsTest {
    @Test
    void manifestRoundTrip(@TempDir Path tmp) throws Exception {
        Path manifest = tmp.resolve("engine-defaults.json");
        EngineDefaults.write(manifest, "JManhunt", "rev-abc",
                List.of("perma-night", "infection"),
                List.of(new EngineDefaults.Preset("chaos-mode",
                        List.of("random-mob-spawner", "gear-dice"))));
        EngineDefaults.LoadedDefaults loaded = EngineDefaults.read(manifest);
        assertEquals("JManhunt", loaded.engine());
        assertEquals("rev-abc", loaded.engineVersion());
        assertEquals(List.of("infection", "perma-night"),
                loaded.modifiers().ids().stream().sorted().toList());
        assertEquals(1, loaded.presets().size());
        assertEquals("chaos-mode", loaded.presets().get(0).id());
        assertEquals(List.of("random-mob-spawner", "gear-dice"),
                loaded.presets().get(0).modifiers());
    }

    @Test
    void manifestRejectsWrongFormat(@TempDir Path tmp) throws Exception {
        Path manifest = tmp.resolve("engine-defaults.json");
        Files.writeString(manifest,
                "{\"format\": 99, \"modifiers\": [], \"presets\": []}",
                StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> EngineDefaults.read(manifest));
    }

    @Test
    void manifestRejectsNonStringIds(@TempDir Path tmp) throws Exception {
        Path manifest = tmp.resolve("engine-defaults.json");
        Files.writeString(manifest,
                "{\"format\": 1, \"modifiers\": [42], \"presets\": []}",
                StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> EngineDefaults.read(manifest));
    }

    @Test
    void manifestRejectsDuplicatePresets(@TempDir Path tmp) throws Exception {
        Path manifest = tmp.resolve("engine-defaults.json");
        Files.writeString(manifest, "{\"format\": 1, \"modifiers\": [], \"presets\": ["
                + "{\"id\": \"chaos-mode\", \"modifiers\": []}, "
                + "{\"id\": \"chaos-mode\", \"modifiers\": []}]}",
                StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> EngineDefaults.read(manifest));
    }

    @Test
    void presetMembersReadInFileOrder(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("pack.yml");
        Files.writeString(file, "meta:\n  name: Pack\nmodifiers:\n  - bingo\n  - infection\n",
                StandardCharsets.UTF_8);
        ModifierYaml.ReadResult read = ModifierYaml.read(file, "pack");
        assertEquals(List.of("bingo", "infection"), read.presetMembers());
        assertTrue(read.behavior().blocks().isEmpty());
    }

    @Test
    void modifierFilesExposeNoPresetMembers(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("mod.yml");
        Files.writeString(file,
                "behavior:\n  main:\n    commands:\n      player:\n        - say ok\n",
                StandardCharsets.UTF_8);
        ModifierYaml.ReadResult read = ModifierYaml.read(file, "mod");
        assertTrue(read.presetMembers().isEmpty());
    }

    @Test
    void fetchDefaultsToPublicValidatorRepo() {
        assertEquals("jruk8/JMHScript-Validator", DefaultsFetch.DEFAULT_REPO);
        assertEquals("engine-defaults/modifiers", DefaultsFetch.MODIFIERS_DIR);
        assertEquals("engine-defaults/presets", DefaultsFetch.PRESETS_DIR);
    }

    @Test
    void fetchAssemblesFilesAndManifest(@TempDir Path tmp) throws Exception {
        Map<String, String> raw = new HashMap<>();
        raw.put("infection.yml", "behavior:\n  main:\n    commands:\n      player:\n        - say spooky\n");
        raw.put("perma-night.yml", "config:\n  settings: {}\n");
        raw.put("chaos-mode.yml",
                "meta:\n  name: Chaos\nmodifiers:\n  - infection\n  - perma-night\n");
        try (StubApi api = new StubApi(
                List.of("infection.yml", "perma-night.yml"), List.of("chaos-mode.yml"), raw)) {
            DefaultsFetch.Fetched fetched = DefaultsFetch.fetch(
                    api.base(), "OWNER/REPO", "rev-stub", tmp.resolve("defaults"));
            assertEquals(List.of("infection", "perma-night"), fetched.modifiers());
            assertEquals(1, fetched.presets().size());
            assertEquals(List.of("infection", "perma-night"),
                    fetched.presets().get(0).modifiers());
            assertTrue(Files.isRegularFile(
                    fetched.dir().resolve("modifiers/infection.yml")));
            assertTrue(Files.isRegularFile(
                    fetched.dir().resolve("presets/chaos-mode.yml")));
            EngineDefaults.LoadedDefaults loaded = EngineDefaults.read(
                    fetched.dir().resolve(EngineDefaults.MANIFEST_NAME));
            assertEquals("rev-stub", loaded.engineVersion());
            assertTrue(loaded.modifiers().isKnown("infection"));
        }
    }

    @Test
    void fetchFailsLoudlyOnMissingFile(@TempDir Path tmp) throws Exception {
        try (StubApi api = new StubApi(
                List.of("ghost.yml"), List.of(), Map.of())) {
            assertThrows(IOException.class, () -> DefaultsFetch.fetch(
                    api.base(), "OWNER/REPO", "rev-stub", tmp.resolve("defaults")));
        }
    }

    /** Local stub for the two fetch endpoints (no network). */
    private static final class StubApi implements AutoCloseable {
        private final HttpServer server;
        private final List<String> modifiers;
        private final List<String> presets;
        private final Map<String, String> raw;

        StubApi(List<String> modifiers, List<String> presets,
                Map<String, String> raw) throws IOException {
            this.modifiers = modifiers;
            this.presets = presets;
            this.raw = raw;
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                byte[] body;
                int status = 200;
                if (path.endsWith("modifiers")) {
                    body = listing(modifiers).getBytes(StandardCharsets.UTF_8);
                } else if (path.endsWith("presets")) {
                    body = listing(presets).getBytes(StandardCharsets.UTF_8);
                } else if (path.startsWith("/raw/")) {
                    String name = path.substring("/raw/".length());
                    if (raw.containsKey(name)) {
                        body = raw.get(name).getBytes(StandardCharsets.UTF_8);
                    } else {
                        status = 404;
                        body = "no such file".getBytes(StandardCharsets.UTF_8);
                    }
                } else {
                    status = 404;
                    body = "no such path".getBytes(StandardCharsets.UTF_8);
                }
                exchange.sendResponseHeaders(status, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
        }

        String base() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private String listing(List<String> names) {
            StringBuilder json = new StringBuilder("[");
            for (int index = 0; index < names.size(); index++) {
                if (index > 0) {
                    json.append(",");
                }
                json.append("{\"name\":\"").append(names.get(index))
                        .append("\",\"type\":\"file\",")
                        .append("\"download_url\":\"").append(base())
                        .append("/raw/").append(names.get(index)).append("\"}");
            }
            json.append("]");
            return json.toString();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
