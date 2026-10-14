package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.OptionsNode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Option-value sweep: every scalar under a behavior block's
 * {@code options} and {@code on-start} must be usable where
 * the engine consumes it. The engine binds these leniently
 * (its loader stringifies scalars into String fields and
 * parses numeric strings strictly, without trimming) and
 * then clamps or defaults bad values silently, so each
 * silent coercion warns here with the effective value.
 * Positions the engine fails to bind (non-numeric strings
 * and booleans in numeric fields, fractional numbers in
 * integer fields, maps and lists anywhere) stay silent
 * here; the shape checks own those.
 */
public final class TriggerParams {

    /** Interval seconds the engine assumes when unset. */
    private static final double DEFAULT_INTERVAL = 60.0;

    private TriggerParams() {
    }

    /** Check one block's option values, in documented order. */
    public static List<Diagnostic> check(String modifierId, String blockId,
            OptionsNode options) {
        List<Diagnostic> found = new ArrayList<>();
        if (options == null) {
            return found;
        }
        checkScope(found, modifierId, blockId, "options.interval-settings",
                options.intervalBehavior());
        checkScope(found, modifierId, blockId, "options.success-chance",
                options.chanceBehavior());
        checkScope(found, modifierId, blockId, "options.execution.pick-random",
                options.pickBehavior());
        checkSelection(found, modifierId, blockId, options.selection());
        checkPreStartOrder(found, modifierId, blockId, options.preStartOrder());
        Double interval = asDouble(options.interval());
        checkInterval(found, modifierId, blockId, interval);
        checkDeviation(found, modifierId, blockId, options, interval);
        checkChance(found, modifierId, blockId, options.chance());
        checkDelay(found, modifierId, blockId, options.delay());
        checkCount(found, modifierId, blockId, options.pickCount());
        return found;
    }

    private static void checkScope(List<Diagnostic> found, String modifierId,
            String blockId, String where, Object raw) {
        String text = asText(raw);
        if (text == null || isOneOf(text, "PER_INVOKE", "PER_EXECUTOR")) {
            return;
        }
        found.add(option(modifierId, blockId, where,
                "Unknown behavior '" + text.trim() + "' in behavior '"
                        + blockId + "' " + where + "; using PER_INVOKE."));
    }

    private static void checkSelection(List<Diagnostic> found, String modifierId,
            String blockId, Object raw) {
        String text = asText(raw);
        if (text == null || isOneOf(text, "IN_ORDER", "PICK_RANDOM")) {
            return;
        }
        found.add(option(modifierId, blockId, "options.execution",
                "Unknown selection '" + text.trim() + "' in behavior '"
                        + blockId + "' options.execution; using IN_ORDER."));
    }

    private static void checkPreStartOrder(List<Diagnostic> found, String modifierId,
            String blockId, Object raw) {
        String text = asText(raw);
        if (text == null || isOneOf(text, "BEFORE", "AFTER")) {
            return;
        }
        found.add(option(modifierId, blockId, "on-start",
                "Unknown pre-start-order '" + text.trim() + "' in behavior '"
                        + blockId + "' on-start; running BEFORE."));
    }

    private static void checkInterval(List<Diagnostic> found, String modifierId,
            String blockId, Double interval) {
        if (interval == null) {
            return;
        }
        if (interval.isNaN()) {
            found.add(option(modifierId, blockId, "options.interval-settings",
                    "Interval in behavior '" + blockId
                            + "' options.interval-settings is not a number;"
                            + " firing every tick."));
        } else if (interval < 0.0) {
            found.add(option(modifierId, blockId, "options.interval-settings",
                    "Negative interval " + interval + " in behavior '" + blockId
                            + "' options.interval-settings disables the behavior."));
        }
    }

    private static void checkDeviation(List<Diagnostic> found, String modifierId,
            String blockId, OptionsNode options, Double interval) {
        Double deviation = asDouble(options.deviation());
        if (deviation == null) {
            return;
        }
        String where = "options.interval-settings";
        if (deviation.isNaN()) {
            found.add(option(modifierId, blockId, where,
                    "Deviation in behavior '" + blockId + "' " + where
                            + " is not a number; using 0."));
            return;
        }
        if (deviation < 0.0) {
            found.add(option(modifierId, blockId, where,
                    "Negative deviation " + deviation + " in behavior '"
                            + blockId + "' " + where + "; using 0."));
            return;
        }
        if (interval == null) {
            if (deviation > DEFAULT_INTERVAL) {
                found.add(option(modifierId, blockId, where,
                        "Deviation " + deviation + " in behavior '" + blockId
                                + "' " + where + " is above the default interval "
                                + DEFAULT_INTERVAL + "; clamped to "
                                + DEFAULT_INTERVAL + "."));
            }
            return;
        }
        if (interval.isNaN() || interval <= 0.0) {
            if (deviation > 0.0) {
                found.add(option(modifierId, blockId, where,
                        "Deviation " + deviation + " in behavior '" + blockId
                                + "' " + where + " has no effect with interval "
                                + interval + "; using 0."));
            }
            return;
        }
        if (deviation > interval) {
            found.add(option(modifierId, blockId, where,
                    "Deviation " + deviation + " in behavior '" + blockId
                            + "' " + where + " is above interval " + interval
                            + "; clamped to " + interval + "."));
        }
    }

    private static void checkChance(List<Diagnostic> found, String modifierId,
            String blockId, Object raw) {
        Double chance = asDouble(raw);
        if (chance == null) {
            return;
        }
        String where = "options.success-chance";
        if (chance.isNaN()) {
            found.add(option(modifierId, blockId, where,
                    "Chance in behavior '" + blockId + "' " + where
                            + " is not a number; using 1.0."));
        } else if (chance < 0.0 || chance > 1.0) {
            double clamped = Math.min(1.0, Math.max(0.0, chance));
            found.add(option(modifierId, blockId, where,
                    "Chance " + chance + " in behavior '" + blockId + "' "
                            + where + " is outside 0.0-1.0; clamped to "
                            + clamped + "."));
        }
    }

    private static void checkDelay(List<Diagnostic> found, String modifierId,
            String blockId, Object raw) {
        Long delay = asLong(raw);
        if (delay == null || delay >= 0L) {
            return;
        }
        found.add(option(modifierId, blockId, "options",
                "Negative delay " + delay + " in behavior '" + blockId
                        + "' options behaves like 0."));
    }

    private static void checkCount(List<Diagnostic> found, String modifierId,
            String blockId, Object raw) {
        Long count = asLong(raw);
        if (count == null || count < Integer.MIN_VALUE || count > Integer.MAX_VALUE) {
            return;
        }
        if (count < 1L) {
            found.add(option(modifierId, blockId, "options.execution.pick-random",
                    "Count " + count + " in behavior '" + blockId
                            + "' options.execution.pick-random behaves like 1."));
        }
    }

    private static Diagnostic option(String modifierId, String blockId,
            String where, String message) {
        return new Diagnostic(new Provenance(modifierId, blockId, where, -1), -1,
                Severity.WARNING, DiagnosticCode.W_OPTION_VALUE, message);
    }

    private static boolean isOneOf(String text, String first, String second) {
        return text.trim().equalsIgnoreCase(first) || text.trim().equalsIgnoreCase(second);
    }

    /**
     * Engine String-field semantics: scalars stringify, maps
     * and lists fail the bind (owned by the shape checks).
     */
    private static String asText(Object raw) {
        if (raw == null || raw instanceof Map || raw instanceof List) {
            return null;
        }
        return String.valueOf(raw);
    }

    /**
     * Engine Double-field semantics: numbers convert, numeric
     * strings parse strictly, anything else fails the bind
     * (owned by the shape checks).
     */
    private static Double asDouble(Object raw) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException unparseable) {
                return null;
            }
        }
        return null;
    }

    /**
     * Engine Long-field semantics: integers convert, whole
     * doubles convert, numeric strings parse strictly, and
     * anything else (including fractional numbers) fails the
     * bind (owned by the shape checks).
     */
    private static Long asLong(Object raw) {
        if (raw instanceof Integer || raw instanceof Long
                || raw instanceof Short || raw instanceof Byte) {
            return ((Number) raw).longValue();
        }
        if (raw instanceof Number number) {
            double value = number.doubleValue();
            if (Double.isNaN(value) || Double.isInfinite(value)
                    || value != Math.floor(value) || value < Long.MIN_VALUE
                    || value > Long.MAX_VALUE) {
                return null;
            }
            return (long) value;
        }
        if (raw instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException unparseable) {
                return null;
            }
        }
        return null;
    }
}
