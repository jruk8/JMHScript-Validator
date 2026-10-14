package com.jruk8.jmhvalidator.core.script;

import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behavior-reference pins: defined callees stay silent under
 * plain, multi-arg, and quoted ids; unknown ids warn; cycles
 * warn per closing edge; blank, dynamic, malformed, and fatal
 * lines stay silent for other checks to own.
 */
class BehaviorRefsTest {

    @Test
    void knownCalleePasses() {
        BehaviorFile file = blocks(Map.of("main", List.of("say <runblock:side>"),
                "side", List.of("say hi")));

        assertTrue(BehaviorRefs.check("fog", file).isEmpty());
    }

    @Test
    void unknownWarns() {
        BehaviorFile file = blocks(Map.of("main", List.of("say <runblock:nope>")));

        List<Diagnostic> found = BehaviorRefs.check("fog", file);

        assertEquals(1, found.size());
        Diagnostic only = found.get(0);
        assertEquals(DiagnosticCode.W_UNKNOWN_BEHAVIOR, only.code());
        assertEquals(Severity.WARNING, only.severity());
        assertEquals("main", only.provenance().behaviorId());
        assertEquals("player", only.provenance().listName());
        assertEquals(0, only.provenance().lineIndex());
        assertEquals(5, only.charOneBased());
        assertEquals("Tag <runblock> hit unknown behavior 'nope'; skipping.",
                only.message());
    }

    @Test
    void multiArgAndQuotedIdsResolve() {
        BehaviorFile file = blocks(Map.of(
                "main", List.of("say <runblock:side,1,2>", "say <runblock:\"side\">"),
                "side", List.of("say hi")));

        assertTrue(BehaviorRefs.check("fog", file).isEmpty());
    }

    @Test
    void blankDynamicMalformedAndFatalSkip() {
        BehaviorFile file = blocks(Map.of("main", List.of(
                "say <runblock:>",
                "say <runblock:<setting:rate>>",
                "say <runblock:\"a'b>",
                "say <runblock:oops")));

        assertTrue(BehaviorRefs.check("fog", file).isEmpty());
    }

    @Test
    void selfCycleWarns() {
        BehaviorFile file = blocks(Map.of("main", List.of("say <runblock:main>")));

        List<Diagnostic> found = BehaviorRefs.check("fog", file);

        assertEquals(1, found.size());
        assertEquals(DiagnosticCode.W_BEHAVIOR_CYCLE, found.get(0).code());
        assertEquals("Tag <runblock> hit cyclic chain at behavior 'main'; skipping.",
                found.get(0).message());
    }

    @Test
    void twoCycleWarnsBothClosingEdges() {
        BehaviorFile file = blocks(Map.of("main", List.of("say <runblock:side>"),
                "side", List.of("say <runblock:main>")));

        List<Diagnostic> found = BehaviorRefs.check("fog", file);

        assertEquals(2, found.size());
        assertTrue(found.stream().allMatch(entry ->
                entry.code() == DiagnosticCode.W_BEHAVIOR_CYCLE));
    }

    @Test
    void diamondPasses() {
        BehaviorFile file = blocks(Map.of("main", List.of("say <runblock:a>", "say <runblock:b>"),
                "a", List.of("say <runblock:c>"),
                "b", List.of("say <runblock:c>"),
                "c", List.of("say hi")));

        assertTrue(BehaviorRefs.check("fog", file).isEmpty());
    }

    @Test
    void unknownExcludedFromCyclePass() {
        BehaviorFile file = blocks(Map.of("main", List.of("say <runblock:nope>")));

        List<Diagnostic> found = BehaviorRefs.check("fog", file);

        assertEquals(1, found.size());
        assertEquals(DiagnosticCode.W_UNKNOWN_BEHAVIOR, found.get(0).code());
    }

    @Test
    void calledOnlyTargetPasses() {
        BehaviorFile file = new BehaviorFile(Map.of(
                "main", new BehaviorFile.BehaviorBlock(
                        Map.of("player", List.of("say <runblock:shared>")), List.of("ON_START")),
                "shared", new BehaviorFile.BehaviorBlock(
                        Map.of("player", List.of("say hi")), null)));

        assertTrue(BehaviorRefs.check("fog", file).isEmpty());
    }

    private static BehaviorFile blocks(Map<String, List<String>> byBlock) {
        Map<String, BehaviorFile.BehaviorBlock> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : byBlock.entrySet()) {
            out.put(entry.getKey(), new BehaviorFile.BehaviorBlock(
                    Map.of("player", entry.getValue()), List.of("ON_START")));
        }
        return new BehaviorFile(out);
    }
}
