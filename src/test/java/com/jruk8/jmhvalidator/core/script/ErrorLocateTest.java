package com.jruk8.jmhvalidator.core.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jruk8.jmhvalidator.core.model.Provenance;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/** Chars report only for unique span matches, never guesses. */
class ErrorLocateTest {

    @Test
    void uniqueHeadReportsOneBasedChar() {
        OptionalInt found = ErrorLocate.charInLine("give <p> beef <min:5>",
                "Tag <min> needs two numbers.");

        assertTrue(found.isPresent());
        assertEquals(15, found.getAsInt());
    }

    @Test
    void nestedHeadMatches() {
        OptionalInt found = ErrorLocate.charInLine("<lf:x,<min:5>>",
                "Tag <min> needs two numbers.");

        assertTrue(found.isPresent());
        assertEquals(7, found.getAsInt());
    }

    @Test
    void duplicateHeadsStayLineLevel() {
        assertTrue(ErrorLocate.charInLine("<min:1> then <min:2>",
                "Tag <min> needs two numbers.").isEmpty());
    }

    @Test
    void missingHeadStaysLineLevel() {
        assertTrue(ErrorLocate.charInLine("say hello",
                "Tag <min> needs two numbers.").isEmpty());
    }

    @Test
    void quotedTagTextStillMatches() {
        OptionalInt found = ErrorLocate.charInLine("say \"<min>\" loudly",
                "Tag <min> needs two numbers.");

        assertTrue(found.isPresent());
        assertEquals(6, found.getAsInt());
    }

    @Test
    void headMatchIgnoresCase() {
        OptionalInt found = ErrorLocate.charInLine("say <MIN:5>",
                "Tag <min> needs two numbers.");

        assertTrue(found.isPresent());
        assertEquals(5, found.getAsInt());
    }

    @Test
    void mapOpTriesBothAliasSpellings() {
        OptionalInt dotted = ErrorLocate.charInLine("say <m.get:flags,k>",
                "Unknown map op: get");
        OptionalInt full = ErrorLocate.charInLine("say <map.get:flags,k>",
                "Unknown map op: get");

        assertTrue(dotted.isPresent());
        assertEquals(5, dotted.getAsInt());
        assertTrue(full.isPresent());
        assertEquals(5, full.getAsInt());
    }

    @Test
    void setOpMatches() {
        OptionalInt found = ErrorLocate.charInLine("say <set.bogus:x>",
                "Unknown set op: bogus");

        assertTrue(found.isPresent());
        assertEquals(5, found.getAsInt());
    }

    @Test
    void headPrefixDoesNotMatchLongerRoot() {
        assertTrue(ErrorLocate.charInLine("say <minion:5>",
                "Tag <min> needs two numbers.").isEmpty());
    }

    @Test
    void blankInputsStayEmpty() {
        assertTrue(ErrorLocate.charInLine(null, "Tag <min> needs two numbers.").isEmpty());
        assertTrue(ErrorLocate.charInLine("say <min:5>", null).isEmpty());
        assertTrue(ErrorLocate.charInLine("   ", "Tag <min> needs two numbers.").isEmpty());
    }

    @Test
    void atFormatsFullClause() {
        Provenance provenance = new Provenance("swap", "main", "all", 1);

        assertEquals(" at modifier 'swap': swap/main/all @ 2:15",
                ErrorLocate.at(provenance, 15));
    }

    @Test
    void atDropsCharWhenUnknown() {
        Provenance provenance = new Provenance("swap", "main", "all", 1);

        assertEquals(" at modifier 'swap': swap/main/all @ 2",
                ErrorLocate.at(provenance, -1));
    }

    @Test
    void atDropsBlankPartsAndUnknownLine() {
        Provenance provenance = new Provenance("debuffs", "", "", -1);

        assertEquals(" at modifier 'debuffs': debuffs", ErrorLocate.at(provenance, -1));
    }
}
