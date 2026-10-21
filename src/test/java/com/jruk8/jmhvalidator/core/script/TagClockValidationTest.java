package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Clock shape pins: rule pairs, single values, and argless timeget. */
class TagClockValidationTest {
    @Test
    void clockArity() {
        assertTrue(TagWorldClock.opError("gamerule", null).isPresent());
        assertTrue(TagWorldClock.opError("rule", "doDaylightCycle").isPresent());
        assertTrue(TagWorldClock.opError("timeset", "day,extra").isPresent());
        assertTrue(TagWorldClock.opError("timeadd", null).isPresent());
        assertTrue(TagWorldClock.opError("timeget", "x").isPresent());
        assertTrue(TagWorldClock.opError("weatherset", "rain,extra").isPresent());
        assertTrue(TagWorldClock.opError("weatherset", null).isPresent());
        assertTrue(TagWorldClock.opError("weatherget", "x").isPresent());
        assertTrue(TagWorldClock.opError("gamerule", "doDaylightCycle,false").isEmpty());
        assertTrue(TagWorldClock.opError("rule", "doDaylightCycle,false").isEmpty());
        assertTrue(TagWorldClock.opError("timeset", "midnight").isEmpty());
        assertTrue(TagWorldClock.opError("timeadd", "-500").isEmpty());
        assertTrue(TagWorldClock.opError("timeget", "").isEmpty());
        assertTrue(TagWorldClock.opError("timeget", null).isEmpty());
        assertTrue(TagWorldClock.opError("weatherset", "rain").isEmpty());
        assertTrue(TagWorldClock.opError("weatherget", "").isEmpty());
        assertTrue(TagWorldClock.opError("weatherget", null).isEmpty());
    }

    @Test
    void clockBlanksRejected() {
        assertTrue(TagWorldClock.opError("gamerule", ",false").isPresent());
        assertTrue(TagWorldClock.opError("gamerule", "doDaylightCycle,").isPresent());
        assertTrue(TagWorldClock.opError("timeset", "").isPresent());
        assertTrue(TagWorldClock.opError("timeset", "'midnight").isPresent());
        assertTrue(TagWorldClock.opError("weatherset", "").isPresent());
        assertTrue(TagWorldClock.opError("weatherset", "'rain").isPresent());
    }
}
