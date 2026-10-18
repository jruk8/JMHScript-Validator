package com.jruk8.jmhvalidator.adapters.cli;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.List;

/** Text rendering behind the CLI report: one line per diagnostic. */
final class ReportText {
    private ReportText() {
    }

    static String renderText(List<ReportJson.FileReport> reports) {
        return renderText(reports, List.of());
    }

    /**
     * Text rendering with per-file premium markers: after each
     * file's diagnostics, premium files print their path plus
     * {@code (premium)}. Premium flags run parallel to the
     * reports; short lists read as free.
     */
    static String renderText(List<ReportJson.FileReport> reports, List<Boolean> premiums) {
        StringBuilder out = new StringBuilder();
        int errors = 0;
        int warnings = 0;
        int dirty = 0;
        for (int index = 0; index < reports.size(); index++) {
            ReportJson.FileReport report = reports.get(index);
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
            if (index < premiums.size() && premiums.get(index)) {
                out.append(report.file()).append(" (premium)\n");
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
