package com.jruk8.jmhvalidator.core.model;

/**
 * Diagnostic severity. Errors are fatal (the line cannot run
 * as written); warnings flag lines the engine degrades on
 * (unknown tags left untouched, skipped items, fallbacks).
 */
public enum Severity {
    /** Fatal: brackets, tag syntax, unknown roots or items. */
    ERROR,
    /** Non-fatal: unknown tags, skipped items, weak filters. */
    WARNING
}
