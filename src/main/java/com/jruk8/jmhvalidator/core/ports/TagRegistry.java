package com.jruk8.jmhvalidator.core.ports;

import java.util.Collection;
import java.util.Set;

/**
 * Tag registry port. The engine owns the tag list (it defines
 * validity by executing tags); the validator only reads it, in
 * process from the engine adapter or out of process from the
 * exported registry JSON. The core never hardcodes tag names.
 */
public interface TagRegistry {
    /** Lowercase tag names the engine dispatches. */
    Set<String> tags();

    /** True when the engine dispatches this tag name. */
    default boolean isKnown(String name) {
        return name != null && tags().contains(name);
    }

    /** Fixed registry for tests and adapters. */
    static TagRegistry of(Collection<String> tags) {
        Set<String> copy = Set.copyOf(tags);
        return () -> copy;
    }
}
