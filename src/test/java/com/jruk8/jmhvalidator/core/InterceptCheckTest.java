package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.ports.InterceptableTriggers;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Intercept-context pins: out-of-context intercepts error once per
 * block, interceptable runs-on (case-blind, mixed lists included)
 * stays clean, and null ports, blocks, and lists stay silent.
 */
class InterceptCheckTest {

    private static final InterceptableTriggers KNOWN = InterceptableTriggers.of(List.of(
            "ON_MOB_KILLED", "ON_BLOCK_BROKEN", "ON_CRAFT_PREPARE", "ON_BLOCK_PLACED"));

    private static BehaviorFile.BehaviorBlock block(List<String> lines, List<String> runsOn) {
        return new BehaviorFile.BehaviorBlock(Map.of("player", lines), runsOn);
    }

    @Test
    void interceptOutsideInterceptableBlockErrors() {
        List<Diagnostic> found = InterceptCheck.check("fog",
                block(List.of("<intercept:[dirt, 1]>"), List.of("INTERVAL")), "main", KNOWN);

        assertEquals(1, found.size());
        Diagnostic only = found.get(0);
        assertEquals(DiagnosticCode.E_INTERCEPT_CONTEXT, only.code());
        assertEquals(Severity.ERROR, only.severity());
        assertEquals("main", only.provenance().behaviorId());
        assertEquals("player", only.provenance().listName());
        assertEquals(0, only.provenance().lineIndex());
        assertEquals(1, only.charOneBased());
        assertEquals("Tag <intercept> runs only in an interceptable block (ON_MOB_KILLED,"
                + " ON_BLOCK_BROKEN, ON_CRAFT_PREPARE, or ON_BLOCK_PLACED).", only.message());
    }

    @Test
    void messageDerivesFromPortOrder() {
        InterceptableTriggers placed =
                InterceptableTriggers.of(List.of("ON_BLOCK_PLACED"));
        List<Diagnostic> found = InterceptCheck.check("fog",
                block(List.of("<intercept:[dirt, 1]>"), List.of("INTERVAL")), "main", placed);

        assertEquals(1, found.size());
        assertEquals("Tag <intercept> runs only in an interceptable block"
                + " (ON_BLOCK_PLACED).", found.get(0).message());
    }

    @Test
    void emptyAndNullRunsOnError() {
        assertEquals(1, InterceptCheck.check("fog",
                block(List.of("<intercept:[dirt, 1]>"), List.of()), "main", KNOWN).size());
        assertEquals(1, InterceptCheck.check("fog",
                block(List.of("<intercept:[dirt, 1]>"), null), "main", KNOWN).size());
    }

    @Test
    void interceptableRunsOnStaysClean() {
        assertTrue(InterceptCheck.check("fog",
                block(List.of("<intercept:[dirt, 1]>"), List.of("on_mob_killed")), "main",
                KNOWN).isEmpty());
        assertTrue(InterceptCheck.check("fog",
                block(List.of("<intercept:[dirt, 1]>"),
                        List.of("INTERVAL", "ON_BLOCK_BROKEN")),
                "main", KNOWN).isEmpty());
        assertTrue(InterceptCheck.check("fog",
                block(List.of("<intercept:[air, 1]>"), List.of("on_block_placed")), "main",
                KNOWN).isEmpty());
    }

    @Test
    void blocksWithoutInterceptsStayClean() {
        assertTrue(InterceptCheck.check("fog",
                block(List.of("say hi", "<p>"), List.of("INTERVAL")), "main", KNOWN).isEmpty());
    }

    @Test
    void nestedInterceptIsDetected() {
        List<Diagnostic> found = InterceptCheck.check("fog",
                block(List.of("<if:\"<intercept:[dirt, 1]>\" == \"x\",\"exit\">"),
                        List.of("INTERVAL")),
                "main", KNOWN);

        assertEquals(1, found.size());
        assertEquals(DiagnosticCode.E_INTERCEPT_CONTEXT, found.get(0).code());
    }

    @Test
    void firstFindingOnlyPerBlock() {
        List<Diagnostic> found = InterceptCheck.check("fog",
                block(List.of("<intercept:[dirt, 1]>", "<intercept:[stone, 1]>"),
                        List.of("INTERVAL")),
                "main", KNOWN);

        assertEquals(1, found.size());
        assertEquals(0, found.get(0).provenance().lineIndex());
    }

    @Test
    void nullPortBlockAndListsStaySilent() {
        BehaviorFile.BehaviorBlock block =
                block(List.of("<intercept:[dirt, 1]>"), List.of("INTERVAL"));
        assertTrue(InterceptCheck.check("fog", block, "main", null).isEmpty());
        assertTrue(InterceptCheck.check("fog", null, "main", KNOWN).isEmpty());
        assertTrue(InterceptCheck.check("fog",
                new BehaviorFile.BehaviorBlock(null, List.of("INTERVAL")), "main",
                KNOWN).isEmpty());
    }
}
