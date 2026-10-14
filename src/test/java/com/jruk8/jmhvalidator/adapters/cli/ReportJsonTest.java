package com.jruk8.jmhvalidator.adapters.cli;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the report bytes: golden files and downstream consumers
 * depend on field order, indentation, and escaping. A format
 * change bumps REPORT_VERSION with the bytes.
 */
class ReportJsonTest {
    @Test
    void reportRendersExactBytes() throws Exception {
        Diagnostic fatal = new Diagnostic(new Provenance("swap", "main", "all", 1),
                15, Severity.ERROR, DiagnosticCode.E_SYNTAX, "Tag <min> needs two numbers.");
        Diagnostic warning = new Diagnostic(new Provenance("swap", "main", "all", 0),
                -1, Severity.WARNING, DiagnosticCode.W_UNKNOWN_TAG,
                "Unknown tag '<bogus>', left untouched at runtime.");

        String rendered = ReportJson.report("dev-test", "JManhunt", "rev-abc",
                List.of(new ReportJson.FileReport("mods/swap.yml", List.of(fatal, warning))));

        assertEquals(expectedReport(), rendered);
    }

    private String expectedReport() throws Exception {
        return Files.readString(Paths.get("src/test/resources/expected-report.json"),
                StandardCharsets.UTF_8);
    }

    @Test
    void escapeCoversQuotesAndControls() {
        assertEquals("\\\"\\\\\\n\\r\\t\\u0001", ReportJson.escape("\"\\\n\r\t\u0001"));
    }
}
