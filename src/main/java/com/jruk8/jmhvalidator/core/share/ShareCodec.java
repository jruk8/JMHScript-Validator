package com.jruk8.jmhvalidator.core.share;

import com.jruk8.jmhvalidator.core.ports.ShareMaterials;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Share strings for modifiers and presets: the versioned envelope
 * schema over framed JSON. Decoding enforces strict schema
 * validation; anything corrupt or off-schema decodes to empty.
 * Material (icon) validity comes through the {@link ShareMaterials}
 * port, which the engine implements from its Bukkit table.
 */
public final class ShareCodec {

    /** Tag for deflated payloads. */
    public static final String TAG_DEFLATED = ShareFraming.TAG_DEFLATED;
    /** Tag for raw payloads. */
    public static final String TAG_RAW = ShareFraming.TAG_RAW;
    /** Hard ceiling on decompressed JSON bytes. */
    public static final int MAX_JSON_BYTES = ShareFraming.MAX_JSON_BYTES;
    /** Name written when an exported modifier has none. */
    public static final String DEFAULT_MODIFIER_NAME = "My Modifier";
    /** Name written when an exported preset has none. */
    public static final String DEFAULT_PRESET_NAME = "My Preset";
    /** Icon written when an exported meta block has none. */
    public static final String DEFAULT_ICON = "STONE";

    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,63}");
    private static final Pattern TRIGGER_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

    private ShareCodec() {
    }

    /** True when an id fits the shared modifier/preset id shape. */
    public static boolean validId(String id) {
        return id != null && ID_PATTERN.matcher(id).matches();
    }

    /** Decoded share string: the kind, id, and validated entry. */
    public enum Kind {
        MODIFIER,
        PRESET
    }

    /** One decoded payload; exactly one of modifier or preset is set. */
    public record Imported(Kind kind, String id,
            ShareModel.Modifier modifier, ShareModel.Preset preset) {
    }

    /** Share string for one modifier. */
    public static String exportModifier(String id, ShareModel.Modifier modifier) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", "modifier");
        envelope.put("id", id);
        envelope.put("data", modifierData(modifier));
        return ShareFraming.encode(ShareJson.stringify(envelope));
    }

    /** Share string for one preset. */
    public static String exportPreset(String id, ShareModel.Preset preset) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", "preset");
        envelope.put("id", id);
        envelope.put("data", presetData(preset));
        return ShareFraming.encode(ShareJson.stringify(envelope));
    }

    /**
     * Decodes and validates a share string. Empty when the tag,
     * checksum, JSON, or schema is wrong.
     */
    public static Optional<Imported> decode(String payload, ShareMaterials materials) {
        if (payload == null) {
            return Optional.empty();
        }
        Optional<String> json = ShareFraming.decode(payload.trim());
        if (json.isEmpty()) {
            return Optional.empty();
        }
        try {
            Optional<Object> root = ShareJson.parse(json.get());
            if (root.isEmpty()) {
                return Optional.empty();
            }
            Map<String, Object> envelope = asMap(root.get());
            if (envelope == null) {
                return Optional.empty();
            }
            String type = requiredString(envelope, "type");
            String id = requiredString(envelope, "id");
            if (!validId(id)) {
                return Optional.empty();
            }
            Map<String, Object> data = optionalObject(envelope, "data");
            if (data == null) {
                return Optional.empty();
            }
            return switch (type) {
                case "modifier" -> Optional.of(new Imported(Kind.MODIFIER, id,
                        readModifier(data, materials), null));
                case "preset" -> Optional.of(new Imported(Kind.PRESET, id,
                        null, readPreset(data, materials)));
                default -> Optional.empty();
            };
        } catch (Invalid failed) {
            return Optional.empty();
        }
    }

    private static Map<String, Object> modifierData(ShareModel.Modifier modifier) {
        Map<String, Object> data = new LinkedHashMap<>();
        // Exports always carry disabled: the entry itself is untouched.
        data.put("enabled", false);
        data.put("meta", modifierMetaData(modifier.meta()));
        if (modifier.settings() != null || modifier.advanced() != null) {
            Map<String, Object> config = configData(modifier.settings(), modifier.advanced());
            if (!config.isEmpty()) {
                data.put("config", config);
            }
        }
        if (modifier.behaviors() != null && !modifier.behaviors().isEmpty()) {
            Map<String, Object> indexed = new LinkedHashMap<>();
            for (Map.Entry<String, ShareModel.Behavior> behavior
                    : modifier.behaviors().entrySet()) {
                if (behavior.getValue() != null) {
                    indexed.put(behavior.getKey(), behaviorData(behavior.getValue()));
                }
            }
            data.put("behavior", indexed);
        }
        return data;
    }

    private static Map<String, Object> modifierMetaData(ShareModel.Meta meta) {
        Map<String, Object> written = new LinkedHashMap<>();
        written.put("name", meta == null || meta.name() == null || meta.name().isBlank()
                ? DEFAULT_MODIFIER_NAME : meta.name());
        if (meta != null && meta.description() != null) {
            written.put("description", meta.description());
        }
        written.put("icon", meta == null || meta.icon() == null || meta.icon().isBlank()
                ? DEFAULT_ICON : meta.icon());
        if (meta != null && meta.author() != null) {
            written.put("author", meta.author());
        }
        if (meta != null && meta.visibility() != null) {
            written.put("visibility", visibilityData(meta.visibility()));
        }
        return written;
    }

    private static Map<String, Object> behaviorData(ShareModel.Behavior behavior) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (behavior.runsOn() != null) {
            data.put("runs-on", new ArrayList<>(behavior.runsOn()));
        }
        if (behavior.preStartOrder() != null) {
            Map<String, Object> onStart = new LinkedHashMap<>();
            onStart.put("pre-start-order", behavior.preStartOrder());
            data.put("on-start", onStart);
        }
        if (behavior.options() != null) {
            data.put("options", optionsData(behavior.options()));
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

    private static Map<String, Object> optionsData(ShareModel.Options options) {
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
            data.put("execution", executionData(options.execution()));
        }
        if (options.delay() != null) {
            data.put("delay", options.delay());
        }
        return data;
    }

    private static Map<String, Object> executionData(ShareModel.Execution execution) {
        Map<String, Object> written = new LinkedHashMap<>();
        if (execution.selection() != null) {
            written.put("selection", execution.selection());
        }
        if (execution.pickRandom() != null) {
            Map<String, Object> pickWritten = new LinkedHashMap<>();
            if (execution.pickRandom().count() != null) {
                pickWritten.put("count", execution.pickRandom().count());
            }
            if (execution.pickRandom().behavior() != null) {
                pickWritten.put("behavior", execution.pickRandom().behavior());
            }
            written.put("pick-random", pickWritten);
        }
        return written;
    }

    private static Map<String, Object> presetData(ShareModel.Preset preset) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("meta", presetMetaData(preset.meta()));
        if (preset.modifiers() != null) {
            data.put("modifiers", new ArrayList<>(preset.modifiers()));
        }
        if (preset.presets() != null) {
            data.put("presets", new ArrayList<>(preset.presets()));
        }
        return data;
    }

    private static Map<String, Object> presetMetaData(ShareModel.Meta meta) {
        Map<String, Object> written = new LinkedHashMap<>();
        written.put("name", meta == null || meta.name() == null || meta.name().isBlank()
                ? DEFAULT_PRESET_NAME : meta.name());
        if (meta != null && meta.description() != null) {
            written.put("description", meta.description());
        }
        written.put("icon", meta == null || meta.icon() == null || meta.icon().isBlank()
                ? DEFAULT_ICON : meta.icon());
        if (meta != null && meta.author() != null) {
            written.put("author", meta.author());
        }
        return written;
    }

    /** Visibility block for exports; omits nulls so defaults stay lean. */
    private static Map<String, Object> visibilityData(ShareModel.Visibility visibility) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (visibility.icon() != null) {
            data.put("icon", visibility.icon());
        }
        if (visibility.color() != null) {
            data.put("color", visibility.color());
        }
        if (visibility.broadcast() != null) {
            data.put("broadcast", visibility.broadcast());
        }
        return data;
    }

    /** Config block for exports: settings raw maps plus advanced wiring. */
    private static Map<String, Object> configData(
            Map<String, Object> settings, Map<String, Object> advanced) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (settings != null && !settings.isEmpty()) {
            data.put("settings", settings);
        }
        if (advanced != null && !advanced.isEmpty()) {
            data.put("advanced", advanced);
        }
        return data;
    }

    private static ShareModel.Modifier readModifier(
            Map<String, Object> data, ShareMaterials materials) {
        // Imports always land disabled: the export value is validated
        // for shape but never applied.
        optionalBoolean(data, "enabled", false);
        Map<String, Object> meta = optionalObject(data, "meta");
        if (meta == null) {
            throw new Invalid();
        }
        String name = requiredString(meta, "name");
        if (name.isBlank()) {
            throw new Invalid();
        }
        String icon = optionalString(meta, "icon", materials.fallbackIcon());
        if (!materials.validIcon(icon)) {
            throw new Invalid();
        }
        ShareModel.Visibility visibility = null;
        Map<String, Object> visibilityData = optionalObject(meta, "visibility");
        if (visibilityData != null) {
            visibility = readVisibility(visibilityData, materials);
        }
        ShareModel.Meta written = new ShareModel.Meta(name,
                optionalString(meta, "description", ""), icon,
                optionalString(meta, "author", ""), visibility);
        Map<String, Object> settings = null;
        Map<String, Object> advanced = null;
        Map<String, Object> config = optionalObject(data, "config");
        if (config != null) {
            settings = optionalObject(config, "settings");
            advanced = optionalObject(config, "advanced");
        }
        Map<String, ShareModel.Behavior> behaviors = null;
        Map<String, Object> behavior = optionalObject(data, "behavior");
        if (behavior != null) {
            behaviors = readBehaviors(behavior);
        }
        return new ShareModel.Modifier(written, settings, advanced, behaviors);
    }

    private static Map<String, ShareModel.Behavior> readBehaviors(Map<String, Object> behavior) {
        Map<String, ShareModel.Behavior> behaviors = new LinkedHashMap<>();
        for (Map.Entry<String, Object> indexed : behavior.entrySet()) {
            Map<String, Object> data = asMap(indexed.getValue());
            if (data == null) {
                throw new Invalid();
            }
            behaviors.put(indexed.getKey(), readBehavior(data));
        }
        return behaviors;
    }

    private static ShareModel.Behavior readBehavior(Map<String, Object> data) {
        List<Object> runs = optionalArray(data, "runs-on");
        List<String> triggers = null;
        if (runs != null) {
            triggers = readTriggers(runs);
        }
        String order = null;
        Map<String, Object> onStart = optionalObject(data, "on-start");
        if (onStart != null) {
            order = requiredString(onStart, "pre-start-order");
            if (!order.equals("BEFORE") && !order.equals("AFTER")) {
                throw new Invalid();
            }
        }
        ShareModel.Options options = null;
        Map<String, Object> optionsData = optionalObject(data, "options");
        if (optionsData != null) {
            options = readOptions(optionsData);
        }
        Map<String, List<String>> commands = null;
        Map<String, Object> commandsData = optionalObject(data, "commands");
        if (commandsData != null) {
            commands = readCommands(commandsData);
        }
        return new ShareModel.Behavior(triggers, order, options, commands);
    }

    private static Map<String, List<String>> readCommands(Map<String, Object> commands) {
        Map<String, List<String>> written = new LinkedHashMap<>();
        for (Map.Entry<String, Object> list : commands.entrySet()) {
            if (list.getKey().isBlank() || !(list.getValue() instanceof List<?> raw)) {
                throw new Invalid();
            }
            List<String> lines = new ArrayList<>();
            for (Object line : raw) {
                if (!(line instanceof String text)) {
                    throw new Invalid();
                }
                lines.add(text);
            }
            written.put(list.getKey(), lines);
        }
        return written;
    }

    private static ShareModel.Options readOptions(Map<String, Object> data) {
        ShareModel.Interval interval = null;
        Map<String, Object> intervalData = optionalObject(data, "interval-settings");
        if (intervalData != null) {
            interval = readInterval(intervalData);
        }
        ShareModel.Chance chance = null;
        Map<String, Object> chanceData = optionalObject(data, "success-chance");
        if (chanceData != null) {
            chance = readChance(chanceData);
        }
        ShareModel.Execution execution = null;
        Map<String, Object> executionData = optionalObject(data, "execution");
        if (executionData != null) {
            execution = readExecution(executionData);
        }
        Long delay = optionalLong(data, "delay", 0L, null, null);
        return new ShareModel.Options(interval, chance, execution, delay);
    }

    private static List<String> readTriggers(List<Object> runs) {
        List<String> triggers = new ArrayList<>();
        for (Object trigger : runs) {
            if (!(trigger instanceof String text) || !TRIGGER_PATTERN.matcher(text).matches()) {
                throw new Invalid();
            }
            triggers.add(text);
        }
        return triggers;
    }

    private static ShareModel.Interval readInterval(Map<String, Object> interval) {
        Double seconds = optionalDouble(interval, "interval", null, null, null);
        Double deviation = optionalDouble(interval, "deviation", 0.0, null, null);
        String behavior = optionalString(interval, "behavior", null);
        if (behavior != null) {
            requireExecutorBehavior(behavior);
        }
        return new ShareModel.Interval(seconds, deviation, behavior);
    }

    private static ShareModel.Chance readChance(Map<String, Object> chance) {
        Double fraction = optionalDouble(chance, "chance", 0.0, 1.0, null);
        String behavior = optionalString(chance, "behavior", null);
        if (behavior != null) {
            requireExecutorBehavior(behavior);
        }
        return new ShareModel.Chance(fraction, behavior);
    }

    private static ShareModel.Execution readExecution(Map<String, Object> execution) {
        String selection = optionalString(execution, "selection", null);
        if (selection != null) {
            if (!selection.equals("IN_ORDER") && !selection.equals("PICK_RANDOM")) {
                throw new Invalid();
            }
        }
        ShareModel.PickRandom pick = null;
        Map<String, Object> pickData = optionalObject(execution, "pick-random");
        if (pickData != null) {
            Integer count = optionalInteger(pickData, "count", 1, null, null);
            String behavior = optionalString(pickData, "behavior", null);
            if (behavior != null) {
                requireExecutorBehavior(behavior);
            }
            pick = new ShareModel.PickRandom(count, behavior);
        }
        return new ShareModel.Execution(selection, pick);
    }

    private static ShareModel.Preset readPreset(
            Map<String, Object> data, ShareMaterials materials) {
        Map<String, Object> meta = optionalObject(data, "meta");
        if (meta == null) {
            throw new Invalid();
        }
        String name = requiredString(meta, "name");
        if (name.isBlank()) {
            throw new Invalid();
        }
        String icon = optionalString(meta, "icon", materials.fallbackIcon());
        if (!materials.validIcon(icon)) {
            throw new Invalid();
        }
        ShareModel.Meta written = new ShareModel.Meta(name,
                optionalString(meta, "description", ""), icon,
                optionalString(meta, "author", ""), null);
        return new ShareModel.Preset(written,
                readIdList(data, "modifiers"), readIdList(data, "presets"));
    }

    /** Validates one imported visibility block. */
    private static ShareModel.Visibility readVisibility(
            Map<String, Object> data, ShareMaterials materials) {
        String icon = null;
        if (data.containsKey("icon")) {
            icon = optionalString(data, "icon", materials.fallbackIcon());
            if (!materials.validIcon(icon)) {
                throw new Invalid();
            }
        }
        String color = null;
        if (data.containsKey("color")) {
            color = optionalString(data, "color", null);
            if (!HexColors.valid(color)) {
                throw new Invalid();
            }
            color = HexColors.normalize(color).orElseThrow(Invalid::new);
        }
        Boolean broadcast = null;
        if (data.containsKey("broadcast")) {
            broadcast = optionalBoolean(data, "broadcast", true);
        }
        return new ShareModel.Visibility(icon, color, broadcast);
    }

    /** Optional id list; null when absent, Invalid on bad entries. */
    private static List<String> readIdList(Map<String, Object> data, String key) {
        List<Object> array = optionalArray(data, key);
        if (array == null) {
            return null;
        }
        List<String> ids = new ArrayList<>();
        for (Object member : array) {
            if (!(member instanceof String text) || text.isBlank()) {
                throw new Invalid();
            }
            ids.add(text);
        }
        return ids;
    }

    private static void requireExecutorBehavior(String behavior) {
        if (!behavior.equals("PER_INVOKE") && !behavior.equals("PER_EXECUTOR")) {
            throw new Invalid();
        }
    }

    private static String requiredString(Map<String, Object> data, String key) {
        Object element = data.get(key);
        if (!(element instanceof String text)) {
            throw new Invalid();
        }
        return text;
    }

    private static String optionalString(Map<String, Object> data, String key, String fallback) {
        Object element = data.get(key);
        if (element == null) {
            return fallback;
        }
        if (!(element instanceof String text)) {
            throw new Invalid();
        }
        return text;
    }

    private static boolean optionalBoolean(Map<String, Object> data, String key, boolean fallback) {
        Object element = data.get(key);
        if (element == null) {
            return fallback;
        }
        if (!(element instanceof Boolean flag)) {
            throw new Invalid();
        }
        return flag;
    }

    private static Double optionalDouble(Map<String, Object> data, String key,
            Double minimum, Double maximum, Double fallback) {
        Object element = data.get(key);
        if (element == null) {
            return fallback;
        }
        if (!(element instanceof Double value)) {
            throw new Invalid();
        }
        if (Double.isNaN(value) || Double.isInfinite(value)
                || (minimum != null && value < minimum)
                || (maximum != null && value > maximum)) {
            throw new Invalid();
        }
        return value;
    }

    private static Integer optionalInteger(Map<String, Object> data, String key,
            Integer minimum, Integer maximum, Integer fallback) {
        Object element = data.get(key);
        if (element == null) {
            return fallback;
        }
        if (!(element instanceof Double value)) {
            throw new Invalid();
        }
        if (value % 1.0 != 0.0 || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE
                || (minimum != null && value < minimum)
                || (maximum != null && value > maximum)) {
            throw new Invalid();
        }
        return (int) (double) value;
    }

    private static Long optionalLong(Map<String, Object> data, String key,
            Long minimum, Long maximum, Long fallback) {
        Object element = data.get(key);
        if (element == null) {
            return fallback;
        }
        if (!(element instanceof Double value)) {
            throw new Invalid();
        }
        if (value % 1.0 != 0.0 || value < Long.MIN_VALUE || value > Long.MAX_VALUE
                || (minimum != null && value < minimum)
                || (maximum != null && value > maximum)) {
            throw new Invalid();
        }
        return (long) (double) value;
    }

    private static Map<String, Object> optionalObject(Map<String, Object> data, String key) {
        Object element = data.get(key);
        if (element == null) {
            return null;
        }
        Map<String, Object> map = asMap(element);
        if (map == null) {
            throw new Invalid();
        }
        return map;
    }

    private static List<Object> optionalArray(Map<String, Object> data, String key) {
        Object element = data.get(key);
        if (element == null) {
            return null;
        }
        if (!(element instanceof List<?> list)) {
            throw new Invalid();
        }
        return asList(list);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object element) {
        if (element instanceof Map) {
            return (Map<String, Object>) element;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(List<?> list) {
        return (List<Object>) list;
    }

    /** Schema violation during decoding; always caught into empty. */
    private static final class Invalid extends RuntimeException {
        Invalid() {
            super(null, null, false, false);
        }
    }
}
