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

    /** Lowercase tag names the paid edition gates; empty when unknown. */
    default Set<String> paidTags() {
        return Set.of();
    }

    /** True when the engine dispatches this tag name. */
    default boolean isKnown(String name) {
        return name != null && tags().contains(name);
    }

    /** True when the paid edition gates this tag name. */
    default boolean isPaid(String name) {
        return name != null && paidTags().contains(name);
    }

    /** Fixed registry for tests and adapters. */
    static TagRegistry of(Collection<String> tags) {
        return of(tags, Set.of());
    }

    /** Fixed registry with paid names for tests and adapters. */
    static TagRegistry of(Collection<String> tags, Collection<String> paid) {
        Set<String> copy = Set.copyOf(tags);
        Set<String> paidCopy = Set.copyOf(paid);
        return new TagRegistry() {
            @Override
            public Set<String> tags() {
                return copy;
            }

            @Override
            public Set<String> paidTags() {
                return paidCopy;
            }
        };
    }
}
