package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Unknown-key sweep: YAML keys the engine never reads warn
 * instead of passing silently. Universes are the single
 * source of truth for both file readers (the CLI adapter and
 * the engine codec); an engine drift test pins the Okaeri
 * schema classes against them. Levels mirror the file shape:
 * the file root, {@code config}, behavior blocks with their
 * option subtrees, and the settings node shape. Command list
 * names, behavior ids, and setting entry names stay open
 * (user-defined and callable by name), so they are never
 * flagged.
 */
public final class UnknownKeys {

    /**
     * One checked level: its known keys, the location clause
     * ({@code {b}} fills the behavior id, {@code {s}} the
     * dotted setting path), and the provenance shape.
     */
    public enum Level {
        MODIFIER_ROOT("the file root", false, "", false,
                Set.of("enabled", "meta", "config", "behavior")),
        PRESET_ROOT("the file root", false, "", false,
                Set.of("meta", "modifiers", "presets")),
        ANY_ROOT("the file root", false, "", false,
                Set.of("enabled", "meta", "config", "behavior", "modifiers", "presets")),
        CONFIG("config", false, "config", false,
                Set.of("settings", "advanced")),
        ADVANCED("config.advanced", false, "config.advanced", false,
                Set.of("exclusive-group", "exclusive-as")),
        MODIFIER_META("meta", false, "meta", false,
                Set.of("name", "description", "icon", "author", "visibility")),
        VISIBILITY("meta.visibility", false, "meta.visibility", false,
                Set.of("icon", "color", "broadcast")),
        BLOCK("behavior '{b}'", true, "", false,
                Set.of("runs-on", "on-start", "options", "commands")),
        ON_START("behavior '{b}' on-start", true, "on-start", false,
                Set.of("pre-start-order")),
        OPTIONS("behavior '{b}' options", true, "options", false,
                Set.of("interval-settings", "success-chance", "execution", "delay")),
        INTERVAL_SETTINGS("behavior '{b}' options.interval-settings", true,
                "options.interval-settings", false,
                Set.of("interval", "deviation", "behavior")),
        SUCCESS_CHANCE("behavior '{b}' options.success-chance", true,
                "options.success-chance", false,
                Set.of("chance", "behavior")),
        EXECUTION("behavior '{b}' options.execution", true, "options.execution", false,
                Set.of("selection", "pick-random")),
        PICK_RANDOM("behavior '{b}' options.execution.pick-random", true,
                "options.execution.pick-random", false,
                Set.of("count", "behavior")),
        NODE("setting '{s}'", false, "", true,
                Set.of("value", "def")),
        DEF("setting '{s}' def", false, "", true,
                Set.of("meta", "value", "folder")),
        SETTING_META("setting '{s}' def.meta", false, "", true,
                Set.of("name", "desc", "icon")),
        SPEC("setting '{s}' def.value", false, "", true,
                Set.of("type", "default", "allowed"));

        private final String where;
        private final boolean useBlock;
        private final String listFixed;
        private final boolean listSetting;
        private final Set<String> known;

        Level(String where, boolean useBlock, String listFixed,
                boolean listSetting, Set<String> known) {
            this.where = where;
            this.useBlock = useBlock;
            this.listFixed = listFixed;
            this.listSetting = listSetting;
            this.known = known;
        }

        /** Known keys at this level. */
        public Set<String> known() {
            return known;
        }

        String where(String blockId, String settingPath) {
            return where.replace("{b}", blockId).replace("{s}", settingPath);
        }

        String behaviorId(String blockId) {
            return useBlock ? blockId : "";
        }

        String listName(String settingPath) {
            return listSetting ? settingPath : listFixed;
        }
    }

    private UnknownKeys() {
    }

    /**
     * Flag the keys at one level that fall outside its
     * universe, in file order. Null keys stay silent.
     */
    public static List<Diagnostic> check(String modifierId, Level level, String blockId,
            String settingPath, Collection<String> actual) {
        List<Diagnostic> found = new ArrayList<>();
        if (actual == null) {
            return found;
        }
        for (String key : actual) {
            if (key != null && !level.known().contains(key)) {
                found.add(new Diagnostic(
                        new Provenance(modifierId, level.behaviorId(blockId),
                                level.listName(settingPath), -1),
                        -1, Severity.WARNING, DiagnosticCode.W_UNKNOWN_KEY,
                        "Unknown key '" + key + "' in "
                                + level.where(blockId, settingPath) + "; ignoring it."));
            }
        }
        return found;
    }
}
