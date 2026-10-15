package com.jruk8.jmhvalidator.adapters.cli;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.List;

/** Text rendering behind the CLI report: one line per diagnostic. */
final class ReportText {
    private ReportText() {
    }

    static String renderText(List<ReportJson.FileReport> reports) {
        StringBuilder out = new StringBuilder();
        int errors = 0;
        int warnings = 0;
        int dirty = 0;
        for (ReportJson.FileReport report : reports) {
            if (!report.diagnostics().isEmpty()) {
                dirty++;
            }
            for (Diagnostic found : report.diagnostics()) {
                if (found.severity() == Severity.ERROR) {
                    errors++;
                } else {
                    warnings++;
                }
                out.append(textLine(report.file(), found)).append("\n");
            }
        }
        out.append("%d files: %d clean, %d with diagnostics (errors: %d, warnings: %d)%n"
                .formatted(reports.size(), reports.size() - dirty, dirty, errors, warnings));
        return out.toString();
    }

    static String textLine(String file, Diagnostic found) {
        String where = file;
        if (!found.provenance().behaviorId().isBlank()) {
            where += " " + found.provenance().behaviorId()
                    + "/" + found.provenance().listName()
                    + ":" + (found.provenance().lineIndex() + 1);
            if (found.charOneBased() > 0) {
                where += ":" + found.charOneBased();
            }
        } else if (!found.provenance().listName().isBlank()) {
            where += " " + found.provenance().listName();
        }
        return where + " " + found.severity().name().toLowerCase()
                + " " + found.code().name() + ": " + found.message();
    }
}
