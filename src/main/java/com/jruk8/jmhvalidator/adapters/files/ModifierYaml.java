package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.SettingsTree;
import com.jruk8.jmhvalidator.core.model.SettingsTree.SettingsNode;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.UnknownKeys;
import com.jruk8.jmhvalidator.core.UnknownKeys.Level;
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
 * Modifier YAML reader: parses one file into the core behavior
 * and settings models. Structural behavior problems become file
 * diagnostics (the sweep never sees them); files without a
 * behavior section (presets) read as empty behavior and expose
 * their `modifiers` member ids instead. Settings read silently:
 * a missing or misshapen `config.settings` loads as empty like
 * the engine Serdes loads it. Map order follows the file, so
 * provenance matches the engine's load order.
 */
public final class ModifierYaml {
    /**
     * One read: both models, preset members, file diagnostics,
     * and unknown-key findings (kept separate so callers order
     * them after the behavior sweeps, like the engine).
     */
    public record ReadResult(BehaviorFile behavior, SettingsTree settings,
            List<String> presetMembers, List<Diagnostic> early, List<Diagnostic> keys) {
    }

    private ModifierYaml() {
    }

    /** Read one modifier file; never throws for content reasons. */
    public static ReadResult read(Path file, String modifierId) {
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException failed) {
            return resultWith(BehaviorFile.empty(), fileDiagnostic(modifierId,
                    DiagnosticCode.E_FILE_READ, "cannot read file: " + failed.getMessage()));
        }
        Object doc;
        try {
            doc = new Yaml().load(text);
        } catch (YAMLException failed) {
            return resultWith(BehaviorFile.empty(), fileDiagnostic(modifierId,
                    DiagnosticCode.E_YAML_PARSE, "unparseable YAML: " + oneLine(failed)));
        }
        if (!(doc instanceof Map<?, ?> map)) {
            return resultWith(BehaviorFile.empty(), fileDiagnostic(modifierId,
                    DiagnosticCode.E_YAML_PARSE, "expected one mapping document"));
        }
        return readDocument(modifierId, map);
    }

    private static ReadResult readDocument(String modifierId, Map<?, ?> doc) {
        Object behavior = doc.get("behavior");
        if (behavior == null) {
            return new ReadResult(BehaviorFile.empty(), readSettings(doc),
                    presetMembers(doc), List.of(), readKeys(modifierId, doc));
        }
        if (!(behavior instanceof Map<?, ?> blocks)) {
            return new ReadResult(BehaviorFile.empty(), readSettings(doc),
                    presetMembers(doc),
                    List.of(fileDiagnostic(modifierId,
                            DiagnosticCode.E_YAML_SHAPE, "'behavior' must be a map")),
                    readKeys(modifierId, doc));
        }
        Map<String, BehaviorFile.BehaviorBlock> out = new LinkedHashMap<>();
        List<Diagnostic> early = new ArrayList<>();
        for (Map.Entry<?, ?> block : blocks.entrySet()) {
            out.put(String.valueOf(block.getKey()),
                    readBlock(modifierId, String.valueOf(block.getKey()), block.getValue(), early));
        }
        return new ReadResult(new BehaviorFile(out), readSettings(doc),
                presetMembers(doc), early, readKeys(modifierId, doc));
    }

    /**
     * Unknown-key pass over the raw document, in file order:
     * the file root, meta, config with the settings subtree,
     * then behavior blocks with their option subtrees. Levels
     * that fail their shape stay silent here for the shape
     * suite to own. Never throws.
     */
    static List<Diagnostic> readKeys(String modifierId, Map<?, ?> doc) {
        List<Diagnostic> keys = new ArrayList<>();
        keys.addAll(UnknownKeys.check(modifierId, rootLevel(doc), "", "", names(doc)));
        keys.addAll(readMetaKeys(modifierId, doc.get("meta")));
        keys.addAll(readConfigKeys(modifierId, doc.get("config")));
        Object behavior = doc.get("behavior");
        if (behavior instanceof Map<?, ?> blocks) {
            for (Map.Entry<?, ?> block : blocks.entrySet()) {
                keys.addAll(readBlockKeys(modifierId,
                        String.valueOf(block.getKey()), block.getValue()));
            }
        }
        return keys;
    }

    private static UnknownKeys.Level rootLevel(Map<?, ?> doc) {
        boolean modifierLike = doc.containsKey("behavior")
                || doc.containsKey("enabled") || doc.containsKey("config");
        boolean presetLike =
                doc.containsKey("modifiers") || doc.containsKey("presets");
        if (modifierLike && !presetLike) {
            return Level.MODIFIER_ROOT;
        }
        if (presetLike && !modifierLike) {
            return Level.PRESET_ROOT;
        }
        return Level.ANY_ROOT;
    }

    private static List<Diagnostic> readMetaKeys(String modifierId, Object raw) {
        List<Diagnostic> keys = new ArrayList<>();
        if (!(raw instanceof Map<?, ?> meta)) {
            return keys;
        }
        keys.addAll(UnknownKeys.check(modifierId, Level.MODIFIER_META, "", "", names(meta)));
        if (meta.get("visibility") instanceof Map<?, ?> visibility) {
            keys.addAll(UnknownKeys.check(modifierId, Level.VISIBILITY, "", "",
                    names(visibility)));
        }
        return keys;
    }

    private static List<Diagnostic> readConfigKeys(String modifierId, Object raw) {
        List<Diagnostic> keys = new ArrayList<>();
        if (!(raw instanceof Map<?, ?> config)) {
            return keys;
        }
        keys.addAll(UnknownKeys.check(modifierId, Level.CONFIG, "", "", names(config)));
        if (config.get("advanced") instanceof Map<?, ?> advanced) {
            keys.addAll(UnknownKeys.check(modifierId, Level.ADVANCED, "", "",
                    names(advanced)));
        }
        if (config.get("settings") instanceof Map<?, ?> settings) {
            keys.addAll(readSettingsKeys(modifierId, settings, ""));
        }
        return keys;
    }

    private static List<Diagnostic> readSettingsKeys(String modifierId,
            Map<?, ?> level, String parent) {
        List<Diagnostic> keys = new ArrayList<>();
        for (Map.Entry<?, ?> entry : level.entrySet()) {
            String path = parent.isEmpty()
                    ? String.valueOf(entry.getKey()) : parent + "." + entry.getKey();
            if (!(entry.getValue() instanceof Map<?, ?> node)) {
                continue;
            }
            keys.addAll(UnknownKeys.check(modifierId, Level.NODE, "", path, names(node)));
            if (node.get("def") instanceof Map<?, ?> def) {
                keys.addAll(readDefKeys(modifierId, path, def));
            }
        }
        return keys;
    }

    private static List<Diagnostic> readDefKeys(String modifierId, String path, Map<?, ?> def) {
        List<Diagnostic> keys = new ArrayList<>();
        keys.addAll(UnknownKeys.check(modifierId, Level.DEF, "", path, names(def)));
        if (def.get("meta") instanceof Map<?, ?> meta) {
            keys.addAll(UnknownKeys.check(modifierId, Level.SETTING_META, "", path,
                    names(meta)));
        }
        if (def.get("value") instanceof Map<?, ?> spec) {
            keys.addAll(UnknownKeys.check(modifierId, Level.SPEC, "", path, names(spec)));
        }
        if (def.get("folder") instanceof List<?> folder) {
            for (Object item : folder) {
                if (!(item instanceof Map<?, ?> entry) || entry.size() != 1) {
                    continue;
                }
                Map.Entry<?, ?> single = entry.entrySet().iterator().next();
                if (single.getValue() instanceof Map<?, ?> node) {
                    String child = path + "." + single.getKey();
                    keys.addAll(UnknownKeys.check(modifierId, Level.NODE, "", child,
                            names(node)));
                    if (node.get("def") instanceof Map<?, ?> childDef) {
                        keys.addAll(readDefKeys(modifierId, child, childDef));
                    }
                }
            }
        }
        return keys;
    }

    private static List<Diagnostic> readBlockKeys(String modifierId, String blockId, Object raw) {
        List<Diagnostic> keys = new ArrayList<>();
        if (!(raw instanceof Map<?, ?> block)) {
            return keys;
        }
        keys.addAll(UnknownKeys.check(modifierId, Level.BLOCK, blockId, "", names(block)));
        if (block.get("on-start") instanceof Map<?, ?> onStart) {
            keys.addAll(UnknownKeys.check(modifierId, Level.ON_START, blockId, "",
                    names(onStart)));
        }
        if (block.get("options") instanceof Map<?, ?> options) {
            keys.addAll(readOptionsKeys(modifierId, blockId, options));
        }
        return keys;
    }

    private static List<Diagnostic> readOptionsKeys(String modifierId, String blockId,
            Map<?, ?> options) {
        List<Diagnostic> keys = new ArrayList<>();
        keys.addAll(UnknownKeys.check(modifierId, Level.OPTIONS, blockId, "",
                names(options)));
        if (options.get("interval-settings") instanceof Map<?, ?> interval) {
            keys.addAll(UnknownKeys.check(modifierId, Level.INTERVAL_SETTINGS, blockId, "",
                    names(interval)));
        }
        if (options.get("success-chance") instanceof Map<?, ?> chance) {
            keys.addAll(UnknownKeys.check(modifierId, Level.SUCCESS_CHANCE, blockId, "",
                    names(chance)));
        }
        if (options.get("execution") instanceof Map<?, ?> execution) {
            keys.addAll(UnknownKeys.check(modifierId, Level.EXECUTION, blockId, "",
                    names(execution)));
            if (execution.get("pick-random") instanceof Map<?, ?> pick) {
                keys.addAll(UnknownKeys.check(modifierId, Level.PICK_RANDOM, blockId, "",
                        names(pick)));
            }
        }
        return keys;
    }

    private static List<String> names(Map<?, ?> map) {
        List<String> names = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            names.add(String.valueOf(entry.getKey()));
        }
        return names;
    }

    /**
     * Preset member ids: string entries of the top-level
     * `modifiers` list, in file order. Anything else (missing,
     * scalar, or non-string entries) reads as empty, like the
     * engine load path skips it. Never diagnoses.
     */
    static List<String> presetMembers(Map<?, ?> doc) {
        Object members = doc.get("modifiers");
        if (!(members instanceof List<?> items)) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (Object entry : items) {
            if (entry instanceof String id) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * Read `config.settings` into the style model. Anything but a
     * map of maps loads as empty, exactly like the engine Serdes
     * skips it: scalar or list entries vanish, and folder items
     * must be single-key maps with map values. Never diagnoses.
     */
    static SettingsTree readSettings(Map<?, ?> doc) {
        Object config = doc.get("config");
        if (!(config instanceof Map<?, ?> settingsMap)) {
            return SettingsTree.empty();
        }
        Object settings = settingsMap.get("settings");
        if (!(settings instanceof Map<?, ?> entries)) {
            return SettingsTree.empty();
        }
        List<SettingsNode> roots = new ArrayList<>();
        for (Map.Entry<?, ?> entry : entries.entrySet()) {
            SettingsNode node = buildNode(String.valueOf(entry.getKey()), entry.getValue());
            if (node != null) {
                roots.add(node);
            }
        }
        return new SettingsTree(roots);
    }

    private static SettingsNode buildNode(String name, Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        boolean valuePresent = map.containsKey("value");
        Object folderRaw = null;
        SettingsTree.SettingSpec spec = null;
        Object nodeDef = map.get("def");
        if (nodeDef instanceof Map<?, ?> defMap) {
            folderRaw = defMap.get("folder");
            if (defMap.get("value") instanceof Map<?, ?> valueMap) {
                Object type = valueMap.get("type");
                spec = new SettingsTree.SettingSpec(
                        type == null ? null : String.valueOf(type),
                        valueMap.get("default"), valueMap.get("allowed"),
                        valuePresent ? map.get("value") : null);
            }
        }
        boolean isFolder = !valuePresent && folderRaw instanceof List<?>;
        List<SettingsNode> children =
                isFolder ? buildChildren((List<?>) folderRaw) : List.of();
        return new SettingsNode(name, isFolder, children, isFolder ? null : spec);
    }

    private static List<SettingsNode> buildChildren(List<?> folder) {
        List<SettingsNode> children = new ArrayList<>();
        for (Object item : folder) {
            if (!(item instanceof Map<?, ?> entry) || entry.size() != 1) {
                continue;
            }
            Map.Entry<?, ?> single = entry.entrySet().iterator().next();
            if (!(single.getValue() instanceof Map<?, ?>)) {
                continue;
            }
            SettingsNode node =
                    buildNode(String.valueOf(single.getKey()), single.getValue());
            if (node != null) {
                children.add(node);
            }
        }
        return children;
    }

    private static BehaviorFile.BehaviorBlock readBlock(String modifierId,
            String blockId, Object raw, List<Diagnostic> early) {
        Map<String, List<String>> lists = new LinkedHashMap<>();
        if (!(raw instanceof Map<?, ?> block)) {
            early.add(shaped(modifierId, blockId,
                    "'behavior." + blockId + "' must be a map"));
            return new BehaviorFile.BehaviorBlock(lists, List.of());
        }
        List<String> runsOn = readRunsOn(modifierId, blockId, block, early);
        Object commands = block.get("commands");
        if (commands == null) {
            return new BehaviorFile.BehaviorBlock(lists, runsOn);
        }
        if (!(commands instanceof Map<?, ?> groups)) {
            early.add(shaped(modifierId, blockId,
                    "'behavior." + blockId + ".commands' must be a map"));
            return new BehaviorFile.BehaviorBlock(lists, runsOn);
        }
        for (Map.Entry<?, ?> list : groups.entrySet()) {
            lists.put(String.valueOf(list.getKey()),
                    readLines(modifierId, blockId, String.valueOf(list.getKey()),
                            list.getValue(), early));
        }
        return new BehaviorFile.BehaviorBlock(lists, runsOn);
    }

    /**
     * Read one block's {@code runs-on} trigger names. A missing
     * list reads empty (called-only blocks omit it); a
     * non-list shape is a file diagnostic, mirroring the engine
     * load failure. Non-string entries stay silent for the
     * broader shape suite to own.
     */
    private static List<String> readRunsOn(String modifierId, String blockId,
            Map<?, ?> block, List<Diagnostic> early) {
        Object runsOn = block.get("runs-on");
        if (runsOn == null) {
            return List.of();
        }
        if (!(runsOn instanceof List<?> items)) {
            early.add(shaped(modifierId, blockId,
                    "'behavior." + blockId + ".runs-on' must be a list"));
            return List.of();
        }
        List<String> triggers = new ArrayList<>();
        for (Object entry : items) {
            if (entry instanceof String trigger) {
                triggers.add(trigger);
            }
        }
        return triggers;
    }

    private static List<String> readLines(String modifierId, String blockId,
            String listName, Object raw, List<Diagnostic> early) {
        List<String> lines = new ArrayList<>();
        if (!(raw instanceof List<?> items)) {
            early.add(shaped(modifierId, blockId,
                    "'behavior." + blockId + ".commands." + listName + "' must be a list"));
            return lines;
        }
        for (int index = 0; index < items.size(); index++) {
            Object entry = items.get(index);
            if (entry instanceof String line) {
                lines.add(line);
            } else {
                lines.add(null);
                early.add(new Diagnostic(
                        new Provenance(modifierId, blockId, listName, index), -1,
                        Severity.ERROR, DiagnosticCode.E_YAML_LINE_TYPE,
                        "command line must be a string"));
            }
        }
        return lines;
    }

    private static Diagnostic shaped(String modifierId, String blockId, String message) {
        return new Diagnostic(new Provenance(modifierId, blockId, "", -1), -1,
                Severity.ERROR, DiagnosticCode.E_YAML_SHAPE, message);
    }

    private static Diagnostic fileDiagnostic(String modifierId,
            DiagnosticCode code, String message) {
        return new Diagnostic(new Provenance(modifierId, "", "", -1), -1,
                Severity.ERROR, code, message);
    }

    private static ReadResult resultWith(BehaviorFile behavior, Diagnostic early) {
        return new ReadResult(behavior, SettingsTree.empty(), List.of(), List.of(early),
                List.of());
    }

    private static String oneLine(Exception failed) {
        return String.join(" ", String.valueOf(failed.getMessage()).split("\\s+"));
    }
}
