package com.jruk8.jmhvalidator.core.model;

import java.util.List;
import java.util.Map;

/**
 * Minimal behavior model for validation: block id to runs-on
 * triggers and command lists to lines. Plain maps only, so
 * the file adapter (YAML) and the engine adapter (config
 * objects) both map into it without dragging their
 * frameworks along.
 */
public record BehaviorFile(Map<String, BehaviorBlock> blocks) {
    /** One behavior block: runs-on triggers, command lines, and option values. */
    public record BehaviorBlock(Map<String, List<String>> lists, List<String> runsOn,
            OptionsNode options) {
        /** Blocks without captured options read as option-less. */
        public BehaviorBlock(Map<String, List<String>> lists, List<String> runsOn) {
            this(lists, runsOn, OptionsNode.empty());
        }
    }

    /** Empty model for files with no behaviors. */
    public static BehaviorFile empty() {
        return new BehaviorFile(Map.of());
    }
}
