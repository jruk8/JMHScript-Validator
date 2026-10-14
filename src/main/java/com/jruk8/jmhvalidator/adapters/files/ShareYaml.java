package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.share.ShareCodec;
import com.jruk8.jmhvalidator.core.share.ShareModel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Modifier and preset YAML for the share commands: file maps to
 * and from the core share model. Shape errors read like the
 * engine file checks (single line, same key names); unknown keys
 * are ignored, matching a load-then-export round trip. Settings
 * cross raw so author content survives the trip untouched.
 */
public final class ShareYaml {
    private ShareYaml() {
    }

    /** One parsed file: its kind, id, and exactly one payload. */
    public record Parsed(ShareCodec.Kind kind, String id,
            ShareModel.Modifier modifier, ShareModel.Preset preset) {
    }

    /**
     * Parses one modifier or preset file. The id is the filename
     * stem; the kind follows the content keys.
     */
    public static Parsed parse(Path file) throws ShareYamlException {
        String text = readText(file);
        Object doc;
        try {
            doc = new Yaml().load(text);
        } catch (YAMLException failed) {
            throw new ShareYamlException("could not parse file: " + firstLine(failed.getMessage()));
        }
        if (!(doc instanceof Map<?, ?> raw)) {
            throw new ShareYamlException("file must be a mapping");
        }
        Map<String, Object> map = stringKeys(raw);
        if (map.isEmpty()) {
            throw new ShareYamlException("file has no content");
        }
        String id = stemOf(file);
        if (!ShareCodec.validId(id)) {
            throw new ShareYamlException("invalid modifier id '" + id + "'");
        }
        boolean modifierLike = map.containsKey("behavior") || map.containsKey("enabled")
                || map.containsKey("config");
        boolean presetLike = map.containsKey("modifiers") || map.containsKey("presets");
        if (modifierLike && presetLike) {
            throw new ShareYamlException("ambiguous file: has both modifier and preset content");
        }
        if (presetLike) {
            return new Parsed(ShareCodec.Kind.PRESET, id, null, readPreset(map));
        }
        return new Parsed(ShareCodec.Kind.MODIFIER, id, readModifier(map), null);
    }

    /** Ordered file map for one decoded modifier. */
    public static Map<String, Object> writeModifier(ShareModel.Modifier modifier) {
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("enabled", false);
        file.put("meta", writeMeta(modifier.meta(), ShareCodec.DEFAULT_MODIFIER_NAME));
        if ((modifier.settings() != null && !modifier.settings().isEmpty())
                || (modifier.advanced() != null && !modifier.advanced().isEmpty())) {
            Map<String, Object> config = new LinkedHashMap<>();
            if (modifier.settings() != null && !modifier.settings().isEmpty()) {
                config.put("settings", modifier.settings());
            }
            if (modifier.advanced() != null && !modifier.advanced().isEmpty()) {
                config.put("advanced", modifier.advanced());
            }
            file.put("config", config);
        }
        if (modifier.behaviors() != null && !modifier.behaviors().isEmpty()) {
            Map<String, Object> behaviors = new LinkedHashMap<>();
            for (Map.Entry<String, ShareModel.Behavior> behavior
                    : modifier.behaviors().entrySet()) {
                if (behavior.getValue() != null) {
                    behaviors.put(behavior.getKey(), writeBehavior(behavior.getValue()));
                }
            }
            if (!behaviors.isEmpty()) {
                file.put("behavior", behaviors);
            }
        }
        return file;
    }

    /** Ordered file map for one decoded preset. */
    public static Map<String, Object> writePreset(ShareModel.Preset preset) {
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("meta", writeMeta(preset.meta(), ShareCodec.DEFAULT_PRESET_NAME));
        if (preset.modifiers() != null) {
            file.put("modifiers", new ArrayList<>(preset.modifiers()));
        }
        if (preset.presets() != null) {
            file.put("presets", new ArrayList<>(preset.presets()));
        }
        return file;
    }

    /** Block-style YAML text for one ordered file map. */
    public static String dump(Map<String, Object> file) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(options).dump(file);
    }

    private static ShareModel.Modifier readModifier(Map<String, Object> map)
            throws ShareYamlException {
        if (map.containsKey("enabled") && !(map.get("enabled") instanceof Boolean)) {
            throw new ShareYamlException("enabled must be true or false");
        }
        Map<String, Object> settings = null;
        Map<String, Object> advanced = null;
        if (map.containsKey("config")) {
            Map<String, Object> config = needMap(map.get("config"), "config");
            if (config.get("settings") instanceof Map<?, ?> rawSettings) {
                settings = stringKeys(rawSettings);
            }
            if (config.get("advanced") instanceof Map<?, ?> rawAdvanced) {
                advanced = stringKeys(rawAdvanced);
            }
        }
        Map<String, ShareModel.Behavior> behaviors = null;
        if (map.containsKey("behavior")) {
            Map<String, Object> raw = needMap(map.get("behavior"), "behavior");
            behaviors = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                if (entry.getValue() == null) {
                    behaviors.put(entry.getKey(), null);
                    continue;
                }
                behaviors.put(entry.getKey(), readBehavior(
                        needMap(entry.getValue(), "behavior '" + entry.getKey() + "'")));
            }
        }
        return new ShareModel.Modifier(readMeta(map), settings, advanced, behaviors);
    }

    private static ShareModel.Preset readPreset(Map<String, Object> map)
            throws ShareYamlException {
        List<String> modifiers = null;
        if (map.containsKey("modifiers")) {
            modifiers = needStrings(map.get("modifiers"), "modifiers");
        }
        List<String> presets = null;
        if (map.containsKey("presets")) {
            presets = needStrings(map.get("presets"), "presets");
        }
        return new ShareModel.Preset(readMeta(map), modifiers, presets);
    }

    private static ShareModel.Meta readMeta(Map<String, Object> map) throws ShareYamlException {
        if (!map.containsKey("meta")) {
            return null;
        }
        Map<String, Object> meta = needMap(map.get("meta"), "meta");
        ShareModel.Visibility visibility = null;
        if (meta.containsKey("visibility")) {
            Map<String, Object> raw = needMap(meta.get("visibility"), "meta.visibility");
            Boolean broadcast = null;
            if (raw.containsKey("broadcast")) {
                if (!(raw.get("broadcast") instanceof Boolean flag)) {
                    throw new ShareYamlException("meta.visibility.broadcast must be true or false");
                }
                broadcast = flag;
            }
            visibility = new ShareModel.Visibility(
                    text(raw.get("icon")), text(raw.get("color")), broadcast);
        }
        return new ShareModel.Meta(text(meta.get("name")), text(meta.get("description")),
                text(meta.get("icon")), text(meta.get("author")), visibility);
    }

    private static ShareModel.Behavior readBehavior(Map<String, Object> map)
            throws ShareYamlException {
        List<String> runsOn = null;
        if (map.containsKey("runs-on")) {
            runsOn = needStrings(map.get("runs-on"), "runs-on");
        }
        String order = null;
        if (map.containsKey("on-start")) {
            Map<String, Object> onStart = needMap(map.get("on-start"), "on-start");
            order = text(onStart.get("pre-start-order"));
        }
        ShareModel.Options options = null;
        if (map.containsKey("options")) {
            options = readOptions(needMap(map.get("options"), "options"));
        }
        Map<String, List<String>> commands = null;
        if (map.containsKey("commands")) {
            Map<String, Object> raw = needMap(map.get("commands"), "commands");
            commands = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                commands.put(entry.getKey(),
                        needStrings(entry.getValue(), "commands '" + entry.getKey() + "'"));
            }
        }
        return new ShareModel.Behavior(runsOn, order, options, commands);
    }

    private static ShareModel.Options readOptions(Map<String, Object> map)
            throws ShareYamlException {
        ShareModel.Interval interval = null;
        if (map.containsKey("interval-settings")) {
            Map<String, Object> raw = needMap(map.get("interval-settings"), "interval-settings");
            interval = new ShareModel.Interval(asDouble(raw.get("interval"), "interval"),
                    asDouble(raw.get("deviation"), "deviation"), text(raw.get("behavior")));
        }
        ShareModel.Chance chance = null;
        if (map.containsKey("success-chance")) {
            Map<String, Object> raw = needMap(map.get("success-chance"), "success-chance");
            chance = new ShareModel.Chance(
                    asDouble(raw.get("chance"), "chance"), text(raw.get("behavior")));
        }
        ShareModel.Execution execution = null;
        if (map.containsKey("execution")) {
            Map<String, Object> raw = needMap(map.get("execution"), "execution");
            ShareModel.PickRandom pick = null;
            if (raw.containsKey("pick-random")) {
                Map<String, Object> pickRaw = needMap(raw.get("pick-random"), "pick-random");
                pick = new ShareModel.PickRandom(
                        asInt(pickRaw.get("count"), "count"), text(pickRaw.get("behavior")));
            }
            execution = new ShareModel.Execution(text(raw.get("selection")), pick);
        }
        return new ShareModel.Options(interval, chance, execution, asLong(map.get("delay"), "delay"));
    }

    private static Map<String, Object> writeMeta(ShareModel.Meta meta, String fallback) {
        Map<String, Object> written = new LinkedHashMap<>();
        written.put("name", meta == null || meta.name() == null || meta.name().isBlank()
                ? fallback : meta.name());
        if (meta != null && meta.description() != null && !meta.description().isEmpty()) {
            written.put("description", meta.description());
        }
        written.put("icon", meta == null || meta.icon() == null || meta.icon().isBlank()
                ? ShareCodec.DEFAULT_ICON : meta.icon());
        if (meta != null && meta.author() != null && !meta.author().isEmpty()) {
            written.put("author", meta.author());
        }
        if (meta != null && meta.visibility() != null) {
            Map<String, Object> visibility = new LinkedHashMap<>();
            if (meta.visibility().icon() != null) {
                visibility.put("icon", meta.visibility().icon());
            }
            if (meta.visibility().color() != null) {
                visibility.put("color", meta.visibility().color());
            }
            if (meta.visibility().broadcast() != null) {
                visibility.put("broadcast", meta.visibility().broadcast());
            }
            if (!visibility.isEmpty()) {
                written.put("visibility", visibility);
            }
        }
        return written;
    }

    private static Map<String, Object> writeBehavior(ShareModel.Behavior behavior) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (behavior.runsOn() != null) {
            data.put("runs-on", new ArrayList<>(behavior.runsOn()));
        }
        if (behavior.preStartOrder() != null) {
            data.put("on-start", Map.of("pre-start-order", behavior.preStartOrder()));
        }
        if (behavior.options() != null) {
            data.put("options", writeOptions(behavior.options()));
        }
        if (behavior.commands() != null) {
            Map<String, Object> commands = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> list : behavior.commands().entrySet()) {
                commands.put(list.getKey(), new ArrayList<>(list.getValue()));
            }
            data.put("commands", commands);
        }
        return data;
    }

    private static Map<String, Object> writeOptions(ShareModel.Options options) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (options.interval() != null) {
            Map<String, Object> written = new LinkedHashMap<>();
            if (options.interval().interval() != null) {
                written.put("interval", options.interval().interval());
            }
            if (options.interval().deviation() != null) {
                written.put("deviation", options.interval().deviation());
            }
            if (options.interval().behavior() != null) {
                written.put("behavior", options.interval().behavior());
            }
            data.put("interval-settings", written);
        }
        if (options.chance() != null) {
            Map<String, Object> written = new LinkedHashMap<>();
            if (options.chance().chance() != null) {
                written.put("chance", options.chance().chance());
            }
            if (options.chance().behavior() != null) {
                written.put("behavior", options.chance().behavior());
            }
            data.put("success-chance", written);
        }
        if (options.execution() != null) {
            data.put("execution", writeExecution(options.execution()));
        }
        if (options.delay() != null) {
            data.put("delay", options.delay());
        }
        return data;
    }

    private static Map<String, Object> writeExecution(ShareModel.Execution execution) {
        Map<String, Object> written = new LinkedHashMap<>();
        if (execution.selection() != null) {
            written.put("selection", execution.selection());
        }
        if (execution.pickRandom() != null) {
            Map<String, Object> pick = new LinkedHashMap<>();
            if (execution.pickRandom().count() != null) {
                pick.put("count", execution.pickRandom().count());
            }
            if (execution.pickRandom().behavior() != null) {
                pick.put("behavior", execution.pickRandom().behavior());
            }
            written.put("pick-random", pick);
        }
        return written;
    }

    private static List<String> needStrings(Object value, String what) throws ShareYamlException {
        if (!(value instanceof List<?> list)) {
            throw new ShareYamlException(what + " must be a list");
        }
        List<String> strings = new ArrayList<>();
        for (Object item : list) {
            if (item == null || item instanceof Map || item instanceof List) {
                throw new ShareYamlException(what + " must hold strings");
            }
            strings.add(String.valueOf(item));
        }
        return strings;
    }

    private static Map<String, Object> needMap(Object value, String what)
            throws ShareYamlException {
        if (value instanceof Map<?, ?> map) {
            return stringKeys(map);
        }
        throw new ShareYamlException(what + " must be a mapping");
    }

    private static Double asDouble(Object value, String what) throws ShareYamlException {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new ShareYamlException(what + " must be a number");
    }

    private static Integer asInt(Object value, String what) throws ShareYamlException {
        Long whole = asLong(value, what);
        if (whole == null) {
            return null;
        }
        if (whole < Integer.MIN_VALUE || whole > Integer.MAX_VALUE) {
            throw new ShareYamlException(what + " must be a whole number");
        }
        return (int) (long) whole;
    }

    private static Long asLong(Object value, String what) throws ShareYamlException {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number
                && number.doubleValue() % 1.0 == 0.0
                && number.doubleValue() >= Long.MIN_VALUE
                && number.doubleValue() <= Long.MAX_VALUE) {
            return number.longValue();
        }
        throw new ShareYamlException(what + " must be a whole number");
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Map<String, Object> stringKeys(Map<?, ?> raw) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            map.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return map;
    }

    private static String readText(Path file) throws ShareYamlException {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException failed) {
            throw new ShareYamlException("could not read file: " + firstLine(failed.getMessage()));
        }
    }

    private static String stemOf(Path file) {
        return file.getFileName().toString().replaceFirst("\\.[^.]*$", "");
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "unknown error";
        }
        String line = message.strip();
        int end = line.indexOf('\n');
        if (end != -1) {
            line = line.substring(0, end).strip();
        }
        return line.isEmpty() ? "unknown error" : line;
    }
}
