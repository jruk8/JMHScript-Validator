package com.jruk8.jmhvalidator.core.model;

import java.util.List;

/**
 * Minimal settings model for style checks: one `config.settings`
 * tree as folders with children and leaves. Plain records only,
 * so the file adapter maps YAML into it without dragging
 * SnakeYAML along. Folder semantics match the engine loader: a
 * map with no `value` key plus a `def.folder` list is a folder;
 * anything else map-shaped is a leaf. Leaf specs carry the
 * `def.value` block for default checks; folders and
 * spec-less leaves hold a null spec.
 */
public record SettingsTree(List<SettingsNode> roots) {
    /** One settings entry: folder with children, or leaf. */
    public record SettingsNode(String name, boolean folder, List<SettingsNode> children,
            SettingSpec spec) {
    }

    /** One leaf's `def.value` block: raw type, default, and allowed. */
    public record SettingSpec(String type, Object defaultValue, Object allowed) {
    }

    /** Empty model for files with no settings. */
    public static SettingsTree empty() {
        return new SettingsTree(List.of());
    }
}
