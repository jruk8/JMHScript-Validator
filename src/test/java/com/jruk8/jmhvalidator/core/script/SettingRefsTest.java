package com.jruk8.jmhvalidator.core.script;

import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.SettingsTree;
import com.jruk8.jmhvalidator.core.model.SettingsTree.SettingsNode;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Setting-reference pins: defined leaves stay silent under
 * both heads and nesting; missing paths, folders, and case
 * mismatches warn; blank, dynamic, and fatal lines stay
 * silent for other checks to own.
 */
class SettingRefsTest {

    @Test
    void definedLeafStaysSilent() {
        SettingsTree settings = tree(leaf("rate"));

        List<Diagnostic> found = SettingRefs.check("fog",
                file("say <setting:rate>"), settings);

        assertTrue(found.isEmpty());
    }

    @Test
    void aliasAndNestingResolve() {
        SettingsTree settings = tree(
                folder("targeting", leaf("interval"), folder("deep", leaf("rate"))));

        List<Diagnostic> found = SettingRefs.check("fog", file(
                "say <s:targeting.interval>",
                "say <if:\"<setting:targeting.deep.rate> == 1\",\"hi\">"), settings);

        assertTrue(found.isEmpty());
    }

    @Test
    void undefinedPathWarnsWithChar() {
        SettingsTree settings = tree(leaf("rate"));

        List<Diagnostic> found = SettingRefs.check("fog",
                file("say <setting:nope>"), settings);

        assertEquals(1, found.size());
        Diagnostic only = found.get(0);
        assertEquals(DiagnosticCode.W_UNKNOWN_SETTING, only.code());
        assertEquals(Severity.WARNING, only.severity());
        assertEquals(5, only.charOneBased());
        assertEquals("main", only.provenance().behaviorId());
        assertEquals("player", only.provenance().listName());
        assertEquals(0, only.provenance().lineIndex());
        assertEquals("Unknown setting 'nope', resolves to null at runtime.",
                only.message());
    }

    @Test
    void folderAndCaseMismatchWarn() {
        SettingsTree settings = tree(folder("targeting", leaf("rate")));

        List<Diagnostic> found = SettingRefs.check("fog", file(
                "say <setting:targeting>",
                "say <setting:targeting.Rate>"), settings);

        assertEquals(2, found.size());
        assertEquals("Unknown setting 'targeting', resolves to null at runtime.",
                found.get(0).message());
        assertEquals("Unknown setting 'targeting.Rate', resolves to null at runtime.",
                found.get(1).message());
    }

    @Test
    void blankDynamicAndFatalLinesStaySilent() {
        SettingsTree settings = tree(leaf("rate"));

        List<Diagnostic> found = SettingRefs.check("fog", file(
                "say <setting>",
                "say <setting:<flag:x>>",
                "say <setting:oops"), settings);

        assertTrue(found.isEmpty());
    }

    @Test
    void paddedPathResolvesLikeTheEngine() {
        SettingsTree settings = tree(leaf("rate"));

        List<Diagnostic> found = SettingRefs.check("fog",
                file("say <setting: rate >"), settings);

        assertTrue(found.isEmpty());
    }

    @Test
    void secondColonRoutesToUnknownSetting() {
        SettingsTree settings = tree(leaf("rate"));

        List<Diagnostic> found = SettingRefs.check("fog",
                file("say <setting:rate:foo>"), settings);

        assertEquals(1, found.size());
        assertEquals(DiagnosticCode.W_UNKNOWN_SETTING, found.get(0).code());
    }

    private static BehaviorFile file(String... lines) {
        return new BehaviorFile(Map.of("main", new BehaviorFile.BehaviorBlock(
                Map.of("player", List.of(lines)), List.of("ON_START"))));
    }

    private static SettingsNode leaf(String name) {
        return new SettingsNode(name, false, List.of(), null, false);
    }

    private static SettingsNode folder(String name, SettingsNode... children) {
        return new SettingsNode(name, true, List.of(children), null, false);
    }

    private static SettingsTree tree(SettingsNode... roots) {
        return new SettingsTree(List.of(roots));
    }
}
