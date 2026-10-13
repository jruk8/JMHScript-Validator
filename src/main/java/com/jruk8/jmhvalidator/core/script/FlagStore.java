package com.jruk8.jmhvalidator.core.script;

import java.util.Optional;

/**
 * Flag name parsing for validation: trimmed, non-blank, quotes
 * parsed. Storage stays engine-side.
 */
public final class FlagStore {

    private FlagStore() {
    }


    /** Validates one flag name: trimmed, non-blank, quotes parsed. */
    public static Optional<String> parseName(String raw) {
        Optional<String> item = TagHeads.parsePickItem(raw);
        if (item.isEmpty() || item.get().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(item.get().strip());
    }
}
