package com.jruk8.jmhvalidator.core.script;

import com.jruk8.jmhvalidator.core.JmhValidator;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.ports.KnownModifiers;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Preset member pins: unknown members warn with the engine's
 * load message byte for byte, known and empty members stay
 * silent, and the sweep diagnostic carries the member position.
 */
class PresetRefsValidationTest {
    private static final KnownModifiers KNOWN =
            KnownModifiers.of(List.of("infection", "perma-night"));

    @Test
    void unknownMessageMatchesEngineLoadWarning() {
        assertEquals("Preset 'horror-manhunt' references unknown modifier 'fog'; skipping.",
                PresetRefs.unknownMessage("horror-manhunt", "fog"));
    }

    @Test
    void unknownMembersWarnInOrderWhileKnownStaySilent() {
        List<String> warnings = PresetRefs.unknownMembers("pack",
                List.of("infection", "fog", "perma-night", "huddle"), KNOWN);
        assertEquals(List.of(
                "Preset 'pack' references unknown modifier 'fog'; skipping.",
                "Preset 'pack' references unknown modifier 'huddle'; skipping."),
                warnings);
    }

    @Test
    void emptyMembersWarnNothing() {
        assertTrue(PresetRefs.unknownMembers("pack", List.of(), KNOWN).isEmpty());
    }

    @Test
    void nullEntriesAreSkipped() {
        List<String> warnings = PresetRefs.unknownMembers("pack",
                Arrays.asList("fog", null), KNOWN);
        assertEquals(1, warnings.size());
    }

    @Test
    void sweepDiagnosticCarriesCodeAndMemberPosition() {
        List<Diagnostic> found = JmhValidator.validatePreset("pack",
                List.of("infection", "fog"), KNOWN);
        assertEquals(1, found.size());
        Diagnostic single = found.get(0);
        assertEquals(DiagnosticCode.W_UNKNOWN_MODIFIER, single.code());
        assertEquals(Severity.WARNING, single.severity());
        assertEquals("modifiers", single.provenance().listName());
        assertEquals("", single.provenance().behaviorId());
        assertEquals(1, single.provenance().lineIndex());
        assertEquals("Preset 'pack' references unknown modifier 'fog'; skipping.",
                single.message());
    }

    @Test
    void sweepWithAllKnownMembersIsClean() {
        assertTrue(JmhValidator.validatePreset("pack",
                List.of("infection", "perma-night"), KNOWN).isEmpty());
    }
}
