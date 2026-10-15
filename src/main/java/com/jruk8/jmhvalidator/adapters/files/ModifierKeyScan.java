package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.UnknownKeys;
import com.jruk8.jmhvalidator.core.UnknownKeys.Level;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Unknown-key scan behind {@link ModifierYaml}: root, meta, config, and blocks. */
final class ModifierKeyScan {
    private ModifierKeyScan() {
    }

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
}
