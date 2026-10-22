package com.jruk8.jmhvalidator.core.script;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** MiniMessage marker pins: closers, hex, and resolved names pass; typos do not. */
class MiniMessageTagsTest {

    @Test
    void markersPass() {
        assertTrue(MiniMessageTags.isMarker("/red"));
        assertTrue(MiniMessageTags.isMarker("#e0dfad"));
        assertTrue(MiniMessageTags.isMarker("red"));
        assertTrue(MiniMessageTags.isMarker("light_purple"));
        assertTrue(MiniMessageTags.isMarker("bold"));
        assertTrue(MiniMessageTags.isMarker("gradient"));
        assertTrue(MiniMessageTags.isMarker("b"));
    }

    @Test
    void nonMarkersFail() {
        assertFalse(MiniMessageTags.isMarker(null));
        assertFalse(MiniMessageTags.isMarker(""));
        assertFalse(MiniMessageTags.isMarker("giv"));
        assertFalse(MiniMessageTags.isMarker("bogus"));
        assertFalse(MiniMessageTags.isMarker("red2"));
    }
}
