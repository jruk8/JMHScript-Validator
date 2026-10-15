package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.SettingsTree;
import com.jruk8.jmhvalidator.core.model.SettingsTree.SettingsNode;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.ports.KnownMaterials;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Duplicate-name pins: folder children repeat in list order,
 * so the first repeat warns with its level path and drops
 * the block; later levels stay silent, mirroring the engine
 * normalize abort.
 */
class SettingsNormalizeTest {

    @Test
    void folderRepeatWarnsAtPathAndDrops() {
        SettingsTree settings = tree(folder("targeting",
                leaf("interval"), leaf("chance"), leaf("interval")));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertTrue(result.dropped());
        assertEquals(2, result.findings().size());
        Diagnostic at = result.findings().get(0);
        assertEquals(DiagnosticCode.W_DUPLICATE_SETTING, at.code());
        assertEquals(Severity.WARNING, at.severity());
        assertEquals("targeting", at.provenance().listName());
        assertEquals("Modifier 'fog' has duplicate setting names at 'targeting'.",
                at.message());
        Diagnostic drop = result.findings().get(1);
        assertEquals(DiagnosticCode.W_DUPLICATE_SETTING, drop.code());
        assertEquals("", drop.provenance().listName());
        assertEquals("Modifier 'fog' has duplicate setting names; "
                + "dropping its config.settings block.", drop.message());
    }

    @Test
    void nestedRepeatNamesDottedPath() {
        SettingsTree settings = tree(folder("outer",
                folder("inner", leaf("rate"), leaf("rate"))));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertTrue(result.dropped());
        assertEquals("Modifier 'fog' has duplicate setting names at 'outer.inner'.",
                result.findings().get(0).message());
        assertEquals("outer.inner", result.findings().get(0).provenance().listName());
    }

    @Test
    void firstRepeatWinsLaterLevelsSilent() {
        SettingsTree settings = tree(
                folder("alpha", leaf("one"), leaf("one")),
                folder("bravo", leaf("two"), leaf("two")));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertTrue(result.dropped());
        assertEquals(2, result.findings().size());
        assertEquals("Modifier 'fog' has duplicate setting names at 'alpha'.",
                result.findings().get(0).message());
    }

    @Test
    void folderAndLeafShareName() {
        SettingsTree settings = tree(folder("mix",
                leaf("rate"), folder("rate", leaf("deep"))));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertTrue(result.dropped());
        assertEquals(2, result.findings().size());
    }

    @Test
    void uniqueNamesStaySilent() {
        SettingsTree settings = tree(
                folder("alpha", leaf("one"), leaf("two")),
                folder("bravo", leaf("one")),
                leaf("enabled"));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertFalse(result.dropped());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void nullSettingsStaySilent() {
        assertFalse(SettingsNormalize.check("nulls", null, null).dropped());
        assertTrue(SettingsNormalize.check("nulls", null, null).findings().isEmpty());
        assertFalse(SettingsNormalize.check("nulls", new SettingsTree(null), null).dropped());
    }

    @Test
    void defaultOutsideAllowedWarns() {
        SettingsTree settings = tree(
                leafSpec("rate", "INTEGER", 500, Map.of("min", 0, "max", 100)),
                leafSpec("ok", "INTEGER", 50, Map.of("min", 0, "max", 100)));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertFalse(result.dropped());
        assertEquals(1, result.findings().size());
        Diagnostic only = result.findings().get(0);
        assertEquals(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED, only.code());
        assertEquals(Severity.WARNING, only.severity());
        assertEquals("rate", only.provenance().listName());
        assertEquals("Modifier 'fog' setting 'rate' default is outside allowed; clamped.",
                only.message());
    }

    @Test
    void missingDefaultWarnsAndFillsZero() {
        SettingsTree settings = tree(leafSpec("rate", "INTEGER", null, null));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertEquals(1, result.findings().size());
        assertEquals(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                result.findings().get(0).code());
    }

    @Test
    void unknownTypeWarnsUppercased() {
        SettingsTree settings = tree(leafSpec("rate", "integerr", 5, null));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertEquals(1, result.findings().size());
        Diagnostic only = result.findings().get(0);
        assertEquals(DiagnosticCode.W_UNKNOWN_SETTING_TYPE, only.code());
        assertEquals("Modifier 'fog' setting 'rate' has unknown type 'INTEGERR'.",
                only.message());
    }

    @Test
    void untypedLeafStaysSilent() {
        SettingsTree settings = tree(leafSpec("rate", null, 500, null));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertTrue(result.findings().isEmpty());
    }

    @Test
    void defaultWarningPrecedesLaterDuplicate() {
        SettingsTree settings = tree(
                leafSpec("rate", "INTEGER", 500, Map.of("min", 0, "max", 100)),
                leaf("dup"), leaf("dup"));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertTrue(result.dropped());
        assertEquals(2, result.findings().size());
        assertEquals(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                result.findings().get(0).code());
        assertEquals(DiagnosticCode.W_DUPLICATE_SETTING,
                result.findings().get(1).code());
    }

    @Test
    void materialNamesNeedThePort() {
        KnownMaterials known = KnownMaterials.of(List.of("DIAMOND_SWORD", "STONE"));
        SettingsTree settings = tree(
                leafSpec("blade", "MATERIAL", "DIAMOND_SWORD", null),
                leafSpec("bogus", "MATERIAL", "NOT_A_MATERIAL", null));

        SettingsNormalize.Result withPort = SettingsNormalize.check("fog", settings, known);

        assertEquals(1, withPort.findings().size());
        assertEquals("Modifier 'fog' setting 'bogus' default is outside allowed; clamped.",
                withPort.findings().get(0).message());

        SettingsNormalize.Result lenient = SettingsNormalize.check("fog", settings, null);

        assertTrue(lenient.findings().isEmpty());
    }

    @Test
    void valueInvalidWarns() {
        SettingsTree settings = tree(
                leafSpec("rate", "INTEGER", 50, Map.of("min", 0, "max", 100), 500));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertFalse(result.dropped());
        assertEquals(2, result.findings().size());
        Diagnostic only = result.findings().get(0);
        assertEquals(DiagnosticCode.W_VALUE_INVALID, only.code());
        assertEquals(Severity.WARNING, only.severity());
        assertEquals("rate", only.provenance().listName());
        assertEquals("Modifier 'fog' setting 'rate' value is invalid; reset to default.",
                only.message());
        assertEquals(DiagnosticCode.E_VALUE_DIFFERS,
                result.findings().get(1).code());
    }

    @Test
    void validValueFlagsOverride() {
        SettingsTree settings = tree(
                leafSpec("rate", "INTEGER", 50, Map.of("min", 0, "max", 100), 60));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertSingle(DiagnosticCode.E_VALUE_DIFFERS, result);
    }

    @Test
    void absentValuePasses() {
        SettingsTree settings = tree(
                leafSpec("absent", "INTEGER", 50, Map.of("min", 0, "max", 100)));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertFalse(result.dropped());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void explicitNullValueFlagsOverride() {
        SettingsTree settings = tree(
                leafSpec("explicit", "INTEGER", 50, Map.of("min", 0, "max", 100), null, true));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertSingle(DiagnosticCode.E_VALUE_DIFFERS, result);
    }

    @Test
    void bothBadWarnsInEngineOrder() {
        SettingsTree settings = tree(
                leafSpec("rate", "INTEGER", 500, Map.of("min", 0, "max", 100), 600));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertEquals(3, result.findings().size());
        assertEquals(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                result.findings().get(0).code());
        assertEquals(DiagnosticCode.W_VALUE_INVALID, result.findings().get(1).code());
        assertEquals(DiagnosticCode.E_VALUE_DIFFERS,
                result.findings().get(2).code());
    }

    @Test
    void unknownTypeSkipsValueCheck() {
        SettingsTree settings = tree(
                leafSpec("rate", "BOGUS", 500, Map.of("min", 0, "max", 100), 600));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertEquals(1, result.findings().size());
        assertEquals(DiagnosticCode.W_UNKNOWN_SETTING_TYPE,
                result.findings().get(0).code());
    }

    @Test
    void ambiguousWarnsFirstThenContinues() {
        SettingsTree settings = tree(ambiguous("thing",
                new SettingsTree.SettingSpec("INTEGER", 500, Map.of("min", 0, "max", 100), 50,
                        true)));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertFalse(result.dropped());
        assertEquals(3, result.findings().size());
        assertEquals(DiagnosticCode.W_AMBIGUOUS_SETTING,
                result.findings().get(0).code());
        assertEquals(DiagnosticCode.E_VALUE_DIFFERS,
                result.findings().get(2).code());
        assertEquals("Modifier 'fog' setting 'thing' defines both value and folder; "
                + "using the value.", result.findings().get(0).message());
        assertEquals(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                result.findings().get(1).code());
    }

    @Test
    void ambiguousWithoutSpecWarnsOnce() {
        SettingsTree settings = tree(ambiguous("thing", null));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertEquals(1, result.findings().size());
        assertEquals(DiagnosticCode.W_AMBIGUOUS_SETTING,
                result.findings().get(0).code());
    }

    @Test
    void booleanRepairsWarnAtBothSites() {
        SettingsTree badDefault = tree(leafSpec("flag", "BOOLEAN", "maybe", null));
        SettingsTree badLive = tree(leafSpec("flag", "BOOLEAN", true, null, "maybe"));

        assertSingle(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                SettingsNormalize.check("fog", badDefault, null));
        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS,
                SettingsNormalize.check("fog", badLive, null));
    }

    @Test
    void doubleRepairsWarnAtBothSites() {
        Object allowed = Map.of("max", 1);
        SettingsTree badDefault = tree(leafSpec("rate", "DOUBLE", 1.5, allowed));
        SettingsTree badLive = tree(leafSpec("rate", "DOUBLE", 0.5, allowed, 1.5));

        assertSingle(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                SettingsNormalize.check("fog", badDefault, null));
        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS,
                SettingsNormalize.check("fog", badLive, null));
    }

    @Test
    void stringCapsWarnAtBothSites() {
        String longText = "x".repeat(300);
        SettingsTree badDefault = tree(leafSpec("motto", "STRING", longText, null));
        SettingsTree badLive = tree(leafSpec("motto", "STRING", "ok", null, longText));

        assertSingle(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                SettingsNormalize.check("fog", badDefault, null));
        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS,
                SettingsNormalize.check("fog", badLive, null));
    }

    @Test
    void maxLengthWarnsAtBothSites() {
        Object allowed = Map.of("maxLength", 2);
        SettingsTree badDefault = tree(leafSpec("tag", "STRING", "abc", allowed));
        SettingsTree badLive = tree(leafSpec("tag", "STRING", "ab", allowed, "abc"));

        assertSingle(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                SettingsNormalize.check("fog", badDefault, null));
        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS,
                SettingsNormalize.check("fog", badLive, null));
    }

    @Test
    void enumMembershipWarnsAtBothSites() {
        Object allowed = List.of("FULL", "BROADCAST");
        SettingsTree badDefault = tree(leafSpec("mode", "ENUM", "zzz", allowed));
        SettingsTree badLive = tree(leafSpec("mode", "ENUM", "FULL", allowed, "zzz"));

        assertSingle(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                SettingsNormalize.check("fog", badDefault, null));
        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS,
                SettingsNormalize.check("fog", badLive, null));
    }

    @Test
    void emptyEnumFlagsOverride() {
        SettingsTree settings = tree(leafSpec("mode", "ENUM", "zzz", null, "yyy"));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertSingle(DiagnosticCode.E_VALUE_DIFFERS, result);
    }

    @Test
    void materialLiveValueNeedsThePort() {
        KnownMaterials known = KnownMaterials.of(List.of("STONE"));
        SettingsTree settings = tree(leafSpec("blade", "MATERIAL", "STONE", null, "nope"));

        SettingsNormalize.Result withPort = SettingsNormalize.check("fog", settings, known);

        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS, withPort);
    }

    @Test
    void colorRepairsWarnAtBothSites() {
        SettingsTree badDefault = tree(leafSpec("tint", "COLOR", "red", null));
        SettingsTree badLive = tree(leafSpec("tint", "COLOR", "ff00aa", null, "red"));

        assertSingle(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                SettingsNormalize.check("fog", badDefault, null));
        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS,
                SettingsNormalize.check("fog", badLive, null));
    }

    @Test
    void listCapsWarnAtBothSites() {
        Object longList = java.util.Collections.nCopies(70, "x");
        SettingsTree badDefault = tree(leafSpec("names", "LIST", longList, null));
        SettingsTree badLive =
                tree(leafSpec("names", "LIST", List.of("x"), null, longList));

        assertSingle(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                SettingsNormalize.check("fog", badDefault, null));
        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS,
                SettingsNormalize.check("fog", badLive, null));
    }

    @Test
    void listChoicesWarnAtBothSites() {
        Object allowed = List.of("a");
        SettingsTree badDefault = tree(leafSpec("pick", "LIST", List.of("a", "b"), allowed));
        SettingsTree badLive =
                tree(leafSpec("pick", "LIST", List.of("a"), allowed, List.of("b")));

        assertSingle(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                SettingsNormalize.check("fog", badDefault, null));
        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS,
                SettingsNormalize.check("fog", badLive, null));
    }

    @Test
    void effectsRepairsWarnAtBothSites() {
        Object bad = List.of(List.of("SPEED", 1));
        Object good = List.of(List.of("SPEED", 1, 30));
        SettingsTree badDefault = tree(leafSpec("fx", "EFFECTS", bad, null));
        SettingsTree badLive = tree(leafSpec("fx", "EFFECTS", good, null, bad));

        assertSingle(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                SettingsNormalize.check("fog", badDefault, null));
        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS,
                SettingsNormalize.check("fog", badLive, null));
    }

    @Test
    void itemsRepairsWarnAtBothSites() {
        KnownMaterials known = KnownMaterials.of(List.of("STONE"));
        Object bad = List.of(List.of("STONE", 1));
        Object good = List.of(List.of("STONE", 1, 1));
        SettingsTree badDefault = tree(leafSpec("kit", "ITEMS", bad, null));
        SettingsTree badLive = tree(leafSpec("kit", "ITEMS", good, null, bad));

        assertSingle(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                SettingsNormalize.check("fog", badDefault, known));
        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS,
                SettingsNormalize.check("fog", badLive, known));
    }

    @Test
    void weightedRepairsWarnAtBothSites() {
        Object bad = List.of(List.of(1.0));
        Object good = List.of(List.of(1.5, "sword"));
        SettingsTree badDefault = tree(leafSpec("drop", "WEIGHTED_ITEM", bad, null));
        SettingsTree badLive = tree(leafSpec("drop", "WEIGHTED_ITEM", good, null, bad));

        assertSingle(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                SettingsNormalize.check("fog", badDefault, null));
        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS,
                SettingsNormalize.check("fog", badLive, null));
    }

    @Test
    void messageRepairsWarnAtBothSites() {
        Object bad = List.of(true);
        Object good = List.of(true, "hi");
        SettingsTree badDefault = tree(leafSpec("msg", "MESSAGE", bad, null));
        SettingsTree badLive = tree(leafSpec("msg", "MESSAGE", good, null, bad));

        assertSingle(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                SettingsNormalize.check("fog", badDefault, null));
        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS,
                SettingsNormalize.check("fog", badLive, null));
    }

    @Test
    void soundRepairsWarnAtBothSites() {
        Object bad = List.of(true, "x");
        Object good = List.of(true, "x", 1.0, 1.0);
        SettingsTree badDefault = tree(leafSpec("sfx", "SOUND", bad, null));
        SettingsTree badLive = tree(leafSpec("sfx", "SOUND", good, null, bad));

        assertSingle(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                SettingsNormalize.check("fog", badDefault, null));
        assertPair(DiagnosticCode.W_VALUE_INVALID, DiagnosticCode.E_VALUE_DIFFERS,
                SettingsNormalize.check("fog", badLive, null));
    }

    private static void assertSingle(DiagnosticCode code,
            SettingsNormalize.Result result) {
        assertFalse(result.dropped());
        assertEquals(1, result.findings().size());
        assertEquals(code, result.findings().get(0).code());
    }

    private static void assertPair(DiagnosticCode first, DiagnosticCode second,
            SettingsNormalize.Result result) {
        assertFalse(result.dropped());
        assertEquals(2, result.findings().size());
        assertEquals(first, result.findings().get(0).code());
        assertEquals(second, result.findings().get(1).code());
    }

    @Test
    void iconlessFolderWarns() {
        SettingsTree settings = tree(
                bareFolder("targeting", leaf("rate")),
                folder("iconed", leaf("rate")));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertFalse(result.dropped());
        assertEquals(1, result.findings().size());
        assertEquals(DiagnosticCode.W_FOLDER_ICON, result.findings().get(0).code());
        assertEquals("Modifier 'fog' folder 'targeting' has no icon; add meta.icon.",
                result.findings().get(0).message());
        assertEquals("targeting", result.findings().get(0).provenance().listName());
    }

    @Test
    void blankIconWarnsLikeMissing() {
        SettingsNode blank = new SettingsNode("grp", true,
                List.of(leaf("rate")), null, false, "  ");
        SettingsNormalize.Result result =
                SettingsNormalize.check("fog", tree(blank), null);

        assertEquals(1, result.findings().size());
        assertEquals(DiagnosticCode.W_FOLDER_ICON, result.findings().get(0).code());
    }

    @Test
    void nestedIconlessFolderWarnsAtPath() {
        SettingsTree settings = tree(
                folder("outer", bareFolder("inner", leaf("rate"))));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertEquals(1, result.findings().size());
        assertEquals("Modifier 'fog' folder 'outer.inner' has no icon; add meta.icon.",
                result.findings().get(0).message());
    }

    @Test
    void iconWarningPrecedesDrop() {
        SettingsTree settings = tree(
                bareFolder("targeting", leaf("rate"), leaf("rate")));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertTrue(result.dropped());
        assertEquals(3, result.findings().size());
        assertEquals(DiagnosticCode.W_FOLDER_ICON, result.findings().get(0).code());
    }

    private static SettingsNode leaf(String name) {
        return new SettingsNode(name, false, List.of(), null, false);
    }

    private static SettingsNode leafSpec(String name, String type, Object def, Object allowed) {
        return leafSpec(name, type, def, allowed, null, false);
    }

    private static SettingsNode leafSpec(String name, String type, Object def, Object allowed,
            Object live) {
        return leafSpec(name, type, def, allowed, live, live != null);
    }

    private static SettingsNode leafSpec(String name, String type, Object def, Object allowed,
            Object live, boolean present) {
        return new SettingsNode(name, false, List.of(),
                new SettingsTree.SettingSpec(type, def, allowed, live, present), false);
    }

    private static SettingsNode ambiguous(String name, SettingsTree.SettingSpec spec) {
        return new SettingsNode(name, false, List.of(), spec, true);
    }

    private static SettingsNode folder(String name, SettingsNode... children) {
        return new SettingsNode(name, true, List.of(children), null, false, "STONE");
    }

    @Test
    void redundantValueWarns() {
        SettingsTree settings = tree(
                leafSpec("rate", "INTEGER", 50, Map.of("min", 0, "max", 100), 50, true));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertFalse(result.dropped());
        assertEquals(1, result.findings().size());
        Diagnostic only = result.findings().get(0);
        assertEquals(DiagnosticCode.W_REDUNDANT_VALUE, only.code());
        assertEquals(Severity.WARNING, only.severity());
        assertEquals("Modifier 'fog' setting 'rate' value repeats the default; omit it.",
                only.message());
    }

    @Test
    void redundantValueComparesAcrossIntAndDouble() {
        SettingsTree settings = tree(
                leafSpec("rate", "DOUBLE", 3, Map.of("min", 0, "max", 10), 3.0, true));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertFalse(result.dropped());
        assertEquals(1, result.findings().size());
        assertEquals(DiagnosticCode.W_REDUNDANT_VALUE,
                result.findings().get(0).code());
    }

    @Test
    void differingValueErrors() {
        SettingsTree settings = tree(
                leafSpec("rate", "INTEGER", 50, Map.of("min", 0, "max", 100), 60, true));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertFalse(result.dropped());
        assertEquals(1, result.findings().size());
        Diagnostic only = result.findings().get(0);
        assertEquals(DiagnosticCode.E_VALUE_DIFFERS, only.code());
        assertEquals(Severity.ERROR, only.severity());
        assertEquals("Modifier 'fog' setting 'rate' value differs from the default;"
                + " shipped files must not carry overrides.", only.message());
    }

    @Test
    void absentValueStaysSilent() {
        SettingsTree settings = tree(
                leafSpec("rate", "INTEGER", 50, Map.of("min", 0, "max", 100), null, false));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertFalse(result.dropped());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void missingDefaultSkipsBothChecks() {
        SettingsTree settings = tree(
                leafSpec("rate", "INTEGER", null, Map.of("min", 0, "max", 100), 50, true));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertFalse(result.dropped());
        for (Diagnostic finding : result.findings()) {
            assertNotEquals(DiagnosticCode.W_REDUNDANT_VALUE, finding.code());
            assertNotEquals(DiagnosticCode.E_VALUE_DIFFERS, finding.code());
        }
    }

    private static SettingsNode bareFolder(String name, SettingsNode... children) {
        return new SettingsNode(name, true, List.of(children), null, false);
    }

    private static SettingsTree tree(SettingsNode... roots) {
        return new SettingsTree(List.of(roots));
    }
}
