package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.ports.KnownModifiers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Engine defaults manifest: the fetched JManhunt default modifier
 * and preset ids (JSON is valid YAML, so no second parser). The
 * format is versioned; anything but format 1 fails loudly so an
 * old CLI never validates against defaults it cannot read.
 * The manifest name is {@code engine-defaults.json}.
 */
public final class EngineDefaults {
    /** Defaults format version this CLI reads and writes. */
    public static final int FORMAT_VERSION = 1;

    /** Manifest file name inside a fetched defaults directory. */
    public static final String MANIFEST_NAME = "engine-defaults.json";

    /** One bundled preset: its id plus its member modifier ids. */
    public record Preset(String id, List<String> modifiers) {
    }

    /** One loaded manifest: the port plus presets and provenance. */
    public record LoadedDefaults(KnownModifiers modifiers, List<Preset> presets,
            String engine, String engineVersion) {
    }

    private EngineDefaults() {
    }

    /** Load and validate one defaults manifest file. */
    public static LoadedDefaults read(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        Object doc;
        try {
            doc = new Yaml().load(text);
        } catch (YAMLException failed) {
            throw new IOException("unparseable defaults: " + oneLine(failed));
        }
        if (!(doc instanceof Map<?, ?> map)) {
            throw new IOException("defaults must hold one mapping document");
        }
        Object format = map.get("format");
        if (!(format instanceof Number number) || number.intValue() != FORMAT_VERSION) {
            throw new IOException("defaults format " + format + " unsupported (want "
                    + FORMAT_VERSION + ")");
        }
        return new LoadedDefaults(KnownModifiers.of(stringList(map.get("modifiers"), "modifiers")),
                readPresets(map.get("presets")), stringOr(map.get("engine"), "unknown"),
                stringOr(map.get("engineVersion"), "unknown"));
    }

    /**
     * Write one defaults manifest. Ids are sorted for stable
     * output; preset members keep file order like the engine
     * load order. Overwrites when present.
     */
    public static void write(Path file, String engine, String engineVersion,
            List<String> modifierIds, List<Preset> presets) throws IOException {
        List<String> modifiers = new ArrayList<>(modifierIds);
        modifiers.sort(String::compareTo);
        List<Preset> ordered = new ArrayList<>(presets);
        ordered.sort((left, right) -> left.id().compareTo(right.id()));
        StringBuilder json = new StringBuilder("{\n");
        json.append("  \"format\": ").append(FORMAT_VERSION).append(",\n");
        json.append("  \"engine\": \"").append(engine).append("\",\n");
        json.append("  \"engineVersion\": \"").append(engineVersion).append("\",\n");
        json.append("  \"modifiers\": [");
        if (modifiers.isEmpty()) {
            json.append("],\n");
        } else {
            json.append("\n");
            appendStrings(json, modifiers);
            json.append("  ],\n");
        }
        json.append("  \"presets\": [");
        if (ordered.isEmpty()) {
            json.append("]\n");
        } else {
            json.append("\n");
            for (int index = 0; index < ordered.size(); index++) {
                Preset preset = ordered.get(index);
                json.append("    {\"id\": \"").append(preset.id())
                        .append("\", \"modifiers\": [");
                appendInlineStrings(json, preset.modifiers());
                json.append("]}");
                json.append(index + 1 < ordered.size() ? ",\n" : "\n");
            }
            json.append("  ]\n");
        }
        json.append("}\n");
        Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
    }

    private static List<Preset> readPresets(Object raw) throws IOException {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> items)) {
            throw new IOException("defaults 'presets' must be a list");
        }
        List<Preset> presets = new ArrayList<>();
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (Object entry : items) {
            if (!(entry instanceof Map<?, ?> preset)) {
                throw new IOException("defaults presets must hold mappings");
            }
            Object id = preset.get("id");
            if (!(id instanceof String name)) {
                throw new IOException("defaults preset 'id' must be a string");
            }
            if (seen.put(name, Boolean.TRUE) != null) {
                throw new IOException("defaults preset '" + name + "' listed twice");
            }
            presets.add(new Preset(name, stringList(preset.get("modifiers"), "presets")));
        }
        return presets;
    }

    private static List<String> stringList(Object raw, String field) throws IOException {
        if (!(raw instanceof List<?> items)) {
            throw new IOException("defaults '" + field + "' must be a list");
        }
        List<String> ids = new ArrayList<>();
        for (Object entry : items) {
            if (!(entry instanceof String id)) {
                throw new IOException("defaults '" + field + "' must hold strings");
            }
            ids.add(id);
        }
        return ids;
    }

    private static void appendStrings(StringBuilder json, List<String> ids) {
        for (int index = 0; index < ids.size(); index++) {
            json.append("    \"").append(ids.get(index)).append("\"");
            json.append(index + 1 < ids.size() ? ",\n" : "\n");
        }
    }

    private static void appendInlineStrings(StringBuilder json, List<String> ids) {
        for (int index = 0; index < ids.size(); index++) {
            json.append("\"").append(ids.get(index)).append("\"");
            if (index + 1 < ids.size()) {
                json.append(", ");
            }
        }
    }

    private static String stringOr(Object value, String fallback) {
        return value instanceof String text ? text : fallback;
    }

    private static String oneLine(Exception failed) {
        return String.join(" ", String.valueOf(failed.getMessage()).split("\\s+"));
    }
}
