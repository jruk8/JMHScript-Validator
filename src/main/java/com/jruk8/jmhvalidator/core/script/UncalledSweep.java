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
 * Uncalled-definition sweep: leaf settings never read and
 * behavior blocks never called. An uncalled setting usually
 * means its call site has a typo, so the intended wiring never
 * happens. Calls resolve exactly like the reference checks
 * (same collectors, same post-drop tree); folders never flag,
 * and files with dynamic ids skip the affected sweep because
 * dynamic targets cannot resolve statically. GUI reads are
 * invisible here: GUI-only settings still warn.
 */
public final class UncalledSweep {

    private UncalledSweep() {
    }

    /** Sweep one modifier's definitions against its calls. */
    public static List<Diagnostic> check(String modifierId, BehaviorFile file,
            SettingsTree tree) {
        List<Diagnostic> found = new ArrayList<>();
        if (file == null || file.blocks() == null) {
            return found;
        }
        found.addAll(uncalledSettings(modifierId, file, tree));
        found.addAll(uncalledBehaviors(modifierId, file));
        return found;
    }

    private static List<Diagnostic> uncalledSettings(String modifierId, BehaviorFile file,
            SettingsTree tree) {
        List<Diagnostic> found = new ArrayList<>();
        if (tree == null || tree.roots() == null) {
            return found;
        }
        SettingRefs.SettingCalls calls = SettingRefs.collectedCalls(file, tree);
        if (calls.dynamic()) {
            return found;
        }
        List<String> defined = new ArrayList<>();
        for (SettingsNode root : tree.roots()) {
            collectLeafs(root, "", defined);
        }
        for (String path : defined) {
            if (!calls.leafs().contains(path)) {
                found.add(new Diagnostic(new Provenance(modifierId, "", path, -1), -1,
                        Severity.WARNING, DiagnosticCode.W_UNCALLED_SETTING,
                        "Setting '" + path + "' is never read."));
            }
        }
        return found;
    }

    private static List<Diagnostic> uncalledBehaviors(String modifierId, BehaviorFile file) {
        List<Diagnostic> found = new ArrayList<>();
        BehaviorRefs.BlockCalls calls = BehaviorRefs.collectedCalls(file);
        if (calls.dynamic()) {
            return found;
        }
        for (Map.Entry<String, BehaviorFile.BehaviorBlock> block : file.blocks().entrySet()) {
            if (hasRunsOn(block.getValue()) || calls.targets().contains(block.getKey())) {
                continue;
            }
            found.add(new Diagnostic(
                    new Provenance(modifierId, block.getKey(), "", -1), -1,
                    Severity.WARNING, DiagnosticCode.W_UNCALLED_BEHAVIOR,
                    "Behavior '" + block.getKey() + "' is never called."));
        }
        return found;
    }

    private static boolean hasRunsOn(BehaviorFile.BehaviorBlock block) {
        return block != null && block.runsOn() != null && !block.runsOn().isEmpty();
    }

    private static void collectLeafs(SettingsNode node, String prefix, List<String> out) {
        if (node == null || node.name() == null) {
            return;
        }
        String path = prefix.isEmpty() ? node.name() : prefix + "." + node.name();
        if (!node.folder()) {
            out.add(path);
            return;
        }
        if (node.children() != null) {
            for (SettingsNode child : node.children()) {
                collectLeafs(child, path, out);
            }
        }
    }
}
