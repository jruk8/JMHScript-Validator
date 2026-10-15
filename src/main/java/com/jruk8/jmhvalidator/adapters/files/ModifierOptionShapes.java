package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.TriggerParams;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import java.util.List;
import java.util.Map;

/** Option-subtree shape scan behind {@link ModifierYaml}: maps, numbers, and strings. */
final class ModifierOptionShapes {
    private ModifierOptionShapes() {
    }

    /**
     * Shape scan of one block's option subtrees: every
     * non-map subtree and non-binder scalar fails the engine
     * load, so each errors here. Bindability reuses the core
     * loader table; absent values stay silent.
     */
    static void readOptionShapes(String modifierId, String blockId,
            Map<?, ?> block, List<Diagnostic> early) {
        String base = "'behavior." + blockId + ".";
        Object onStart = block.get("on-start");
        if (onStart != null && !(onStart instanceof Map)) {
            early.add(ModifierYaml.shaped(modifierId, blockId, base + "on-start' must be a map"));
        } else if (onStart instanceof Map<?, ?> start) {
            mustBeString(early, modifierId, blockId,
                    start.get("pre-start-order"), base + "on-start.pre-start-order'");
        }
        if (!(block.get("options") instanceof Map<?, ?> options)) {
            if (block.get("options") != null) {
                early.add(ModifierYaml.shaped(modifierId, blockId, base + "options' must be a map"));
            }
            return;
        }
        readIntervalSettings(early, modifierId, blockId, options, base);
        readSuccessChance(early, modifierId, blockId, options, base);
        readExecution(early, modifierId, blockId, options, base);
        mustBeInteger(early, modifierId, blockId, options.get("delay"),
                base + "options.delay'", false);
    }

    /** Interval-settings subtree: interval, deviation, behavior. */
    private static void readIntervalSettings(List<Diagnostic> early, String modifierId,
            String blockId, Map<?, ?> options, String base) {
        if (!(options.get("interval-settings") instanceof Map<?, ?> settings)) {
            if (options.get("interval-settings") != null) {
                early.add(ModifierYaml.shaped(modifierId, blockId,
                        base + "options.interval-settings' must be a map"));
            }
            return;
        }
        mustBeNumber(early, modifierId, blockId, settings.get("interval"),
                base + "options.interval-settings.interval'");
        mustBeNumber(early, modifierId, blockId, settings.get("deviation"),
                base + "options.interval-settings.deviation'");
        mustBeString(early, modifierId, blockId, settings.get("behavior"),
                base + "options.interval-settings.behavior'");
    }

    /** Success-chance subtree: chance plus behavior. */
    private static void readSuccessChance(List<Diagnostic> early, String modifierId,
            String blockId, Map<?, ?> options, String base) {
        if (!(options.get("success-chance") instanceof Map<?, ?> success)) {
            if (options.get("success-chance") != null) {
                early.add(ModifierYaml.shaped(modifierId, blockId,
                        base + "options.success-chance' must be a map"));
            }
            return;
        }
        mustBeNumber(early, modifierId, blockId, success.get("chance"),
                base + "options.success-chance.chance'");
        mustBeString(early, modifierId, blockId, success.get("behavior"),
                base + "options.success-chance.behavior'");
    }

    /** Execution subtree: selection plus the pick-random pair. */
    private static void readExecution(List<Diagnostic> early, String modifierId,
            String blockId, Map<?, ?> options, String base) {
        if (!(options.get("execution") instanceof Map<?, ?> execution)) {
            if (options.get("execution") != null) {
                early.add(ModifierYaml.shaped(modifierId, blockId,
                        base + "options.execution' must be a map"));
            }
            return;
        }
        mustBeString(early, modifierId, blockId, execution.get("selection"),
                base + "options.execution.selection'");
        if (!(execution.get("pick-random") instanceof Map<?, ?> pick)) {
            if (execution.get("pick-random") != null) {
                early.add(ModifierYaml.shaped(modifierId, blockId,
                        base + "options.execution.pick-random' must be a map"));
            }
            return;
        }
        mustBeInteger(early, modifierId, blockId, pick.get("count"),
                base + "options.execution.pick-random.count'", true);
        mustBeString(early, modifierId, blockId, pick.get("behavior"),
                base + "options.execution.pick-random.behavior'");
    }

    private static void mustBeNumber(List<Diagnostic> early, String modifierId,
            String blockId, Object raw, String path) {
        if (raw != null && TriggerParams.asDouble(raw) == null) {
            early.add(ModifierYaml.shaped(modifierId, blockId, path + " must be a number"));
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
            early.add(ModifierYaml.shaped(modifierId, blockId, path + " must be an integer"));
        }
    }

    private static void mustBeString(List<Diagnostic> early, String modifierId,
            String blockId, Object raw, String path) {
        if (raw instanceof Map || raw instanceof List) {
            early.add(ModifierYaml.shaped(modifierId, blockId, path + " must be a string"));
        }
    }
}
