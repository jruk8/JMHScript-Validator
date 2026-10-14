package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.ArrayList;
import java.util.List;

/**
 * Duplicate YAML keys: one mapping repeats a key, so the
 * loader silently keeps the last value. Hits are found by a
 * SnakeYAML compose walk (see the adapters scanner, which is
 * the only place SnakeYAML types may appear); this class owns
 * the hit shape, the message text, and the diagnostics, so the
 * engine warning stays byte-identical by delegation.
 */
public final class DupKeys {

    /** One repeated key: dotted mapping path, key, 1-based line. */
    public record Hit(String path, String key, int line) {
    }

    private DupKeys() {
    }

    /**
     * Message for one repeated key. The engine warning carries
     * this text verbatim through its duplicate-keys pass.
     */
    public static String message(String path, String key, int line) {
        String where = path == null || path.isEmpty() ? "the file root" : "'" + path + "'";
        return "Duplicate YAML key '" + key + "' in " + where
                + " (line " + line + "); keeping the last.";
    }

    /**
     * One warning per hit, in hit order: the loader keeps the
     * last value and continues, so findings warn, never error.
     */
    public static List<Diagnostic> diagnostics(String modifierId, List<Hit> hits) {
        List<Diagnostic> found = new ArrayList<>();
        if (hits == null) {
            return found;
        }
        for (Hit hit : hits) {
            String path = hit.path() == null ? "" : hit.path();
            found.add(new Diagnostic(
                    new Provenance(modifierId, "", path, hit.line() - 1),
                    -1, Severity.WARNING, DiagnosticCode.W_DUPLICATE_KEY,
                    message(path, hit.key(), hit.line())));
        }
        return found;
    }
}
