package com.jruk8.jmhvalidator.core.ports;

import java.util.Collection;
import java.util.List;
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

    /** Trigger names the engine removed or renamed. */
    default Set<String> removedTriggers() {
        return Set.of();
    }

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

    /** True when the engine removed or renamed this trigger name. */
    default boolean isRemoved(String trigger) {
        if (trigger == null) {
            return false;
        }
        String want = trigger.trim();
        for (String removed : removedTriggers()) {
            if (removed.equalsIgnoreCase(want)) {
                return true;
            }
        }
        return false;
    }

    /** Fixed registry for tests and adapters. */
    static KnownTriggers of(Collection<String> triggers) {
        return of(triggers, List.of());
    }

    /** Fixed registry with a removed set, for tests and adapters. */
    static KnownTriggers of(Collection<String> triggers, Collection<String> removed) {
        Set<String> known = Set.copyOf(triggers);
        Set<String> gone = Set.copyOf(removed);
        return new KnownTriggers() {
            @Override
            public Set<String> triggers() {
                return known;
            }

            @Override
            public Set<String> removedTriggers() {
                return gone;
            }
        };
    }
}
