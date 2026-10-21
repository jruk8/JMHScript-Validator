package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** String shape pins: listformat triples and single-arg timeformat. */
class TagStringsValidationTest {
    @Test
    void formatterArity() {
        assertTrue(TagStrings.opError("string.listformat", null).isPresent());
        assertTrue(TagStrings.opError("string.listformat", "[a],[b],[c],[d]").isPresent());
        assertTrue(TagStrings.opError("string.timeformat", null).isPresent());
        assertTrue(TagStrings.opError("string.timeformat", "90,extra").isPresent());
        assertTrue(TagStrings.opError("s.tformat", "90,extra").isPresent());
        assertTrue(TagStrings.opError("string.listformat", "[a,b,c]").isEmpty());
        assertTrue(TagStrings.opError("s.lformat", "[a],red,gray").isEmpty());
        assertTrue(TagStrings.opError("string.timeformat", "90").isEmpty());
        assertTrue(TagStrings.opError("string.tformat", "90").isEmpty());
        assertTrue(TagStrings.opError("s.timeformat", "90").isEmpty());
        assertTrue(TagStrings.opError("s.tformat", "90").isEmpty());
    }

    @Test
    void formatterQuotesRejected() {
        assertTrue(TagStrings.opError("string.listformat", "[a],'b").isPresent());
        assertTrue(TagStrings.opError("string.timeformat", "'90").isPresent());
    }
}
