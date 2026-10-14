package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.ports.KnownMaterials;
import com.jruk8.jmhvalidator.core.ports.KnownTriggers;
import com.jruk8.jmhvalidator.core.ports.TagRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Registry loader: reads the engine-exported tag registry JSON
 * (JSON is valid YAML, so no second parser). The format is
 * versioned; anything but format 1 fails loudly so an old CLI
 * never validates against a registry it cannot read.
 */
public final class TagRegistryFiles {
    /** Registry format version this CLI reads. */
    public static final int FORMAT_VERSION = 1;

    /**
     * One loaded registry: the ports plus provenance. Triggers
     * and materials stay null when the file predates their
     * arrays, and those checks stay silent then.
     */
    public record LoadedRegistry(TagRegistry registry, KnownTriggers triggers,
            KnownMaterials materials, String engine, String engineVersion) {
    }

    private TagRegistryFiles() {
    }

    /** Load and validate one registry file. */
    public static LoadedRegistry read(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        Object doc;
        try {
            doc = new Yaml().load(text);
        } catch (YAMLException failed) {
            throw new IOException("unparseable registry: " + oneLine(failed));
        }
        if (!(doc instanceof Map<?, ?> map)) {
            throw new IOException("registry must hold one mapping document");
        }
        Object format = map.get("format");
        if (!(format instanceof Number number) || number.intValue() != FORMAT_VERSION) {
            throw new IOException("registry format " + format + " unsupported (want "
                    + FORMAT_VERSION + ")");
        }
        Object tags = map.get("tags");
        if (!(tags instanceof List<?> items)) {
            throw new IOException("registry 'tags' must be a list");
        }
        List<String> names = new ArrayList<>();
        for (Object entry : items) {
            if (!(entry instanceof String name)) {
                throw new IOException("registry tags must be strings");
            }
            names.add(name);
        }
        return new LoadedRegistry(TagRegistry.of(names), readTriggers(map),
                readMaterials(map),
                stringOr(map.get("engine"), "unknown"),
                stringOr(map.get("engineVersion"), "unknown"));
    }

    private static KnownTriggers readTriggers(Map<?, ?> map) throws IOException {
        Object triggers = map.get("triggers");
        if (triggers == null) {
            return null;
        }
        if (!(triggers instanceof List<?> items)) {
            throw new IOException("registry 'triggers' must be a list");
        }
        List<String> names = new ArrayList<>();
        for (Object entry : items) {
            if (!(entry instanceof String name)) {
                throw new IOException("registry triggers must be strings");
            }
            names.add(name);
        }
        return KnownTriggers.of(names, readRemovedTriggers(map));
    }

    private static List<String> readRemovedTriggers(Map<?, ?> map) throws IOException {
        Object removed = map.get("removedTriggers");
        if (removed == null) {
            return List.of();
        }
        if (!(removed instanceof List<?> items)) {
            throw new IOException("registry 'removedTriggers' must be a list");
        }
        List<String> names = new ArrayList<>();
        for (Object entry : items) {
            if (!(entry instanceof String name)) {
                throw new IOException("registry removed triggers must be strings");
            }
            names.add(name);
        }
        return names;
    }

    private static KnownMaterials readMaterials(Map<?, ?> map) throws IOException {
        Object materials = map.get("materials");
        if (materials == null) {
            return null;
        }
        if (!(materials instanceof List<?> items)) {
            throw new IOException("registry 'materials' must be a list");
        }
        List<String> names = new ArrayList<>();
        for (Object entry : items) {
            if (!(entry instanceof String name)) {
                throw new IOException("registry materials must be strings");
            }
            names.add(name);
        }
        return KnownMaterials.of(names);
    }

    private static String stringOr(Object value, String fallback) {
        return value instanceof String text ? text : fallback;
    }

    private static String oneLine(Exception failed) {
        return String.join(" ", String.valueOf(failed.getMessage()).split("\\s+"));
    }
}
