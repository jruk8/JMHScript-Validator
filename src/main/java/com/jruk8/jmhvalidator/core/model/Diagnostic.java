package com.jruk8.jmhvalidator.core.model;

/**
 * One validation finding: severity, stable code, the engine's
 * message text verbatim, and the source location. Line and char
 * are 1-based like editors; a non-positive char stays
 * line-level, matching the engine's locator.
 */
public record Diagnostic(Provenance provenance, int charOneBased, Severity severity,
        DiagnosticCode code, String message) {
    /**
     * Classic rendering: message plus the engine's location
     * clause. Byte-identical to the pre-module load sweep, so
     * engine facades delegate without reformatting.
     */
    public String renderText() {
        return message + " at " + provenance.describe(charOneBased);
    }
}
