package com.jruk8.jmhvalidator.core.model;

/**
 * Stable diagnostic codes for the JSON contract. Codes never
 * change meaning; new checks add codes, retired checks keep
 * theirs reserved. Consumers map codes, never message text.
 * Each code is documented in docs/error-catalog.md (pinned by
 * a test); message text stays byte-identical to the engine.
 */
public enum DiagnosticCode {
    /** Stray closing bracket with no opener. */
    E_BRACKET_UNMATCHED_CLOSE("Unmatched angle bracket"),
    /** Opener left hanging at end of line. */
    E_BRACKET_UNCLOSED_OPEN("Unclosed angle bracket"),
    /** Fatal tag syntax (shape, arity, quotes, types). */
    E_SYNTAX("Malformed tag arguments"),
    /** First token names no known command. */
    E_UNKNOWN_ROOT("Unknown command"),
    /** Give-shape item names no known material. */
    E_UNKNOWN_ITEM("Unknown item"),
    /** Tag name outside the registry and def scope. */
    W_UNKNOWN_TAG("Unknown tag"),
    /** Random-pick item skipped at runtime. */
    W_RANDOM_PICK_ITEM("Skipped pick item"),
    /** All-players filter outside letters-only. */
    W_ALL_PLAYERS_FILTER("Weak filter"),
    /** File cannot be read. */
    E_FILE_READ("Unreadable file"),
    /** File is not parseable YAML. */
    E_YAML_PARSE("Unparseable YAML"),
    /** Behavior section has the wrong shape (map/list expected). */
    E_YAML_SHAPE("Unexpected YAML shape"),
    /** Behavior command entry is not a string line. */
    E_YAML_LINE_TYPE("Non-string command line");

    private final String title;

    DiagnosticCode(String title) {
        this.title = title;
    }

    /** Short human title for catalogs and summaries. */
    public String title() {
        return title;
    }
}
