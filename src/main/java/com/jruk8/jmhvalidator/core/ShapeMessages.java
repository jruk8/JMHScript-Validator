package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.Severity;

/**
 * Tolerated-shape findings: node shapes the engine loads
 * past (skipping, defaulting, or stringifying) instead of
 * failing the file. Each names what the engine did with
 * the node. The commands shapes are mirrored: the engine
 * load sweep reports the identical text through these same
 * factories. Shapes the engine cannot see (nulls, which its
 * YAML layer drops) and settings shapes (which its serdes
 * skips without a hook) are validator-only.
 */
public final class ShapeMessages {

    /**
     * Synthetic list name the loader invents for
     * list-shaped commands blocks, pinned by test.
     */
    public static final String SYNTHETIC_LIST = "$$__value__$$";

    private ShapeMessages() {
    }

    /** Scalar commands block, ignored; mirrored by the engine. */
    public static Diagnostic ignoredCommands(String modifierId, String blockId) {
        return shape(modifierId, blockId, "", -1,
                "commands in behavior '" + blockId + "' must be a map; ignoring it.");
    }

    /** List commands block, read as one list; mirrored by the engine. */
    public static Diagnostic listedCommands(String modifierId, String blockId) {
        return shape(modifierId, blockId, "", -1,
                "commands in behavior '" + blockId
                        + "' must be a map; reading the entries as one list.");
    }

    /** Scalar or map commands list, skipped; mirrored by the engine. */
    public static Diagnostic skippedList(String modifierId, String blockId,
            String listName) {
        return shape(modifierId, blockId, listName, -1,
                "commands list '" + listName + "' in behavior '" + blockId
                        + "' must be a list; ignoring it.");
    }

    /** Null commands list, skipped; validator-only. */
    public static Diagnostic nullList(String modifierId, String blockId,
            String listName) {
        return shape(modifierId, blockId, listName, -1,
                "commands list '" + listName + "' in behavior '" + blockId
                        + "' has no value; ignoring it.");
    }

    /** Null behavior block, dropped; validator-only. */
    public static Diagnostic nullBlock(String modifierId, String blockId) {
        return shape(modifierId, blockId, "", -1,
                "behavior '" + blockId + "' has no value; ignoring it.");
    }

    /** Scalar command line, stringified; mirrored by the engine. */
    public static Diagnostic coercedLine(String modifierId, String blockId,
            String listName, int index, Object raw) {
        return shape(modifierId, blockId, listName, index,
                "command line " + raw + " in behavior '" + blockId + "' commands '"
                        + listName + "' is not a string; reading it as '"
                        + raw + "'.");
    }

    /** Null command line, skipped; validator-only. */
    public static Diagnostic nullLine(String modifierId, String blockId,
            String listName, int index) {
        return shape(modifierId, blockId, listName, index,
                "command line in behavior '" + blockId + "' commands '" + listName
                        + "' has no value; ignoring it.");
    }

    /** Explicit empty commands list; validator-only. */
    public static Diagnostic emptyList(String modifierId, String blockId,
            String listName) {
        return shape(modifierId, blockId, listName, -1,
                "commands list '" + listName + "' in behavior '" + blockId
                        + "' is empty; it adds no lines.");
    }

    /** Null runs-on entry, never matching; validator-only. */
    public static Diagnostic nullTrigger(String modifierId, String blockId, int index) {
        return shape(modifierId, blockId, "runs-on", index,
                "runs-on entry in behavior '" + blockId
                        + "' has no value; it matches nothing.");
    }

    /** Null preset member, never matching; validator-only. */
    public static Diagnostic nullMember(String modifierId, String listName,
            int index, String kind) {
        return shape(modifierId, "", listName, index,
                "preset member in '" + listName + "' has no value; it matches no "
                        + kind + ".");
    }

    /** Non-map advanced block, defaulted; validator-only. */
    public static Diagnostic skippedAdvanced(String modifierId) {
        return shape(modifierId, "", "", -1,
                "'config.advanced' must be a map; ignoring it.");
    }

    /** Non-map settings block, emptied; validator-only. */
    public static Diagnostic skippedSettings(String modifierId) {
        return shape(modifierId, "", "", -1,
                "'config.settings' must be a map; ignoring it.");
    }

    /** Non-map settings entry, dropped; validator-only. */
    public static Diagnostic settingNotMap(String modifierId, String path) {
        return setting(modifierId, path, -1,
                "'config.settings." + path + "' must be a map; ignoring it.");
    }

    /** Non-map setting def, dropped; validator-only. */
    public static Diagnostic settingDefNotMap(String modifierId, String path) {
        return setting(modifierId, path, -1,
                "'config.settings." + path + ".def' must be a map; ignoring it.");
    }

    /** Non-map setting value spec, dropped; validator-only. */
    public static Diagnostic settingSpecNotMap(String modifierId, String path) {
        return setting(modifierId, path, -1,
                "'config.settings." + path + ".def.value' must be a map; ignoring it.");
    }

    /** Non-list setting folder, dropped; validator-only. */
    public static Diagnostic settingFolderNotList(String modifierId, String path) {
        return setting(modifierId, path, -1,
                "'config.settings." + path + ".def.folder' must be a list; ignoring it.");
    }

    /** Non-single-key folder entry, dropped; validator-only. */
    public static Diagnostic folderItemNotMap(String modifierId, String path, int index) {
        return setting(modifierId, path, index,
                "folder entry in 'config.settings." + path
                        + "' must be a single-key map; ignoring it.");
    }

    /** Non-map folder entry value, dropped; validator-only. */
    public static Diagnostic folderValueNotMap(String modifierId, String path) {
        return setting(modifierId, path, -1,
                "'config.settings." + path + "' must be a map; ignoring it.");
    }

    /** Non-map setting meta, dropped; validator-only. */
    public static Diagnostic settingMetaNotMap(String modifierId, String path) {
        return setting(modifierId, path, -1,
                "'config.settings." + path + ".def.meta' must be a map; ignoring it.");
    }

    private static Diagnostic shape(String modifierId, String blockId,
            String listName, int lineIndex, String message) {
        return new Diagnostic(new Provenance(modifierId, blockId, listName, lineIndex),
                -1, Severity.WARNING, DiagnosticCode.W_SHAPE, message);
    }

    private static Diagnostic setting(String modifierId, String path,
            int lineIndex, String message) {
        return new Diagnostic(new Provenance(modifierId, "", path, lineIndex),
                -1, Severity.WARNING, DiagnosticCode.W_SHAPE, message);
    }
}
