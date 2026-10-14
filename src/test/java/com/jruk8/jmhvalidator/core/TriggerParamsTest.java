package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.OptionsNode;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Option-value pins: documented values pass in every
 * accepted spelling and kind; unknown enums, clamped
 * numbers, and disabling values warn with the effective
 * value; positions the engine fails to bind stay silent
 * for the shape checks.
 */
class TriggerParamsTest {

    @Test
    void documentedValuesStaySilent() {
        OptionsNode options = new OptionsNode(60, 15, "PER_EXECUTOR", 0.5,
                "per_invoke", "pick_random", 2, "PER_INVOKE", 5L, "AFTER");

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertTrue(found.isEmpty());
    }

    @Test
    void emptyAndNullOptionsStaySilent() {
        assertTrue(TriggerParams.check("fog", "main", OptionsNode.empty()).isEmpty());
        assertTrue(TriggerParams.check("fog", "main", null).isEmpty());
    }

    @Test
    void numericStringsParseLikeTheEngine() {
        OptionsNode options = new OptionsNode("60", "15", "PER_INVOKE", "0.5",
                "PER_INVOKE", "IN_ORDER", "3", "PER_INVOKE", "10", "BEFORE");

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertTrue(found.isEmpty());
    }

    @Test
    void wholeDoublesCountAsIntegers() {
        OptionsNode options = new OptionsNode(null, null, null, null,
                null, null, 3.0, null, 5.0, null);

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertTrue(found.isEmpty());
    }

    @Test
    void unknownScopesWarnWithFallback() {
        OptionsNode options = new OptionsNode(null, null, "FOO", null,
                5, null, null, true, null, null);

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertEquals(3, found.size());
        assertEquals("Unknown behavior 'FOO' in behavior 'main'"
                + " options.interval-settings; using PER_INVOKE.",
                found.get(0).message());
        assertEquals("options.success-chance",
                found.get(1).provenance().listName());
        assertTrue(found.get(1).message().contains("'5'"));
        assertTrue(found.get(2).message().contains("'true'"));
        for (Diagnostic finding : found) {
            assertEquals(DiagnosticCode.W_OPTION_VALUE, finding.code());
            assertEquals(Severity.WARNING, finding.severity());
        }
    }

    @Test
    void unknownSelectionWarnsWithFallback() {
        OptionsNode options = new OptionsNode(null, null, null, null,
                null, "SHUFFLE", null, null, null, null);

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertEquals(1, found.size());
        assertEquals("Unknown selection 'SHUFFLE' in behavior 'main'"
                + " options.execution; using IN_ORDER.", found.get(0).message());
    }

    @Test
    void unknownPreStartOrderWarnsWithFallback() {
        OptionsNode options = new OptionsNode(null, null, null, null,
                null, null, null, null, null, "SOMETIME");

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertEquals(1, found.size());
        assertEquals("Unknown pre-start-order 'SOMETIME' in behavior 'main'"
                + " on-start; running BEFORE.", found.get(0).message());
        assertEquals("on-start", found.get(0).provenance().listName());
    }

    @Test
    void chanceOutsideRangeWarnsWithClamp() {
        OptionsNode high = new OptionsNode(null, null, null, 99, null,
                null, null, null, null, null);
        OptionsNode low = new OptionsNode(null, null, null, -0.5, null,
                null, null, null, null, null);

        List<Diagnostic> foundHigh = TriggerParams.check("fog", "main", high);
        List<Diagnostic> foundLow = TriggerParams.check("fog", "main", low);

        assertEquals(1, foundHigh.size());
        assertEquals("Chance 99.0 in behavior 'main' options.success-chance"
                + " is outside 0.0-1.0; clamped to 1.0.",
                foundHigh.get(0).message());
        assertEquals(1, foundLow.size());
        assertTrue(foundLow.get(0).message().endsWith("clamped to 0.0."),
                foundLow.get(0).message());
    }

    @Test
    void nanChanceWarnsWithOne() {
        OptionsNode options = new OptionsNode(null, null, null,
                Double.NaN, null, null, null, null, null, null);

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertEquals(1, found.size());
        assertTrue(found.get(0).message().endsWith("using 1.0."),
                found.get(0).message());
    }

    @Test
    void deviationAboveIntervalWarnsWithClamp() {
        OptionsNode options = new OptionsNode(60, 999, null, null,
                null, null, null, null, null, null);

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertEquals(1, found.size());
        assertEquals("Deviation 999.0 in behavior 'main'"
                + " options.interval-settings is above interval 60.0;"
                + " clamped to 60.0.", found.get(0).message());
    }

    @Test
    void deviationAboveDefaultWarnsWhenIntervalAbsent() {
        OptionsNode options = new OptionsNode(null, 999, null, null,
                null, null, null, null, null, null);

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertEquals(1, found.size());
        assertTrue(found.get(0).message().contains("default interval 60.0"),
                found.get(0).message());
    }

    @Test
    void negativeDeviationWarnsWithZero() {
        OptionsNode options = new OptionsNode(60, -5, null, null,
                null, null, null, null, null, null);

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertEquals(1, found.size());
        assertTrue(found.get(0).message().endsWith("using 0."),
                found.get(0).message());
    }

    @Test
    void deviationWithDeadIntervalWarnsNoEffect() {
        OptionsNode options = new OptionsNode(0, 15, null, null,
                null, null, null, null, null, null);

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertEquals(1, found.size());
        assertTrue(found.get(0).message().contains("has no effect with interval 0.0"),
                found.get(0).message());
    }

    @Test
    void negativeIntervalWarnsDisabled() {
        OptionsNode options = new OptionsNode(-60, null, null, null,
                null, null, null, null, null, null);

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertEquals(1, found.size());
        assertEquals("Negative interval -60.0 in behavior 'main'"
                + " options.interval-settings disables the behavior.",
                found.get(0).message());
    }

    @Test
    void zeroIntervalStaysSilent() {
        OptionsNode options = new OptionsNode(0, null, null, null,
                null, null, null, null, null, null);

        assertTrue(TriggerParams.check("fog", "main", options).isEmpty());
    }

    @Test
    void negativeDelayWarnsWithZero() {
        OptionsNode options = new OptionsNode(null, null, null, null,
                null, null, null, null, -5, null);

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertEquals(1, found.size());
        assertEquals("Negative delay -5 in behavior 'main' options behaves like 0.",
                found.get(0).message());
    }

    @Test
    void lowCountWarnsWithOne() {
        OptionsNode zero = new OptionsNode(null, null, null, null,
                null, null, 0, null, null, null);
        OptionsNode negative = new OptionsNode(null, null, null, null,
                null, null, -2, null, null, null);

        List<Diagnostic> foundZero = TriggerParams.check("fog", "main", zero);
        List<Diagnostic> foundNegative = TriggerParams.check("fog", "main", negative);

        assertEquals(1, foundZero.size());
        assertTrue(foundZero.get(0).message().endsWith("behaves like 1."),
                foundZero.get(0).message());
        assertEquals(1, foundNegative.size());
    }

    @Test
    void engineThrowPositionsStaySilentForShapes() {
        OptionsNode options = new OptionsNode("banana", true, Map.of("x", 1),
                "banana", List.of("x"), Map.of(), 2.5, List.of(), "5.5", List.of());

        List<Diagnostic> found = TriggerParams.check("fog", "main", options);

        assertTrue(found.isEmpty());
    }

    @Test
    void hugeCountStaysSilentForShapes() {
        OptionsNode options = new OptionsNode(null, null, null, null,
                null, null, Long.MAX_VALUE, null, null, null);

        assertTrue(TriggerParams.check("fog", "main", options).isEmpty());
    }
}
