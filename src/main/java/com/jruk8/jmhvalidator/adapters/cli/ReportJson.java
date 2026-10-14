package com.jruk8.jmhvalidator.adapters.cli;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.List;

/**
 * Versioned JSON report writer (reportVersion 1). Field order
 * and escaping are pinned by {@code ReportJsonTest}: golden
 * files and downstream consumers both depend on the shape.
 * Consumers assert reportVersion and map codes; validatorVersion
 * is informational and excluded from parity comparison.
 */
public final class ReportJson {
    /** Report contract version asserted by every consumer. */
    public static final int REPORT_VERSION = 1;

    /** One file's diagnostics for the report. */
    public record FileReport(String file, List<Diagnostic> diagnostics) {
    }

    private ReportJson() {
    }

    /** Render a full report over the given files, in order. */
    public static String report(String validatorVersion, String engine,
            String engineVersion, List<FileReport> files) {
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        appendField(out, 1, "reportVersion", String.valueOf(REPORT_VERSION), false, true);
        appendField(out, 1, "validatorVersion", validatorVersion, true, true);
        appendRegistry(out, engine, engineVersion);
        appendFiles(out, files);
        appendSummary(out, files);
        out.append("}\n");
        return out.toString();
    }

    private static void appendRegistry(StringBuilder out, String engine, String engineVersion) {
        indent(out, 1);
        out.append("\"registry\": {\n");
        appendField(out, 2, "engine", engine, true, true);
        appendField(out, 2, "engineVersion", engineVersion, true, false);
        indent(out, 1);
        out.append("},\n");
    }

    private static void appendFiles(StringBuilder out, List<FileReport> files) {
        indent(out, 1);
        out.append("\"files\": [\n");
        for (int index = 0; index < files.size(); index++) {
            appendFile(out, files.get(index));
            out.append(index + 1 < files.size() ? ",\n" : "\n");
        }
        indent(out, 1);
        out.append("],\n");
    }

    private static void appendFile(StringBuilder out, FileReport file) {
        indent(out, 2);
        out.append("{\n");
        appendField(out, 3, "file", file.file(), true, true);
        indent(out, 3);
        out.append("\"diagnostics\": [");
        List<Diagnostic> found = file.diagnostics();
        if (found.isEmpty()) {
            out.append("]\n");
        } else {
            out.append("\n");
            appendDiagnostics(out, found);
            indent(out, 3);
            out.append("]\n");
        }
        indent(out, 2);
        out.append("}");
    }

    private static void appendDiagnostics(StringBuilder out, List<Diagnostic> found) {
        for (int index = 0; index < found.size(); index++) {
            appendDiagnostic(out, found.get(index));
            out.append(index + 1 < found.size() ? ",\n" : "\n");
        }
    }

    private static void appendDiagnostic(StringBuilder out, Diagnostic found) {
        indent(out, 4);
        out.append("{\n");
        appendField(out, 5, "behavior", found.provenance().behaviorId(), true, true);
        appendField(out, 5, "list", found.provenance().listName(), true, true);
        appendField(out, 5, "line", lineOf(found), false, true);
        appendField(out, 5, "char", charOf(found), false, true);
        appendField(out, 5, "severity", found.severity().name().toLowerCase(), true, true);
        appendField(out, 5, "code", found.code().name(), true, true);
        appendField(out, 5, "message", found.message(), true, false);
        indent(out, 4);
        out.append("}");
    }

    private static String lineOf(Diagnostic found) {
        return String.valueOf(found.provenance().lineIndex() + 1);
    }

    private static String charOf(Diagnostic found) {
        return found.charOneBased() > 0 ? String.valueOf(found.charOneBased()) : "null";
    }

    private static void appendSummary(StringBuilder out, List<FileReport> files) {
        int errors = 0;
        int warnings = 0;
        for (FileReport file : files) {
            for (Diagnostic found : file.diagnostics()) {
                if (found.severity() == Severity.ERROR) {
                    errors++;
                } else {
                    warnings++;
                }
            }
        }
        indent(out, 1);
        out.append("\"summary\": {\n");
        appendField(out, 2, "files", String.valueOf(files.size()), false, true);
        appendField(out, 2, "errors", String.valueOf(errors), false, true);
        appendField(out, 2, "warnings", String.valueOf(warnings), false, false);
        indent(out, 1);
        out.append("}\n");
    }

    private static void appendField(StringBuilder out, int depth, String name,
            String value, boolean quoted, boolean comma) {
        indent(out, depth);
        out.append("\"").append(name).append("\": ");
        if (quoted) {
            out.append("\"").append(escape(value)).append("\"");
        } else {
            out.append(value);
        }
        out.append(comma ? ",\n" : "\n");
    }

    private static void indent(StringBuilder out, int depth) {
        out.append("  ".repeat(depth));
    }

    /** Minimal JSON string escaping for messages and paths. */
    static String escape(String raw) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < raw.length(); index++) {
            escapeChar(raw.charAt(index), escaped);
        }
        return escaped.toString();
    }

    private static void escapeChar(char letter, StringBuilder escaped) {
        switch (letter) {
            case '"' -> escaped.append("\\\"");
            case '\\' -> escaped.append("\\\\");
            case '\n' -> escaped.append("\\n");
            case '\r' -> escaped.append("\\r");
            case '\t' -> escaped.append("\\t");
            default -> {
                if (letter < 0x20) {
                    escaped.append("\\u%04x".formatted((int) letter));
                } else {
                    escaped.append(letter);
                }
            }
        }
    }
}
