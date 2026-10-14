package com.jruk8.jmhvalidator.core.model;

/**
 * Diagnostic severity. Errors are fatal (the line cannot run
 * as written); warnings flag lines the engine degrades on
 * (unknown tags left untouched, skipped items, fallbacks)
 * and settings style the loader accepts but authors should
 * fix (crowded levels, misplaced leaves).
 */
public enum Severity {
    /** Fatal: brackets, tag syntax, unknown roots or items. */
    ERROR,
    /** Non-fatal: unknown tags, skipped items, weak filters, settings style. */
    WARNING
}
