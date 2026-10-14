package com.jruk8.jmhvalidator.core.share;

import java.util.List;
import java.util.Map;

/**
 * Plain share-string model: the codec data without engine
 * (Okaeri) or file (YAML) bindings. Every component is nullable;
 * null means absent, exactly like a missing JSON key, so exports
 * stay byte-stable across decode and re-export.
 */
public final class ShareModel {
    private ShareModel() {
    }

    /** One modifier payload: meta, config, and behaviors. */
    public record Modifier(Meta meta, Map<String, Object> settings,
            Map<String, Object> advanced, Map<String, Behavior> behaviors) {
    }

    /** One preset payload: meta plus member and nested ids. */
    public record Preset(Meta meta, List<String> modifiers, List<String> presets) {
    }

    /** Display meta shared by modifiers and presets. */
    public record Meta(String name, String description, String icon,
            String author, Visibility visibility) {
    }

    /** Broadcast visibility for modifier exports. */
    public record Visibility(String icon, String color, Boolean broadcast) {
    }

    /** One behavior block: triggers, order, options, commands. */
    public record Behavior(List<String> runsOn, String preStartOrder,
            Options options, Map<String, List<String>> commands) {
    }

    /** Execution options for one behavior block. */
    public record Options(Interval interval, Chance chance,
            Execution execution, Long delay) {
    }

    /** Interval timing for recurring behaviors. */
    public record Interval(Double interval, Double deviation, String behavior) {
    }

    /** Success chance gate for one behavior block. */
    public record Chance(Double chance, String behavior) {
    }

    /** List execution selection for one behavior block. */
    public record Execution(String selection, PickRandom pickRandom) {
    }

    /** Pick-random subset for list execution. */
    public record PickRandom(Integer count, String behavior) {
    }
}
