package com.jruk8.jmhvalidator.core;

/**
 * Duplicate file-id messages: first in load order wins, later
 * same-id files skip. Single source of truth for the engine
 * load warning and the CLI duplicate-id finding.
 */
public final class DuplicateIds {

    private DuplicateIds() {
    }

    /**
     * Engine-identical skip message for one duplicate id.
     * Kind is the engine kind word ({@code modifier} or
     * {@code preset}); paths render as given.
     */
    public static String message(String kind, String id, String winner, String skipped) {
        return "Duplicate " + kind + " id '" + id + "': '" + winner
                + "' wins, skipping '" + skipped + "'.";
    }

    /**
     * Suffix when the duplicated id is also engine-bundled:
     * appended to {@link #message}, naming the override the
     * operator may not have intended.
     */
    public static String bundledNote(String kind, String id) {
        return " '" + id + "' is also an engine-bundled " + kind + " id.";
    }
}
