package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.SettingsTree;
import com.jruk8.jmhvalidator.core.model.SettingsTree.SettingSpec;
import com.jruk8.jmhvalidator.core.model.SettingsTree.SettingsNode;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.ports.KnownMaterials;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Load-mirror sweep over one modifier's settings tree:
 * duplicate sibling names drop the whole
 * {@code config.settings} block, unknown spec types warn,
 * and defaults outside allowed warn, exactly like the engine
 * normalize. Traversal is pre-order mirroring the engine
 * loops: each sibling is duplicate-checked then visited in
 * order, and the first duplicate level aborts the sweep, so
 * later levels stay silent. Findings are warnings with the
 * engine's messages verbatim; the {@code dropped} flag tells
 * callers to skip style checks on the dropped block. Message
 * builders are the single source of truth for the engine
 * normalize warnings.
 */
public final class SettingsNormalize {

    /** One sweep: findings plus whether the block dropped. */
    public record Result(List<Diagnostic> findings, boolean dropped) {
    }

    private SettingsNormalize() {
    }

    /**
     * Message for a duplicate level: names the folder path.
     * Byte-identical to the engine normalize warning.
     */
    public static String atMessage(String modifierId, String path) {
        return "Modifier '" + modifierId + "' has duplicate setting names at '"
                + path + "'.";
    }

    /**
     * Message for the block drop that follows any duplicate
     * level. Byte-identical to the engine normalize warning.
     */
    public static String dropMessage(String modifierId) {
        return "Modifier '" + modifierId + "' has duplicate setting names; "
                + "dropping its config.settings block.";
    }

    /**
     * Message for a spec type the engine cannot parse.
     * Byte-identical to the engine normalize warning; the
     * type reads upper-cased, like the engine stores it.
     */
    public static String typeMessage(String modifierId, String path, String upperType) {
        return "Modifier '" + modifierId + "' setting '" + path + "' has unknown type '"
                + upperType + "'.";
    }

    /**
     * Message for a default outside its allowed range.
     * Byte-identical to the engine normalize warning.
     */
    public static String defaultMessage(String modifierId, String path) {
        return "Modifier '" + modifierId + "' setting '" + path
                + "' default is outside allowed; clamped.";
    }

    /**
     * Message for a live value the engine cannot keep.
     * Byte-identical to the engine normalize warning.
     */
    public static String valueMessage(String modifierId, String path) {
        return "Modifier '" + modifierId + "' setting '" + path
                + "' value is invalid; reset to default.";
    }

    /**
     * Message for a setting defining both value and folder.
     * Byte-identical to the engine normalize warning.
     */
    public static String ambiguousMessage(String modifierId, String path) {
        return "Modifier '" + modifierId + "' setting '" + path
                + "' defines both value and folder; using the value.";
    }

    /**
     * Sweep every level of one modifier's settings tree. A
     * null materials port leaves material names unfiltered
     * instead of guessing.
     */
    public static Result check(String modifierId, SettingsTree settings,
            KnownMaterials known) {
        List<Diagnostic> found = new ArrayList<>();
        if (settings == null || settings.roots() == null) {
            return new Result(found, false);
        }
        Set<String> seen = new HashSet<>();
        for (SettingsNode node : settings.roots()) {
            if (!seen.add(node.name())) {
                found.add(drop(modifierId));
                return new Result(found, true);
            }
            if (visit(modifierId, node, node.name(), known, found)) {
                return new Result(found, true);
            }
        }
        return new Result(found, false);
    }

    /**
     * Visit one node: leaf specs check, then folders recurse
     * into duplicate-checked children. True when a duplicate
     * level aborts the sweep.
     */
    private static boolean visit(String modifierId, SettingsNode node, String path,
            KnownMaterials known, List<Diagnostic> found) {
        if (!node.folder()) {
            checkLeaf(modifierId, path, node, known, found);
            return false;
        }
        List<SettingsNode> children =
                node.children() == null ? List.of() : node.children();
        Set<String> seen = new HashSet<>();
        for (SettingsNode child : children) {
            if (!seen.add(child.name())) {
                found.add(at(modifierId, path));
                found.add(drop(modifierId));
                return true;
            }
            if (visit(modifierId, child, path + "." + child.name(), known, found)) {
                return true;
            }
        }
        return false;
    }

    private static void checkLeaf(String modifierId, String path, SettingsNode node,
            KnownMaterials known, List<Diagnostic> found) {
        if (node.ambiguous()) {
            found.add(new Diagnostic(new Provenance(modifierId, "", path, -1), -1,
                    Severity.WARNING, DiagnosticCode.W_AMBIGUOUS_SETTING,
                    ambiguousMessage(modifierId, path)));
        }
        SettingSpec spec = node.spec();
        if (spec == null) {
            return;
        }
        SettingType type = SettingType.parse(spec.type());
        if (spec.type() != null && type == null) {
            found.add(new Diagnostic(new Provenance(modifierId, "", path, -1), -1,
                    Severity.WARNING, DiagnosticCode.W_UNKNOWN_SETTING_TYPE,
                    typeMessage(modifierId, path,
                            spec.type().trim().toUpperCase(Locale.ROOT))));
            return;
        }
        if (type == null) {
            return;
        }
        Object clamped =
                SettingCoerce.clampDefault(type, spec.defaultValue(), spec.allowed(), known);
        if (!SettingCoerce.sameRaw(clamped, spec.defaultValue())) {
            found.add(new Diagnostic(new Provenance(modifierId, "", path, -1), -1,
                    Severity.WARNING, DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                    defaultMessage(modifierId, path)));
        }
        Object fixed =
                SettingCoerce.coerce(type, spec.liveValue(), spec.allowed(), known);
        if (!SettingCoerce.sameRaw(fixed, spec.liveValue())) {
            found.add(new Diagnostic(new Provenance(modifierId, "", path, -1), -1,
                    Severity.WARNING, DiagnosticCode.W_VALUE_INVALID,
                    valueMessage(modifierId, path)));
        }
    }

    private static Diagnostic at(String modifierId, String path) {
        return new Diagnostic(new Provenance(modifierId, "", path, -1), -1,
                Severity.WARNING, DiagnosticCode.W_DUPLICATE_SETTING,
                atMessage(modifierId, path));
    }

    private static Diagnostic drop(String modifierId) {
        return new Diagnostic(new Provenance(modifierId, "", "", -1), -1,
                Severity.WARNING, DiagnosticCode.W_DUPLICATE_SETTING,
                dropMessage(modifierId));
    }
}
