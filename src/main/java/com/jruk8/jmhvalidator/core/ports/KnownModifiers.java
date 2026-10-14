package com.jruk8.jmhvalidator.core.ports;

import java.util.Collection;
import java.util.Set;

/**
 * Known modifier ids port. The engine owns the id universe (its
 * bundled defaults plus the data-folder files it loaded); the
 * validator only reads it, in process from the engine adapter or
 * out of process from the exported defaults manifest. The core
 * never hardcodes modifier ids.
 */
public interface KnownModifiers {
    /** Modifier ids preset members may reference. */
    Set<String> ids();

    /** True when this modifier id is known. */
    default boolean isKnown(String id) {
        return id != null && ids().contains(id);
    }

    /** Fixed id set for tests and adapters. */
    static KnownModifiers of(Collection<String> ids) {
        Set<String> copy = Set.copyOf(ids);
        return () -> copy;
    }
}
