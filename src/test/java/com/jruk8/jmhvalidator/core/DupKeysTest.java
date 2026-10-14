package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Duplicate-key messages: root versus nested shape, plus assembly order. */
class DupKeysTest {

    @Test
    void rootMessageNamesKeyAndLine() {
        assertEquals(
                "Duplicate YAML key 'config' in the file root (line 12); keeping the last.",
                DupKeys.message("", "config", 12));
    }

    @Test
    void nestedMessageNamesPathKeyAndLine() {
        assertEquals(
                "Duplicate YAML key 'rate' in 'config.settings' (line 30); keeping the last.",
                DupKeys.message("config.settings", "rate", 30));
    }

    @Test
    void diagnosticsWarnInHitOrder() {
        List<Diagnostic> found = DupKeys.diagnostics("fog",
                List.of(new DupKeys.Hit("", "config", 12),
                        new DupKeys.Hit("config.settings", "rate", 30)));

        assertEquals(2, found.size());
        assertEquals(DiagnosticCode.W_DUPLICATE_KEY, found.get(0).code());
        assertEquals(Severity.WARNING, found.get(0).severity());
        assertEquals("Duplicate YAML key 'config' in the file root (line 12); keeping the last.",
                found.get(0).message());
        assertEquals("config.settings", found.get(1).provenance().listName());
        assertEquals(29, found.get(1).provenance().lineIndex());
    }

    @Test
    void nullHitsStaySilent() {
        assertTrue(DupKeys.diagnostics("fog", null).isEmpty());
    }
}
