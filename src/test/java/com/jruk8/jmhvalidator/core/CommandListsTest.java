package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * List-name pins: the six dispatched names pass, unknown
 * names warn in map order, matching is exact, and the
 * synthetic loader list stays silent for the shape check.
 */
class CommandListsTest {

    @Test
    void dispatchedNamesStaySilent() {
        List<Diagnostic> found = CommandLists.check("fog", "main", List.of(
                "console", "player", "hunter", "speedrunner",
                "console-cleanup", "player-cleanup"));

        assertTrue(found.isEmpty());
    }

    @Test
    void unknownNameWarns() {
        List<Diagnostic> found =
                CommandLists.check("fog", "main", List.of("player", "playre"));

        assertEquals(1, found.size());
        Diagnostic only = found.get(0);
        assertEquals(DiagnosticCode.W_UNKNOWN_LIST, only.code());
        assertEquals(Severity.WARNING, only.severity());
        assertEquals("main", only.provenance().behaviorId());
        assertEquals("playre", only.provenance().listName());
        assertEquals("Unknown commands list 'playre' in behavior 'main';"
                + " it never runs.", only.message());
    }

    @Test
    void matchingIsExact() {
        List<Diagnostic> found = CommandLists.check("fog", "main",
                List.of("Player", " player", "PLAYER"));

        assertEquals(3, found.size());
    }

    @Test
    void syntheticListStaysSilent() {
        List<Diagnostic> found = CommandLists.check("fog", "main",
                List.of(ShapeMessages.SYNTHETIC_LIST));

        assertTrue(found.isEmpty());
    }

    @Test
    void nullAndEmptyStaySilent() {
        assertTrue(CommandLists.check("fog", "main", null).isEmpty());
        assertTrue(CommandLists.check("fog", "main", List.of()).isEmpty());
    }
}
