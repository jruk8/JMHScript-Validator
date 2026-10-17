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
    /** Paid tag under free-edition validation. */
    W_PAID_TAG("Paid tag"),
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
    E_YAML_LINE_TYPE("Non-string command line"),
    /** Intercept tag outside an interceptable block. */
    E_INTERCEPT_CONTEXT("Intercept outside interceptable block"),
    /** Settings level holds more than 5 folders. */
    W_SETTINGS_FOLDERS("Too many folders"),
    /** Leaf beside folders that is not `enabled`, or a flat root. */
    W_SETTINGS_STRAY_LEAF("Misplaced settings leaf"),
    /** Settings level holds more than 7 leafs. */
    W_SETTINGS_LEAFS("Too many leafs"),
    /** Preset member names no known modifier id. */
    W_UNKNOWN_MODIFIER("Unknown modifier"),
    /** Later same-id file skipped; first in load order wins. */
    W_DUPLICATE_ID("Duplicate id"),
    /** Duplicate sibling setting names; the block drops. */
    W_DUPLICATE_SETTING("Duplicate setting names"),
    /** Setting reference names no defined leaf setting. */
    W_UNKNOWN_SETTING("Unknown setting"),
    /** Runs-on trigger names no engine-dispatched event. */
    W_UNKNOWN_TRIGGER("Unknown trigger"),
    /** YAML key names nothing the engine reads. */
    W_UNKNOWN_KEY("Unknown key"),
    /** Setting spec type names no known setting type. */
    W_UNKNOWN_SETTING_TYPE("Unknown setting type"),
    /** Setting default falls outside its allowed range. */
    W_DEFAULT_OUTSIDE_ALLOWED("Default outside allowed"),
    /** Live setting value falls outside its type or allowed range. */
    W_VALUE_INVALID("Value invalid"),
    W_REDUNDANT_VALUE("Redundant value"),
    E_VALUE_DIFFERS("Value differs from default"),
    /** One YAML mapping repeats a key; the loader keeps the last. */
    W_DUPLICATE_KEY("Duplicate YAML key"),
    /** Scanned id reuses an engine-bundled id without being its copy. */
    W_SHADOWED_ID("Shadowed bundled id"),
    /** Runblock names no defined behavior block of the modifier. */
    W_UNKNOWN_BEHAVIOR("Unknown behavior"),
    /** Runblock chain recurses into its own call path. */
    W_BEHAVIOR_CYCLE("Behavior cycle"),
    /** Setting defines both value and folder; the value wins. */
    W_AMBIGUOUS_SETTING("Ambiguous setting"),
    /** Leaf setting no behavior line reads. */
    W_UNCALLED_SETTING("Uncalled setting"),
    /** Behavior block no trigger fires and no runblock calls. */
    W_UNCALLED_BEHAVIOR("Uncalled behavior"),
    /** Behavior option value the engine clamps or defaults silently. */
    W_OPTION_VALUE("Option value"),
    /** Node shape the engine tolerates or drops silently. */
    W_SHAPE("Shape tolerated"),
    /** Settings folder without a display icon. */
    W_FOLDER_ICON("Folder icon"),
    /** Commands list no dispatch key reads. */
    W_UNKNOWN_LIST("Unknown list"),
    /** Trigger the engine removed or renamed. */
    W_REMOVED_TRIGGER("Removed trigger"),
    /** Tag the engine removed; the message names the replacement. */
    W_REMOVED_TAG("Removed tag");

    private final String title;

    DiagnosticCode(String title) {
        this.title = title;
    }

    /** Short human title for catalogs and summaries. */
    public String title() {
        return title;
    }
}
