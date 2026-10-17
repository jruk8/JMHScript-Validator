package com.jruk8.jmhvalidator.core.script;

import com.jruk8.jmhvalidator.core.JmhValidator;
import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.ports.TagRegistry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paid-tag diagnostic pins: free validation flags paid tags
 * distinctly from unknown ones; default validation stays silent.
 */
class PaidTagValidationTest {

    private static TagRegistry registry() {
        return TagRegistry.of(List.of("give", "win", "pslot"), List.of("win", "pslot"));
    }

    @Test
    void portDefaultsStayOpen() {
        TagRegistry plain = TagRegistry.of(List.of("give"));

        assertEquals(Set.of(), plain.paidTags());
        assertTrue(!plain.isPaid("give"));
        assertTrue(!plain.isPaid(null));
    }

    @Test
    void freeFlagsPaidButNotFreeTags() {
        TagRegistry registry = registry();

        assertEquals(List.of(
                "Paid tag '<win:HUNTER,done>' needs the paid edition and will not load on free."),
                CommandSyntax.warnings("say <win:HUNTER,done>", Set.of(), registry, true));
        assertTrue(CommandSyntax.warnings("say <give:Steve,dirt>",
                Set.of(), registry, true).isEmpty());
    }

    @Test
    void defaultValidationStaysSilentOnPaid() {
        TagRegistry registry = registry();

        assertTrue(CommandSyntax.warnings("say <win:HUNTER,done>",
                Set.of(), registry).isEmpty());
    }

    @Test
    void unknownStillUnknownUnderFree() {
        TagRegistry registry = registry();

        assertEquals(List.of("Unknown tag '<bogus>', left untouched at runtime."),
                CommandSyntax.warnings("say <bogus>", Set.of(), registry, true));
    }

    @Test
    void sweepCodesPaidDistinctly() {
        BehaviorFile file = new BehaviorFile(Map.of("main",
                new BehaviorFile.BehaviorBlock(
                        Map.of("console", List.of("say <win:HUNTER,done>")),
                        List.of("ON_START"))));

        List<Diagnostic> found = JmhValidator.validateBehavior("mod", file, registry(), true);

        assertEquals(1, found.size(), found.toString());
        assertEquals(DiagnosticCode.W_PAID_TAG, found.get(0).code());
        assertEquals(Severity.WARNING, found.get(0).severity());
        assertTrue(JmhValidator.validateBehavior("mod", file, registry()).isEmpty());
    }
}
