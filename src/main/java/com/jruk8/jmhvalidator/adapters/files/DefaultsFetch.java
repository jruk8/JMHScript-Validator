package com.jruk8.jmhvalidator.adapters.files;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Engine defaults fetch: downloads the bundled JManhunt default
 * modifier and preset files at one engine rev, plus a manifest
 * listing their ids. Sources mirror the engine resources the
 * {@code ModsDefaults} unit test pins, so the fetched set and
 * the engine seed set cannot drift silently: a preset member
 * missing from the fetched modifiers fails the preset check
 * downstream. Layout under the output dir is {@code
 * modifiers/*.yml}, {@code presets/*.yml}, and {@code
 * engine-defaults.json}. Any transport or shape problem fails
 * loudly; nothing is cached or retried.
 */
public final class DefaultsFetch {
    /** GitHub API base used unless a test overrides it. */
    public static final String GITHUB_API = "https://api.github.com";

    /** Default engine repository. */
    public static final String DEFAULT_REPO = "jruk8/JManhunt";

    /** Engine resource dir holding the default modifiers. */
    public static final String MODIFIERS_DIR = "src/main/resources/mods/modifiers";

    /** Engine resource dir holding the bundled presets. */
    public static final String PRESETS_DIR = "src/main/resources/mods/presets";

    /** One fetched tree: its dir plus the manifested ids. */
    public record Fetched(Path dir, List<String> modifiers,
            List<EngineDefaults.Preset> presets) {
    }

    private DefaultsFetch() {
    }

    /** Fetch from the GitHub API at one engine rev. */
    public static Fetched fetch(String repo, String rev, Path out) throws IOException {
        return fetch(GITHUB_API, repo, rev, out);
    }

    /** Fetch from one API base (tests point this at a stub). */
    static Fetched fetch(String apiBase, String repo, String rev, Path out)
            throws IOException {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30)).build();
        Path modifiersDir = createDir(out.resolve("modifiers"));
        Path presetsDir = createDir(out.resolve("presets"));
        List<String> modifiers = fetchKind(http, apiBase, repo, rev,
                MODIFIERS_DIR, modifiersDir, false);
        List<String> presetIds = fetchKind(http, apiBase, repo, rev,
                PRESETS_DIR, presetsDir, true);
        List<EngineDefaults.Preset> presets = readPresets(presetsDir, presetIds);
        EngineDefaults.write(out.resolve(EngineDefaults.MANIFEST_NAME),
                "JManhunt", rev, modifiers, presets);
        return new Fetched(out, modifiers, presets);
    }

    private static List<String> fetchKind(HttpClient http, String apiBase,
            String repo, String rev, String resourceDir, Path targetDir,
            boolean isPreset) throws IOException {
        List<FileEntry> entries = listDir(http, apiBase, repo, rev, resourceDir);
        List<String> ids = new ArrayList<>();
        for (FileEntry entry : entries) {
            String text = download(http, entry.url(), entry.name());
            Files.writeString(targetDir.resolve(entry.name()), text,
                    StandardCharsets.UTF_8);
            String stem = entry.name().substring(0, entry.name().length() - 4);
            ids.add(stem);
        }
        if (!isPreset) {
            ids.sort(String::compareTo);
        }
        return ids;
    }

    private static List<EngineDefaults.Preset> readPresets(Path presetsDir,
            List<String> presetIds) throws IOException {
        List<EngineDefaults.Preset> presets = new ArrayList<>();
        Yaml yaml = new Yaml();
        for (String id : presetIds) {
            String text = Files.readString(presetsDir.resolve(id + ".yml"),
                    StandardCharsets.UTF_8);
            Object doc;
            try {
                doc = yaml.load(text);
            } catch (YAMLException failed) {
                throw new IOException("unparseable preset '" + id + "': "
                        + oneLine(failed));
            }
            if (!(doc instanceof Map<?, ?> map)) {
                throw new IOException("preset '" + id + "' must hold one mapping document");
            }
            presets.add(new EngineDefaults.Preset(id, ModifierYaml.presetMembers(map)));
        }
        return presets;
    }

    private static List<FileEntry> listDir(HttpClient http, String apiBase,
            String repo, String rev, String resourceDir) throws IOException {
        String url = apiBase + "/repos/" + repo + "/contents/" + resourceDir
                + "?ref=" + rev;
        String body = get(http, url);
        Object doc;
        try {
            doc = new Yaml().load(body);
        } catch (YAMLException failed) {
            throw new IOException("unparseable listing for " + resourceDir + ": "
                    + oneLine(failed));
        }
        if (!(doc instanceof List<?> items)) {
            throw new IOException("listing for " + resourceDir + " must be a list");
        }
        List<FileEntry> entries = new ArrayList<>();
        for (Object item : items) {
            FileEntry entry = fileEntry(item);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static FileEntry fileEntry(Object item) throws IOException {
        if (!(item instanceof Map<?, ?> map)) {
            throw new IOException("listing entry must be a mapping");
        }
        if (!"file".equals(map.get("type"))) {
            return null;
        }
        Object name = map.get("name");
        Object url = map.get("download_url");
        if (!(name instanceof String file) || !(url instanceof String link)) {
            throw new IOException("listing entry needs a name and a download url");
        }
        if (!file.toLowerCase().endsWith(".yml")) {
            return null;
        }
        return new FileEntry(file, link);
    }

    private static String download(HttpClient http, String url, String name)
            throws IOException {
        return get(http, url, "file '" + name + "'");
    }

    private static String get(HttpClient http, String url) throws IOException {
        return get(http, url, url);
    }

    private static String get(HttpClient http, String url, String what) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/vnd.github+json").build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted fetching " + what);
        } catch (IllegalArgumentException failed) {
            throw new IOException("bad url for " + what + ": " + failed.getMessage());
        } catch (IOException failed) {
            throw new IOException("cannot fetch " + what + ": " + failed.getMessage());
        }
        if (response.statusCode() != 200) {
            throw new IOException("cannot fetch " + what + ": HTTP "
                    + response.statusCode());
        }
        return response.body();
    }

    private static Path createDir(Path dir) throws IOException {
        return Files.createDirectories(dir);
    }

    private static String oneLine(Exception failed) {
        return String.join(" ", String.valueOf(failed.getMessage()).split("\\s+"));
    }

    private record FileEntry(String name, String url) {
    }
}
