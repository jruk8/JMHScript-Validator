package com.jruk8.jmhvalidator.adapters.cli;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the report bytes: golden files and the JMH-Mods adapter
 * depend on field order, indentation, and escaping. A format
 * change bumps REPORT_VERSION with the bytes.
 */
class ReportJsonTest {
    @Test
    void reportRendersExactBytes() {
        Diagnostic fatal = new Diagnostic(new Provenance("swap", "main", "all", 1),
                15, Severity.ERROR, DiagnosticCode.E_SYNTAX, "Tag <min> needs two numbers.");
        Diagnostic warning = new Diagnostic(new Provenance("swap", "main", "all", 0),
                -1, Severity.WARNING, DiagnosticCode.W_UNKNOWN_TAG,
                "Unknown tag '<bogus>', left untouched at runtime.");

        String rendered = ReportJson.report("dev-test", "JManhunt", "rev-abc",
                List.of(new ReportJson.FileReport("mods/swap.yml", List.of(fatal, warning))));

        assertEquals("""
                {
                  "reportVersion": 1,
                  "validatorVersion": "dev-test",
                  "registry": {
                    "engine": "JManhunt",
                    "engineVersion": "rev-abc"
                  },
                  "files": [
                    {
                      "file": "mods/swap.yml",
                      "diagnostics": [
                        {
                          "behavior": "main",
                          "list": "all",
                          "line": 2,
                          "char": 15,
                          "severity": "error",
                          "code": "E_SYNTAX",
                          "message": "Tag <min> needs two numbers."
                        },
                        {
                          "behavior": "main",
                          "list": "all",
                          "line": 1,
                          "char": null,
                          "severity": "warning",
                          "code": "W_UNKNOWN_TAG",
                          "message": "Unknown tag '<bogus>', left untouched at runtime."
                        }
                      ]
                    }
                  ],
                  "summary": {
                    "files": 1,
                    "errors": 1,
                    "warnings": 1
                  }
                }
                """, rendered);
    }

    @Test
    void escapeCoversQuotesAndControls() {
        assertEquals("\\\"\\\\\\n\\r\\t\\u0001", ReportJson.escape("\"\\\n\r\t\u0001"));
    }
}
