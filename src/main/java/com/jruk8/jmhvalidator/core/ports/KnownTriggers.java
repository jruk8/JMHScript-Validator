package com.jruk8.jmhvalidator.core.ports;

import java.util.Collection;
import java.util.Set;

/**
 * Trigger registry port. The engine owns the trigger list
 * (it defines validity by dispatching events); the validator
 * only reads it, in process from the engine adapter or out of
 * process from the exported registry JSON. Matching trims
 * and ignores case, mirroring the engine dispatcher. The
 * core never hardcodes trigger names.
 */
public interface KnownTriggers {
    /** Trigger names the engine dispatches. */
    Set<String> triggers();

    /** True when the engine dispatches this trigger name. */
    default boolean isKnown(String trigger) {
        if (trigger == null) {
            return false;
        }
        String want = trigger.trim();
        for (String known : triggers()) {
            if (known.equalsIgnoreCase(want)) {
                return true;
            }
        }
        return false;
    }

    /** Fixed registry for tests and adapters. */
    static KnownTriggers of(Collection<String> triggers) {
        Set<String> copy = Set.copyOf(triggers);
        return () -> copy;
    }
}
