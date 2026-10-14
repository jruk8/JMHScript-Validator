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
        assertEquals(1, result.findings().size());
        Diagnostic only = result.findings().get(0);
        assertEquals(DiagnosticCode.W_VALUE_INVALID, only.code());
        assertEquals(Severity.WARNING, only.severity());
        assertEquals("rate", only.provenance().listName());
        assertEquals("Modifier 'fog' setting 'rate' value is invalid; reset to default.",
                only.message());
    }

    @Test
    void validValuePasses() {
        SettingsTree settings = tree(
                leafSpec("rate", "INTEGER", 50, Map.of("min", 0, "max", 100), 60));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertFalse(result.dropped());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void absentAndNullValuesPass() {
        SettingsTree settings = tree(
                leafSpec("absent", "INTEGER", 50, Map.of("min", 0, "max", 100)),
                leafSpec("explicit", "INTEGER", 50, Map.of("min", 0, "max", 100), null));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertFalse(result.dropped());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void bothBadWarnsInEngineOrder() {
        SettingsTree settings = tree(
                leafSpec("rate", "INTEGER", 500, Map.of("min", 0, "max", 100), 600));

        SettingsNormalize.Result result = SettingsNormalize.check("fog", settings, null);

        assertEquals(2, result.findings().size());
        assertEquals(DiagnosticCode.W_DEFAULT_OUTSIDE_ALLOWED,
                result.findings().get(0).code());
        assertEquals(DiagnosticCode.W_VALUE_INVALID, result.findings().get(1).code());
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

    private static SettingsNode leaf(String name) {
        return new SettingsNode(name, false, List.of(), null);
    }

    private static SettingsNode leafSpec(String name, String type, Object def, Object allowed) {
        return leafSpec(name, type, def, allowed, null);
    }

    private static SettingsNode leafSpec(String name, String type, Object def, Object allowed,
            Object live) {
        return new SettingsNode(name, false, List.of(),
                new SettingsTree.SettingSpec(type, def, allowed, live));
    }

    private static SettingsNode folder(String name, SettingsNode... children) {
        return new SettingsNode(name, true, List.of(children), null);
    }

    private static SettingsTree tree(SettingsNode... roots) {
        return new SettingsTree(List.of(roots));
    }
}
