package com.jruk8.jmhvalidator.core.model;

import java.util.List;
import java.util.Map;

/**
 * Minimal behavior model for validation: block id to command
 * lists to lines. Plain maps only, so the file adapter (YAML)
 * and the engine adapter (config objects) both map into it
 * without dragging their frameworks along.
 */
public record BehaviorFile(Map<String, BehaviorBlock> blocks) {
    /** One behavior block: list name to command lines. */
    public record BehaviorBlock(Map<String, List<String>> lists) {
    }

    /** Empty model for files with no behaviors. */
    public static BehaviorFile empty() {
        return new BehaviorFile(Map.of());
    }
}
