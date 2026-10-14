package com.jruk8.jmhvalidator.core.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TagIntercept shape pins: exactly one payload arg, quote hygiene,
 * and literal tag-length failures. Dynamic payloads abstain for
 * runtime; block context is the InterceptCheck rule.
 */
class TagInterceptValidationTest {
    @Test
    void arityAndHygiene() {
        assertTrue(TagIntercept.opError("intercept", null).isPresent());
        assertTrue(TagIntercept.opError("intercept", "").isPresent());
        assertTrue(TagIntercept.opError("intercept", "a,b").isPresent());
        assertTrue(TagIntercept.opError("intercept", "[dirt, 1]").isEmpty());
        assertTrue(TagIntercept.opError("intercept", "[[dirt, 1], [stone, 2]]").isEmpty());
        assertEquals("<intercept:item>", TagIntercept.example());
    }

    @Test
    void literalTagLengthFails() {
        String overlong = "[dirt, 1, [tag, " + "x".repeat(101) + "]]";
        assertEquals("Tag <intercept> tag value allows 100 chars at most.",
                TagIntercept.opError("intercept", overlong).orElseThrow());
        String fitting = "[dirt, 1, [tag, " + "x".repeat(100) + "]]";
        assertTrue(TagIntercept.opError("intercept", fitting).isEmpty());
        assertTrue(TagIntercept.opError("intercept", "[dirt, 1, [tag, <setting:x>]]")
                .isEmpty());
    }

    @Test
    void quotedCommasSurviveThePayload() {
        assertTrue(TagIntercept.opError("intercept", "[dirt, 64, 'a={},b={}']").isEmpty());
        String overlong = "[dirt, 1, [tag, \"" + "x".repeat(60) + "," + "y".repeat(60)
                + "\"]]";
        assertEquals("Tag <intercept> tag value allows 100 chars at most.",
                TagIntercept.opError("intercept", overlong).orElseThrow());
    }
}
