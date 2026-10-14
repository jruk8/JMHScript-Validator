package com.jruk8.jmhvalidator.core.ports;

import java.util.Collection;
import java.util.Set;

/**
 * Interceptable-trigger port. The engine owns the interceptable
 * set (it defines validity by dispatching events); the validator
 * only reads it, in process from the engine adapter or out of
 * process from the exported registry JSON. Matching trims
 * and ignores case, mirroring the engine dispatcher. The
 * core never hardcodes trigger names.
 */
public interface InterceptableTriggers {
    /** Trigger names whose events carry an intercept context. */
    Set<String> triggers();

    /** True when this trigger name is interceptable. */
    default boolean isInterceptable(String trigger) {
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
    static InterceptableTriggers of(Collection<String> triggers) {
        Set<String> copy = Set.copyOf(triggers);
        return () -> copy;
    }
}
