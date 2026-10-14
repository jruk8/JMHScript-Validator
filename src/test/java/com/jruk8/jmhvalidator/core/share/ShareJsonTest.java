package com.jruk8.jmhvalidator.core.share;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Core JSON: Gson-identical bytes for the codec subset. The pins
 * below come from an oracle probe run against gson 2.10.1 and
 * 2.14.0 (identical output), not from this implementation.
 */
class ShareJsonTest {
    /** One backslash, so expected unicode escapes stay readable. */
    private static final String BS = "\\";

    @Test
    void escapesMatchGsonTable() {
        String tricky = "\"\\\b\f\n\r\t<>&='" + ((char) 0xE9)
                + ((char) 0x2028) + ((char) 0x2029) + ((char) 0) + ((char) 0x1F)
                + ((char) 0x7F);
        Map<String, Object> value = Map.of("s", tricky);

        String expected = "{\"s\":\"\\\"\\\\\\b\\f\\n\\r\\t"
                + BS + "u003c" + BS + "u003e" + BS + "u0026" + BS + "u003d" + BS + "u0027"
                + ((char) 0xE9) + BS + "u2028" + BS + "u2029" + BS + "u0000" + BS + "u001f"
                + ((char) 0x7F) + "\"}";

        assertEquals(expected, ShareJson.stringify(value));
    }

    @Test
    void commandLineEscapesTagBrackets() {
        assertEquals("{\"s\":\"give " + BS + "u003cp" + BS + "u003e beef 8\"}",
                ShareJson.stringify(Map.of("s", "give <p> beef 8")));
    }

    @Test
    void numbersRenderLikeGson() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("i", 5);
        value.put("l", 100L);
        value.put("d", 15.0);
        value.put("d2", 0.5);
        value.put("neg", -0.0);

        assertEquals("{\"i\":5,\"l\":100,\"d\":15.0,\"d2\":0.5,\"neg\":-0.0}",
                ShareJson.stringify(value));
    }

    @Test
    void nonFiniteNumbersThrowLikeGson() {
        assertThrows(IllegalArgumentException.class,
                () -> ShareJson.stringify(Map.of("v", Double.NaN)));
        assertThrows(IllegalArgumentException.class,
                () -> ShareJson.stringify(Map.of("v", Double.POSITIVE_INFINITY)));
    }

    @Test
    void decodedNumbersBecomeDouble() {
        Optional<Object> parsed = ShareJson.parse("{\"a\":5,\"b\":5.0,\"c\":1e3}");

        assertTrue(parsed.isPresent());
        Map<?, ?> map = (Map<?, ?>) parsed.get();
        assertEquals(5.0, map.get("a"));
        assertEquals(5.0, map.get("b"));
        assertEquals(1000.0, map.get("c"));
        assertTrue(map.get("a") instanceof Double);
        assertTrue(map.get("b") instanceof Double);
        assertTrue(map.get("c") instanceof Double);
    }

    @Test
    void overflowNumberBecomesInfinity() {
        Optional<Object> parsed = ShareJson.parse("{\"b\":1e999}");

        assertTrue(parsed.isPresent());
        assertEquals(Double.POSITIVE_INFINITY, ((Map<?, ?>) parsed.get()).get("b"));
    }

    @Test
    void stringEscapesRoundTrip() {
        String input = "\"a\\\"b" + BS + "u003cc" + BS + "ud83d" + BS + "ude00\"";

        Optional<Object> parsed = ShareJson.parse(input);

        assertTrue(parsed.isPresent());
        assertEquals("a\"b<c" + new String(Character.toChars(0x1F600)), parsed.get());
    }

    @Test
    void malformedInputsParseEmpty() {
        String[] bad = {
            "", "{", "}", "[", "]", "{\"a\":}", "{\"a\" 1}", "[1,2", "{\"a\":NaN}",
            "{\"a\":01}", "{\"a\":+1}", "{\"a\":.5}", "{\"a\":'x'}", "{\"a\":\"\\x\"}",
            "{\"a\":\"unterminated}", "{\"a\":1} trailing", "{,}", "[1,]", "[",
            "nul", "tru", "{\"a\":nul}",
        };
        for (String input : bad) {
            assertTrue(ShareJson.parse(input).isEmpty(), input);
        }
    }

    @Test
    void deepNestingParsesEmpty() {
        assertTrue(ShareJson.parse("[".repeat(101) + "]".repeat(101)).isEmpty());
    }

    @Test
    void shallowNestingParses() {
        Optional<Object> parsed = ShareJson.parse("[".repeat(5) + "]".repeat(5));

        assertTrue(parsed.isPresent());
    }

    @Test
    void literalsAndContainersParse() {
        assertTrue(ShareJson.parse("null").isEmpty());
        assertEquals(Boolean.TRUE, ShareJson.parse("true").orElseThrow());
        assertEquals(Boolean.FALSE, ShareJson.parse(" false ").orElseThrow());
        assertEquals(Map.of(), ShareJson.parse("{}").orElseThrow());
        assertEquals(List.of(), ShareJson.parse("[]").orElseThrow());
        assertEquals("e", ShareJson.parse("\"e\"").orElseThrow());
    }
}
