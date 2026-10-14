package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.ports.KnownTriggers;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs-on pins: unknown triggers warn in list order, known
 * triggers match trimmed and case-insensitively, and a
 * missing trigger port stays silent.
 */
class RunTriggersTest {

    private static final KnownTriggers KNOWN = KnownTriggers.of(
            List.of("ON_START", "INTERVAL", "ON_DEATH"));

    @Test
    void unknownTriggerWarns() {
        List<Diagnostic> found = RunTriggers.check("fog", "main",
                List.of("ON_START", "ULTIMATE_INTERVAL"), KNOWN);

        assertEquals(1, found.size());
        Diagnostic only = found.get(0);
        assertEquals(DiagnosticCode.W_UNKNOWN_TRIGGER, only.code());
        assertEquals(Severity.WARNING, only.severity());
        assertEquals("main", only.provenance().behaviorId());
        assertEquals("", only.provenance().listName());
        assertEquals(-1, only.provenance().lineIndex());
        assertEquals(-1, only.charOneBased());
        assertEquals("Unknown trigger 'ULTIMATE_INTERVAL', it never fires.",
                only.message());
    }

    @Test
    void knownMatchesTrimmedAndCaseBlind() {
        List<Diagnostic> found = RunTriggers.check("fog", "main",
                List.of("on_start", "  INTERVAL ", "On_Death"), KNOWN);

        assertTrue(found.isEmpty());
    }

    @Test
    void nullPortAndNullListStaySilent() {
        assertTrue(RunTriggers.check("fog", "main", List.of("NOPE"), null).isEmpty());
        assertTrue(RunTriggers.check("fog", "main", null, KNOWN).isEmpty());
    }

    @Test
    void paramShapeRoutesToUnknownTrigger() {
        List<Diagnostic> found = RunTriggers.check("fog", "main",
                List.of("INTERVAL:30"), KNOWN);

        assertEquals(1, found.size());
        assertEquals(DiagnosticCode.W_UNKNOWN_TRIGGER, found.get(0).code());
        assertEquals("Unknown trigger 'INTERVAL:30', it never fires.",
                found.get(0).message());
    }

    @Test
    void removedTriggerWarnsApart() {
        KnownTriggers known = KnownTriggers.of(
                List.of("ON_START"), List.of("ON_MOB_KILL"));

        List<Diagnostic> found =
                RunTriggers.check("fog", "main", List.of("ON_MOB_KILL"), known);

        assertEquals(1, found.size());
        assertEquals(DiagnosticCode.W_REMOVED_TRIGGER, found.get(0).code());
        assertEquals("Trigger 'ON_MOB_KILL' was removed or renamed; it never fires.",
                found.get(0).message());
    }

    @Test
    void removedMatchingIgnoresCaseAndTrims() {
        KnownTriggers known = KnownTriggers.of(
                List.of("ON_START"), List.of("ON_MOB_KILL"));

        List<Diagnostic> found = RunTriggers.check("fog", "main",
                List.of("  on_mob_kill "), known);

        assertEquals(1, found.size());
        assertEquals(DiagnosticCode.W_REMOVED_TRIGGER, found.get(0).code());
        assertEquals("Trigger 'on_mob_kill' was removed or renamed; it never fires.",
                found.get(0).message());
    }

    @Test
    void unknownStaysUnknownWithoutRemoved() {
        List<Diagnostic> found =
                RunTriggers.check("fog", "main", List.of("ON_MOB_KILL"), KNOWN);

        assertEquals(1, found.size());
        assertEquals(DiagnosticCode.W_UNKNOWN_TRIGGER, found.get(0).code());
    }
}
