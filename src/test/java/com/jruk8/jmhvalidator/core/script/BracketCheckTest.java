package com.jruk8.jmhvalidator.core.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Golden accepts mirror engine dispatch (nested tags, escapes,
 * quoted brackets, masked if-conditions); faults report the exact
 * zero-based char of the bad bracket.
 */
class BracketCheckTest {

    @Test
    void plainTextPasses() {
        assertTrue(BracketCheck.firstFault("give @a minecraft:stone 3").isEmpty());
    }

    @Test
    void nullAndBlankPass() {
        assertTrue(BracketCheck.firstFault(null).isEmpty());
        assertTrue(BracketCheck.firstFault("   ").isEmpty());
    }

    @Test
    void nestedTagsPass() {
        String line = "<if:<list.contains:[a, b], a>,<p.name>,nobody>";
        assertTrue(BracketCheck.firstFault(line).isEmpty());
    }

    @Test
    void escapedBracketsPass() {
        assertTrue(BracketCheck.firstFault("\\<white\\>hello\\</white\\>").isEmpty());
    }

    @Test
    void escapedOpenBeforeRealTagPasses() {
        assertTrue(BracketCheck.firstFault("\\<<p.name>").isEmpty());
    }

    @Test
    void quotedBracketsPass() {
        assertTrue(BracketCheck.firstFault("<set:note,\"a>b<c\">").isEmpty());
        assertTrue(BracketCheck.firstFault("say 'a<b' done").isEmpty());
    }

    @Test
    void nestedIfPasses() {
        assertTrue(BracketCheck.firstFault("<if:<if:x,y,z>,yes,no>").isEmpty());
    }

    @Test
    void bareLessThanInsideIfConditionFaultsLikeDispatch() {
        Optional<BracketCheck.Fault> fault = BracketCheck.firstFault("<if:a < b,yes,no>");

        assertTrue(fault.isPresent());
        assertEquals(BracketCheck.FaultKind.UNCLOSED_OPEN, fault.get().kind());
        assertEquals(0, fault.get().index());
    }

    @Test
    void unclosedOpenReportsOpenerIndex() {
        String line = "give @a stone <count";
        Optional<BracketCheck.Fault> fault = BracketCheck.firstFault(line);

        assertTrue(fault.isPresent());
        assertEquals(BracketCheck.FaultKind.UNCLOSED_OPEN, fault.get().kind());
        assertEquals(line.indexOf('<'), fault.get().index());
    }

    @Test
    void strayCloserReportsCloserIndex() {
        String line = "say hello> world";
        Optional<BracketCheck.Fault> fault = BracketCheck.firstFault(line);

        assertTrue(fault.isPresent());
        assertEquals(BracketCheck.FaultKind.UNMATCHED_CLOSE, fault.get().kind());
        assertEquals(line.indexOf('>'), fault.get().index());
    }

    @Test
    void doubleCloserFlagsSecond() {
        String line = "<p.name>>";
        Optional<BracketCheck.Fault> fault = BracketCheck.firstFault(line);

        assertTrue(fault.isPresent());
        assertEquals(BracketCheck.FaultKind.UNMATCHED_CLOSE, fault.get().kind());
        assertEquals(8, fault.get().index());
    }

    @Test
    void nestedUnclosedFlagsEarliestOpener() {
        Optional<BracketCheck.Fault> fault = BracketCheck.firstFault("<outer:<inner>");

        assertTrue(fault.isPresent());
        assertEquals(BracketCheck.FaultKind.UNCLOSED_OPEN, fault.get().kind());
        assertEquals(0, fault.get().index());
    }

    @Test
    void unclosedQuoteSwallowsRestLikeDispatch() {
        assertTrue(BracketCheck.firstFault("say \"a<b").isEmpty());
    }
}
