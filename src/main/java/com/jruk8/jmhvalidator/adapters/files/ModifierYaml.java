package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.OptionsNode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.SettingsTree;
import com.jruk8.jmhvalidator.core.model.SettingsTree.SettingsNode;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.DupKeys;
import com.jruk8.jmhvalidator.core.ShapeMessages;
import com.jruk8.jmhvalidator.core.TriggerParams;
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
     * unknown-key findings, and tolerated-shape warnings. Keys
     * and shapes stay separate so callers order them after the
     * behavior sweeps, like the engine.
     */
    public record ReadResult(BehaviorFile behavior, SettingsTree settings,
            List<String> presetMembers, List<Diagnostic> early, List<Diagnostic> keys,
            List<Diagnostic> shapes) {
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
        ReadResult inner = readDocument(modifierId, map);
        List<Diagnostic> keys = new ArrayList<>(
                DupKeys.diagnostics(modifierId, DupKeyScan.scan(text)));
        keys.addAll(inner.keys());
        return new ReadResult(inner.behavior(), inner.settings(), inner.presetMembers(),
                inner.early(), keys, inner.shapes());
    }

    private static ReadResult readDocument(String modifierId, Map<?, ?> doc) {
        List<Diagnostic> early = new ArrayList<>(readRootShapes(modifierId, doc));
        List<Diagnostic> shapes = new ArrayList<>();
        readMetaShapes(modifierId, doc, early);
        readSettingsShapes(modifierId, doc, shapes);
        scanNestedMembers(modifierId, doc, early, shapes);
        Object behavior = doc.get("behavior");
        if (behavior == null) {
            return new ReadResult(BehaviorFile.empty(), readSettings(doc),
                    presetMembers(modifierId, doc, early, shapes), early,
                    readKeys(modifierId, doc), shapes);
        }
        if (!(behavior instanceof Map<?, ?> blocks)) {
            early.add(fileDiagnostic(modifierId,
                    DiagnosticCode.E_YAML_SHAPE, "'behavior' must be a map"));
            return new ReadResult(BehaviorFile.empty(), readSettings(doc),
                    presetMembers(modifierId, doc, early, shapes), early,
                    readKeys(modifierId, doc), shapes);
        }
        Map<String, BehaviorFile.BehaviorBlock> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> block : blocks.entrySet()) {
            BehaviorFile.BehaviorBlock read = readBlock(modifierId,
                    String.valueOf(block.getKey()), block.getValue(), early, shapes);
            if (read != null) {
                out.put(String.valueOf(block.getKey()), read);
            }
        }
        return new ReadResult(new BehaviorFile(out), readSettings(doc),
                presetMembers(modifierId, doc, early, shapes), early,
                readKeys(modifierId, doc), shapes);
    }

    /**
     * Root shape gates, mirroring the engine kind and section
     * checks with their texts. Signals count only when
     * non-null: the engine YAML layer drops null values, so
     * null keys are absent there. Kind-gated like the engine:
     * modifier files check the modifier root, preset files
     * the preset root, both-signals files fail ambiguous, and
     * signal-less files check only the shared meta shape.
     */
    private static List<Diagnostic> readRootShapes(String modifierId, Map<?, ?> doc) {
        boolean modifierLike = doc.get("behavior") != null
                || doc.get("enabled") != null || doc.get("config") != null;
        boolean presetLike =
                doc.get("modifiers") != null || doc.get("presets") != null;
        if (modifierLike && presetLike) {
            return List.of(fileDiagnostic(modifierId, DiagnosticCode.E_YAML_SHAPE,
                    "ambiguous file: has both modifier and preset content"));
        }
        List<Diagnostic> early = new ArrayList<>();
        if (doc.get("meta") != null && !(doc.get("meta") instanceof Map)) {
            early.add(fileDiagnostic(modifierId, DiagnosticCode.E_YAML_SHAPE,
                    "meta must be a mapping"));
        }
        if (modifierLike) {
            Object enabled = doc.get("enabled");
            if (enabled != null && !isEnabledBoolean(enabled)) {
                early.add(fileDiagnostic(modifierId, DiagnosticCode.E_YAML_SHAPE,
                        "enabled must be true or false"));
            }
            if (doc.get("config") != null && !(doc.get("config") instanceof Map)) {
                early.add(fileDiagnostic(modifierId, DiagnosticCode.E_YAML_SHAPE,
                        "config must be a mapping"));
            }
        } else if (presetLike) {
            if (doc.get("modifiers") != null && !(doc.get("modifiers") instanceof List)) {
                early.add(fileDiagnostic(modifierId, DiagnosticCode.E_YAML_SHAPE,
                        "modifiers must be a list of ids"));
            }
            if (doc.get("presets") != null && !(doc.get("presets") instanceof List)) {
                early.add(fileDiagnostic(modifierId, DiagnosticCode.E_YAML_SHAPE,
                        "presets must be a list of ids"));
            }
        }
        return early;
    }

    /**
     * True for values the engine reads as booleans: real
     * booleans plus the YAML 1.1 spellings its parser folds.
     * Quoted true and false stay strings there and fail, so
     * only the y/n/on/off spellings join real booleans here.
     */
    private static boolean isEnabledBoolean(Object raw) {
        if (raw instanceof Boolean) {
            return true;
        }
        if (!(raw instanceof String text)) {
            return false;
        }
        return switch (text) {
            case "y", "Y", "yes", "Yes", "YES", "n", "N", "no", "No", "NO",
                    "on", "On", "ON", "off", "Off", "OFF" -> true;
            default -> false;
        };
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
    static List<String> presetMembers(String modifierId, Map<?, ?> doc,
            List<Diagnostic> early, List<Diagnostic> shapes) {
        Object members = doc.get("modifiers");
        if (!(members instanceof List<?> items)) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            Object entry = items.get(index);
            if (entry instanceof String id) {
                ids.add(id);
            } else if (entry == null) {
                shapes.add(ShapeMessages.nullMember(
                        modifierId, "modifiers", index, "modifier"));
            } else if (entry instanceof Map || entry instanceof List) {
                early.add(shapedAt(modifierId, "", "modifiers", index,
                        "preset member must be a string"));
            } else {
                ids.add(String.valueOf(entry));
            }
        }
        return ids;
    }

    /**
     * Shape-only scan of nested preset refs: member shapes
     * fail the load like modifier members, but ref targets
     * stay unchecked here. Scalars drop silently (refs are a
     * later check); nulls warn; maps and lists error.
     */
    private static void scanNestedMembers(String modifierId, Map<?, ?> doc,
            List<Diagnostic> early, List<Diagnostic> shapes) {
        if (!(doc.get("presets") instanceof List<?> items)) {
            return;
        }
        for (int index = 0; index < items.size(); index++) {
            Object entry = items.get(index);
            if (entry == null) {
                shapes.add(ShapeMessages.nullMember(
                        modifierId, "presets", index, "preset"));
            } else if (entry instanceof Map || entry instanceof List) {
                early.add(shapedAt(modifierId, "", "presets", index,
                        "preset member must be a string"));
            }
        }
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
        String icon = null;
        Object nodeDef = map.get("def");
        if (nodeDef instanceof Map<?, ?> defMap) {
            folderRaw = defMap.get("folder");
            if (defMap.get("value") instanceof Map<?, ?> valueMap) {
                Object type = valueMap.get("type");
                spec = new SettingsTree.SettingSpec(
                        type == null ? null : String.valueOf(type),
                        valueMap.get("default"), valueMap.get("allowed"),
                        valuePresent ? map.get("value") : null, valuePresent);
            }
            if (defMap.get("meta") instanceof Map<?, ?> metaMap) {
                Object rawIcon = metaMap.get("icon");
                icon = rawIcon == null ? null : String.valueOf(rawIcon);
            }
        }
        boolean isFolder = !valuePresent && folderRaw instanceof List<?>;
        boolean ambiguous = valuePresent && folderRaw instanceof List<?>;
        List<SettingsNode> children =
                isFolder ? buildChildren((List<?>) folderRaw) : List.of();
        return new SettingsNode(name, isFolder, children, isFolder ? null : spec,
                ambiguous, icon);
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
            String blockId, Object raw, List<Diagnostic> early, List<Diagnostic> shapes) {
        if (raw == null) {
            shapes.add(ShapeMessages.nullBlock(modifierId, blockId));
            return null;
        }
        Map<String, List<String>> lists = new LinkedHashMap<>();
        if (!(raw instanceof Map<?, ?> block)) {
            early.add(shaped(modifierId, blockId,
                    "'behavior." + blockId + "' must be a map"));
            return new BehaviorFile.BehaviorBlock(lists, List.of());
        }
        List<String> runsOn = readRunsOn(modifierId, blockId, block, early, shapes);
        OptionsNode options = readOptions(block);
        readOptionShapes(modifierId, blockId, block, early);
        readCommands(modifierId, blockId, block.get("commands"), lists, early, shapes);
        return new BehaviorFile.BehaviorBlock(lists, runsOn, options);
    }

    /**
     * Read one block's commands into the lists model. Null
     * stays absent; a map reads list by list with skipped
     * shapes warned; a list becomes the loader's synthetic
     * single list; anything else is ignored with a warning.
     */
    private static void readCommands(String modifierId, String blockId, Object commands,
            Map<String, List<String>> lists, List<Diagnostic> early,
            List<Diagnostic> shapes) {
        if (commands == null) {
            return;
        }
        if (commands instanceof Map<?, ?> groups) {
            for (Map.Entry<?, ?> list : groups.entrySet()) {
                String listName = String.valueOf(list.getKey());
                Object value = list.getValue();
                if (value == null) {
                    shapes.add(ShapeMessages.nullList(modifierId, blockId, listName));
                } else if (value instanceof List<?> items) {
                    if (items.isEmpty()) {
                        shapes.add(ShapeMessages.emptyList(
                                modifierId, blockId, listName));
                    }
                    lists.put(listName, readLines(modifierId, blockId, listName,
                            value, early, shapes));
                } else {
                    shapes.add(ShapeMessages.skippedList(
                            modifierId, blockId, listName));
                }
            }
            return;
        }
        if (commands instanceof List) {
            shapes.add(ShapeMessages.listedCommands(modifierId, blockId));
            lists.put(ShapeMessages.SYNTHETIC_LIST, readLines(modifierId, blockId,
                    ShapeMessages.SYNTHETIC_LIST, commands, early, shapes));
            return;
        }
        shapes.add(ShapeMessages.ignoredCommands(modifierId, blockId));
    }

    /**
     * Read one block's raw option values. Misshapen subtrees
     * read as absent here; the shape checks own them.
     */
    private static OptionsNode readOptions(Map<?, ?> block) {
        Object preStartOrder = null;
        if (block.get("on-start") instanceof Map<?, ?> onStart) {
            preStartOrder = onStart.get("pre-start-order");
        }
        if (!(block.get("options") instanceof Map<?, ?> options)) {
            return new OptionsNode(null, null, null, null, null, null, null, null,
                    null, preStartOrder);
        }
        Object interval = null;
        Object deviation = null;
        Object intervalBehavior = null;
        if (options.get("interval-settings") instanceof Map<?, ?> settings) {
            interval = settings.get("interval");
            deviation = settings.get("deviation");
            intervalBehavior = settings.get("behavior");
        }
        Object chance = null;
        Object chanceBehavior = null;
        if (options.get("success-chance") instanceof Map<?, ?> success) {
            chance = success.get("chance");
            chanceBehavior = success.get("behavior");
        }
        Object selection = null;
        Object pickCount = null;
        Object pickBehavior = null;
        if (options.get("execution") instanceof Map<?, ?> execution) {
            selection = execution.get("selection");
            if (execution.get("pick-random") instanceof Map<?, ?> pick) {
                pickCount = pick.get("count");
                pickBehavior = pick.get("behavior");
            }
        }
        return new OptionsNode(interval, deviation, intervalBehavior, chance,
                chanceBehavior, selection, pickCount, pickBehavior,
                options.get("delay"), preStartOrder);
    }

    /**
     * Shape scan of one block's option subtrees: every
     * non-map subtree and non-binder scalar fails the engine
     * load, so each errors here. Bindability reuses the core
     * loader table; absent values stay silent.
     */
    private static void readOptionShapes(String modifierId, String blockId,
            Map<?, ?> block, List<Diagnostic> early) {
        String base = "'behavior." + blockId + ".";
        Object onStart = block.get("on-start");
        if (onStart != null && !(onStart instanceof Map)) {
            early.add(shaped(modifierId, blockId, base + "on-start' must be a map"));
        } else if (onStart instanceof Map<?, ?> start) {
            mustBeString(early, modifierId, blockId,
                    start.get("pre-start-order"), base + "on-start.pre-start-order'");
        }
        if (!(block.get("options") instanceof Map<?, ?> options)) {
            if (block.get("options") != null) {
                early.add(shaped(modifierId, blockId, base + "options' must be a map"));
            }
            return;
        }
        if (!(options.get("interval-settings") instanceof Map<?, ?> settings)) {
            if (options.get("interval-settings") != null) {
                early.add(shaped(modifierId, blockId,
                        base + "options.interval-settings' must be a map"));
            }
        } else {
            mustBeNumber(early, modifierId, blockId, settings.get("interval"),
                    base + "options.interval-settings.interval'");
            mustBeNumber(early, modifierId, blockId, settings.get("deviation"),
                    base + "options.interval-settings.deviation'");
            mustBeString(early, modifierId, blockId, settings.get("behavior"),
                    base + "options.interval-settings.behavior'");
        }
        if (!(options.get("success-chance") instanceof Map<?, ?> success)) {
            if (options.get("success-chance") != null) {
                early.add(shaped(modifierId, blockId,
                        base + "options.success-chance' must be a map"));
            }
        } else {
            mustBeNumber(early, modifierId, blockId, success.get("chance"),
                    base + "options.success-chance.chance'");
            mustBeString(early, modifierId, blockId, success.get("behavior"),
                    base + "options.success-chance.behavior'");
        }
        if (!(options.get("execution") instanceof Map<?, ?> execution)) {
            if (options.get("execution") != null) {
                early.add(shaped(modifierId, blockId,
                        base + "options.execution' must be a map"));
            }
        } else {
            mustBeString(early, modifierId, blockId, execution.get("selection"),
                    base + "options.execution.selection'");
            if (!(execution.get("pick-random") instanceof Map<?, ?> pick)) {
                if (execution.get("pick-random") != null) {
                    early.add(shaped(modifierId, blockId,
                            base + "options.execution.pick-random' must be a map"));
                }
            } else {
                mustBeInteger(early, modifierId, blockId, pick.get("count"),
                        base + "options.execution.pick-random.count'", true);
                mustBeString(early, modifierId, blockId, pick.get("behavior"),
                        base + "options.execution.pick-random.behavior'");
            }
        }
        mustBeInteger(early, modifierId, blockId, options.get("delay"),
                base + "options.delay'", false);
    }

    private static void mustBeNumber(List<Diagnostic> early, String modifierId,
            String blockId, Object raw, String path) {
        if (raw != null && TriggerParams.asDouble(raw) == null) {
            early.add(shaped(modifierId, blockId, path + " must be a number"));
        }
    }

    private static void mustBeInteger(List<Diagnostic> early, String modifierId,
            String blockId, Object raw, String path, boolean intRange) {
        if (raw == null) {
            return;
        }
        Long value = TriggerParams.asLong(raw);
        if (value == null
                || (intRange && (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE))) {
            early.add(shaped(modifierId, blockId, path + " must be an integer"));
        }
    }

    private static void mustBeString(List<Diagnostic> early, String modifierId,
            String blockId, Object raw, String path) {
        if (raw instanceof Map || raw instanceof List) {
            early.add(shaped(modifierId, blockId, path + " must be a string"));
        }
    }

    /**
     * Shape scan of display metadata: string fields reject
     * maps and lists at load, and a scalar visibility block
     * fails with them. Scalars stringify and stay silent.
     */
    private static void readMetaShapes(String modifierId, Map<?, ?> doc,
            List<Diagnostic> early) {
        if (!(doc.get("meta") instanceof Map<?, ?> meta)) {
            return;
        }
        for (String key : List.of("name", "description", "icon", "author")) {
            if (meta.get(key) instanceof Map || meta.get(key) instanceof List) {
                early.add(fileDiagnostic(modifierId, DiagnosticCode.E_YAML_SHAPE,
                        "'meta." + key + "' must be a string"));
            }
        }
        Object visibility = meta.get("visibility");
        if (visibility == null) {
            return;
        }
        if (!(visibility instanceof Map<?, ?> vis)) {
            early.add(fileDiagnostic(modifierId, DiagnosticCode.E_YAML_SHAPE,
                    "'meta.visibility' must be a map"));
            return;
        }
        for (String key : List.of("icon", "color")) {
            if (vis.get(key) instanceof Map || vis.get(key) instanceof List) {
                early.add(fileDiagnostic(modifierId, DiagnosticCode.E_YAML_SHAPE,
                        "'meta.visibility." + key + "' must be a string"));
            }
        }
    }

    /**
     * Shape scan of the settings subtree, paralleling the
     * node builder drop for drop: every node the builder
     * skips warns here. Nulls count as absent on both sides
     * and stay silent.
     */
    private static void readSettingsShapes(String modifierId, Map<?, ?> doc,
            List<Diagnostic> shapes) {
        if (!(doc.get("config") instanceof Map<?, ?> config)) {
            return;
        }
        Object settings = config.get("settings");
        if (settings == null) {
            return;
        }
        if (!(settings instanceof Map<?, ?> entries)) {
            shapes.add(ShapeMessages.skippedSettings(modifierId));
        } else {
            for (Map.Entry<?, ?> entry : entries.entrySet()) {
                scanSetting(modifierId, String.valueOf(entry.getKey()),
                        entry.getValue(), shapes);
            }
        }
        Object advanced = config.get("advanced");
        if (advanced != null && !(advanced instanceof Map)) {
            shapes.add(ShapeMessages.skippedAdvanced(modifierId));
        }
    }

    private static void scanSetting(String modifierId, String path, Object raw,
            List<Diagnostic> shapes) {
        if (!(raw instanceof Map<?, ?> map)) {
            shapes.add(ShapeMessages.settingNotMap(modifierId, path));
            return;
        }
        Object nodeDef = map.get("def");
        if (nodeDef == null) {
            return;
        }
        if (!(nodeDef instanceof Map<?, ?> def)) {
            shapes.add(ShapeMessages.settingDefNotMap(modifierId, path));
            return;
        }
        Object meta = def.get("meta");
        if (meta != null && !(meta instanceof Map)) {
            shapes.add(ShapeMessages.settingMetaNotMap(modifierId, path));
        }
        Object spec = def.get("value");
        if (spec != null && !(spec instanceof Map)) {
            shapes.add(ShapeMessages.settingSpecNotMap(modifierId, path));
        }
        Object folder = def.get("folder");
        if (folder == null) {
            return;
        }
        if (!(folder instanceof List<?> list)) {
            shapes.add(ShapeMessages.settingFolderNotList(modifierId, path));
            return;
        }
        for (int index = 0; index < list.size(); index++) {
            Object item = list.get(index);
            if (!(item instanceof Map<?, ?> entry) || entry.size() != 1) {
                shapes.add(ShapeMessages.folderItemNotMap(modifierId, path, index));
                continue;
            }
            Map.Entry<?, ?> single = entry.entrySet().iterator().next();
            String child = path + "." + single.getKey();
            if (!(single.getValue() instanceof Map)) {
                shapes.add(ShapeMessages.folderValueNotMap(modifierId, child));
                continue;
            }
            scanSetting(modifierId, child, single.getValue(), shapes);
        }
    }

    /**
     * Read one block's {@code runs-on} trigger names. A missing
     * list reads empty (called-only blocks omit it); a
     * non-list shape is a file diagnostic, mirroring the engine
     * load failure. Scalar entries stringify like the loader
     * so the trigger sweep sees them; nulls warn and drop;
     * maps and lists fail the load and error here.
     */
    private static List<String> readRunsOn(String modifierId, String blockId,
            Map<?, ?> block, List<Diagnostic> early, List<Diagnostic> shapes) {
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
        for (int index = 0; index < items.size(); index++) {
            Object entry = items.get(index);
            if (entry instanceof String trigger) {
                triggers.add(trigger);
            } else if (entry == null) {
                shapes.add(ShapeMessages.nullTrigger(modifierId, blockId, index));
            } else if (entry instanceof Map || entry instanceof List) {
                early.add(shapedAt(modifierId, blockId, "runs-on", index,
                        "runs-on entry must be a string"));
            } else {
                triggers.add(String.valueOf(entry));
            }
        }
        return triggers;
    }

    /**
     * Read one commands list. Strings read verbatim; scalars
     * stringify with a warning like the loader; nulls read
     * null (skipped downstream, like the engine) with a
     * warning; maps and lists fail the load and error here.
     */
    private static List<String> readLines(String modifierId, String blockId,
            String listName, Object raw, List<Diagnostic> early,
            List<Diagnostic> shapes) {
        List<String> lines = new ArrayList<>();
        List<?> items = (List<?>) raw;
        for (int index = 0; index < items.size(); index++) {
            Object entry = items.get(index);
            if (entry instanceof String line) {
                lines.add(line);
            } else if (entry == null) {
                lines.add(null);
                shapes.add(ShapeMessages.nullLine(
                        modifierId, blockId, listName, index));
            } else if (entry instanceof Map || entry instanceof List) {
                lines.add(null);
                early.add(new Diagnostic(
                        new Provenance(modifierId, blockId, listName, index), -1,
                        Severity.ERROR, DiagnosticCode.E_YAML_LINE_TYPE,
                        "command line must be a string"));
            } else {
                lines.add(String.valueOf(entry));
                shapes.add(ShapeMessages.coercedLine(
                        modifierId, blockId, listName, index, entry));
            }
        }
        return lines;
    }

    private static Diagnostic shaped(String modifierId, String blockId, String message) {
        return shapedAt(modifierId, blockId, "", -1, message);
    }

    private static Diagnostic shapedAt(String modifierId, String blockId,
            String listName, int lineIndex, String message) {
        return new Diagnostic(
                new Provenance(modifierId, blockId, listName, lineIndex), -1,
                Severity.ERROR, DiagnosticCode.E_YAML_SHAPE, message);
    }

    private static Diagnostic fileDiagnostic(String modifierId,
            DiagnosticCode code, String message) {
        return new Diagnostic(new Provenance(modifierId, "", "", -1), -1,
                Severity.ERROR, code, message);
    }

    private static ReadResult resultWith(BehaviorFile behavior, Diagnostic early) {
        return new ReadResult(behavior, SettingsTree.empty(), List.of(), List.of(early),
                List.of(), List.of());
    }

    private static String oneLine(Exception failed) {
        return String.join(" ", String.valueOf(failed.getMessage()).split("\\s+"));
    }
}
