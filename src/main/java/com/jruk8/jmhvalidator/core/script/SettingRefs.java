package com.jruk8.jmhvalidator.core.script;

import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.SettingsTree;
import com.jruk8.jmhvalidator.core.model.SettingsTree.SettingsNode;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Setting-reference sweep: every {@code <setting:path>} (alias
 * {@code <s:path>}) in behavior lines must name a defined leaf
 * setting. Missing paths and folder paths both read
 * {@code null} at runtime, so both warn. Resolution walks
 * dotted parts with exact case-sensitive matches, mirroring
 * the engine's path lookup. Lines with fatal bracket or
 * syntax errors stay silent here (the fatal owns the line),
 * and dynamic paths holding nested tags cannot resolve
 * statically, so they stay silent too.
 */
public final class SettingRefs {

    private SettingRefs() {
    }

    /** Check every behavior line of one modifier, in file order. */
    public static List<Diagnostic> check(String modifierId, BehaviorFile file,
            SettingsTree settings) {
        List<Diagnostic> found = new ArrayList<>();
        if (file == null || file.blocks() == null) {
            return found;
        }
        for (Map.Entry<String, BehaviorFile.BehaviorBlock> block : file.blocks().entrySet()) {
            if (block.getValue() == null || block.getValue().lists() == null) {
                continue;
            }
            for (Map.Entry<String, List<String>> list : block.getValue().lists().entrySet()) {
                checkList(modifierId, block.getKey(), list.getKey(),
                        list.getValue(), settings, found);
            }
        }
        return found;
    }

    private static void checkList(String modifierId, String blockId, String listName,
            List<String> lines, SettingsTree settings, List<Diagnostic> found) {
        if (lines == null) {
            return;
        }
        for (int index = 0; index < lines.size(); index++) {
            checkLine(modifierId, blockId, listName, index,
                    lines.get(index), settings, found);
        }
    }

    private static void checkLine(String modifierId, String blockId, String listName,
            int index, String line, SettingsTree settings, List<Diagnostic> found) {
        if (line == null || BracketCheck.firstFault(line).isPresent()
                || CommandSyntax.error(line).isPresent()) {
            return;
        }
        for (Span ref : settingSpans(line, 0)) {
            if (!isLeaf(settings, ref.path())) {
                found.add(new Diagnostic(
                        new Provenance(modifierId, blockId, listName, index),
                        ref.openOneBased(), Severity.WARNING,
                        DiagnosticCode.W_UNKNOWN_SETTING,
                        "Unknown setting '" + ref.path()
                                + "', resolves to null at runtime."));
            }
        }
    }

    /** One static setting reference: 1-based opener plus path. */
    private record Span(int openOneBased, String path) {
    }

    /**
     * Static setting references in one line, nested spans
     * included. Heads match case-insensitively; blank and
     * dynamic (tag-holding) paths stay silent.
     */
    private static List<Span> settingSpans(String line, int base) {
        List<Span> refs = new ArrayList<>();
        for (int[] span : TagHeads.topLevelTagSpans(line)) {
            String body = line.substring(span[0] + 1, span[1] - 1);
            String head = TagHeads.tagName(body);
            if (head.equals("setting") || head.equals("s")) {
                String path = args(body).trim();
                if (!path.isEmpty() && !holdsTag(path)) {
                    refs.add(new Span(base + span[0] + 1, path));
                }
            }
            if (span[1] - span[0] > 2) {
                refs.addAll(settingSpans(body, base + span[0] + 1));
            }
        }
        return refs;
    }

    private static String args(String body) {
        int separator = body.indexOf(':');
        return separator < 0 ? "" : body.substring(separator + 1);
    }

    private static boolean holdsTag(String path) {
        return path.indexOf('<') >= 0 || path.indexOf('>') >= 0;
    }

    /**
     * True when the dotted path walks to a leaf, with exact
     * case-sensitive part matches. Folders and missing parts
     * read null at runtime, so they resolve false.
     */
    private static boolean isLeaf(SettingsTree settings, String path) {
        if (settings == null || settings.roots() == null) {
            return false;
        }
        List<SettingsNode> level = settings.roots();
        String[] parts = path.split("\\.");
        for (int index = 0; index < parts.length; index++) {
            SettingsNode current = null;
            for (SettingsNode node : level) {
                if (node.name().equals(parts[index])) {
                    current = node;
                    break;
                }
            }
            if (current == null) {
                return false;
            }
            if (index == parts.length - 1) {
                return !current.folder();
            }
            if (!current.folder()) {
                return false;
            }
            level = current.children() == null ? List.of() : current.children();
        }
        return false;
    }
}
