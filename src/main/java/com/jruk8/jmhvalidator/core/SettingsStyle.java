package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.SettingsTree;
import com.jruk8.jmhvalidator.core.model.SettingsTree.SettingsNode;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Settings style sweep: the strict tree limits, ported 1:1 from
 * the original settings tester (R1/R2/R3 there). At every level
 * including the root: at most 5 folders, no leaf beside folders
 * except `enabled`, at most 7 leafs. Findings are warnings with
 * the dotted settings path in the list field; behavior stays
 * empty with line 0 and a null char (file-level shape).
 *
 * <p>CLI-only: the engine load sweep never runs style checks,
 * so call this from out-of-process sweeps, never from
 * {@link JmhValidator#validateBehavior}.
 */
public final class SettingsStyle {
    /** Max folders per level (R1). */
    public static final int MAX_FOLDERS_PER_LEVEL = 5;
    /** Max leafs per level (R3). */
    public static final int MAX_LEAFS_PER_LEVEL = 7;
    /** Display path for the settings root level. */
    public static final String ROOT_PATH = "<root>";

    private SettingsStyle() {
    }

    /** Check every level of one modifier's settings tree. */
    public static List<Diagnostic> check(String modifierId, SettingsTree settings) {
        List<Diagnostic> found = new ArrayList<>();
        if (settings == null || settings.roots() == null) {
            return found;
        }
        walk(modifierId, settings.roots(), ROOT_PATH, true, found);
        return found;
    }

    private static void walk(String modifierId, List<SettingsNode> nodes,
            String path, boolean isRoot, List<Diagnostic> found) {
        found.addAll(checkLevel(modifierId, nodes, path, isRoot));
        for (SettingsNode node : nodes) {
            if (node.folder()) {
                walk(modifierId, childrenOf(node), joinPath(path, node.name()), false, found);
            }
        }
    }

    private static List<Diagnostic> checkLevel(String modifierId,
            List<SettingsNode> nodes, String path, boolean isRoot) {
        List<Diagnostic> found = new ArrayList<>();
        found.addAll(checkFolders(modifierId, nodes, path));
        found.addAll(checkStray(modifierId, nodes, path, isRoot));
        found.addAll(checkLeafs(modifierId, nodes, path));
        return found;
    }

    private static List<Diagnostic> checkFolders(String modifierId,
            List<SettingsNode> nodes, String path) {
        List<SettingsNode> folders = folders(nodes);
        if (folders.size() <= MAX_FOLDERS_PER_LEVEL) {
            return List.of();
        }
        return List.of(style(modifierId, path, DiagnosticCode.W_SETTINGS_FOLDERS,
                folders.size() + " folders (max " + MAX_FOLDERS_PER_LEVEL + "): "
                        + names(folders)));
    }

    private static List<Diagnostic> checkStray(String modifierId,
            List<SettingsNode> nodes, String path, boolean isRoot) {
        List<SettingsNode> folders = folders(nodes);
        List<SettingsNode> leafs = leafs(nodes);
        List<Diagnostic> found = new ArrayList<>();
        if (!folders.isEmpty()) {
            for (SettingsNode leaf : leafs) {
                if (!isEnabledName(leaf.name())) {
                    found.add(style(modifierId, joinPath(path, leaf.name()),
                            DiagnosticCode.W_SETTINGS_STRAY_LEAF,
                            "stray leaf '" + leaf.name() + "' beside " + folders.size()
                                    + " folder(s); only 'enabled' may sit beside folders"));
                }
            }
            return found;
        }
        if (isRoot) {
            for (SettingsNode leaf : leafs) {
                found.add(style(modifierId, joinPath(path, leaf.name()),
                        DiagnosticCode.W_SETTINGS_STRAY_LEAF,
                        "flat settings root holds bare leaf '" + leaf.name()
                                + "' with zero folders; group leafs into folders"));
            }
        }
        return found;
    }

    private static List<Diagnostic> checkLeafs(String modifierId,
            List<SettingsNode> nodes, String path) {
        List<SettingsNode> leafs = leafs(nodes);
        if (leafs.size() <= MAX_LEAFS_PER_LEVEL) {
            return List.of();
        }
        return List.of(style(modifierId, path, DiagnosticCode.W_SETTINGS_LEAFS,
                leafs.size() + " leafs (max " + MAX_LEAFS_PER_LEVEL + "): "
                        + names(leafs)));
    }

    private static Diagnostic style(String modifierId, String path,
            DiagnosticCode code, String message) {
        return new Diagnostic(new Provenance(modifierId, "", path, -1), -1,
                Severity.WARNING, code, message);
    }

    private static List<SettingsNode> folders(List<SettingsNode> nodes) {
        List<SettingsNode> folders = new ArrayList<>();
        for (SettingsNode node : nodes) {
            if (node.folder()) {
                folders.add(node);
            }
        }
        return folders;
    }

    private static List<SettingsNode> leafs(List<SettingsNode> nodes) {
        List<SettingsNode> leafs = new ArrayList<>();
        for (SettingsNode node : nodes) {
            if (!node.folder()) {
                leafs.add(node);
            }
        }
        return leafs;
    }

    private static List<SettingsNode> childrenOf(SettingsNode node) {
        return node.children() == null ? List.of() : node.children();
    }

    private static String names(List<SettingsNode> nodes) {
        List<String> names = new ArrayList<>();
        for (SettingsNode node : nodes) {
            names.add(node.name());
        }
        return String.join(", ", names);
    }

    /** True when a leaf name is the exempt `enabled` switch. */
    static boolean isEnabledName(String name) {
        return normalizeSegment(name).equals("enabled");
    }

    /** One path segment, normalized: case-insensitive, dash/underscore unify. */
    static String normalizeSegment(String name) {
        return name.strip().toLowerCase(Locale.ROOT).replace("-", "_");
    }

    /** Append one segment to a dotted path (root has no prefix). */
    static String joinPath(String parent, String name) {
        if (parent.equals(ROOT_PATH)) {
            return name;
        }
        return parent + "." + name;
    }
}
