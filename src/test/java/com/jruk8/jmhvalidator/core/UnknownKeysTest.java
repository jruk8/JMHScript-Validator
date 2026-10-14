package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unknown-key pins: every level flags a bogus key and stays
 * silent on its full universe; messages and provenance carry
 * the level location.
 */
class UnknownKeysTest {

    @Test
    void everyLevelFlagsBogusKey() {
        for (UnknownKeys.Level level : UnknownKeys.Level.values()) {
            List<Diagnostic> found = UnknownKeys.check("fog", level, "main",
                    "targeting.rate", List.of("bogus-key"));

            assertEquals(1, found.size(), level.name());
            Diagnostic only = found.get(0);
            assertEquals(DiagnosticCode.W_UNKNOWN_KEY, only.code(), level.name());
            assertEquals(Severity.WARNING, only.severity(), level.name());
            assertTrue(only.message().startsWith("Unknown key 'bogus-key' in "),
                    level.name());
            assertTrue(only.message().endsWith("; ignoring it."), level.name());
        }
    }

    @Test
    void everyUniverseStaysSilent() {
        for (UnknownKeys.Level level : UnknownKeys.Level.values()) {
            List<Diagnostic> found = UnknownKeys.check("fog", level, "main",
                    "targeting.rate", new ArrayList<>(level.known()));

            assertTrue(found.isEmpty(), level.name());
        }
    }

    @Test
    void messagesNameLevelLocations() {
        assertEquals("Unknown key 'conflg' in the file root; ignoring it.",
                message(UnknownKeys.Level.MODIFIER_ROOT, "conflg"));
        assertEquals("Unknown key 'runs-onn' in behavior 'main'; ignoring it.",
                message(UnknownKeys.Level.BLOCK, "runs-onn"));
        assertEquals("Unknown key 'chancee' in behavior 'main' options.success-chance;"
                + " ignoring it.", message(UnknownKeys.Level.SUCCESS_CHANCE, "chancee"));
        assertEquals("Unknown key 'valeu' in setting 'targeting.rate' def.value;"
                + " ignoring it.", message(UnknownKeys.Level.SPEC, "valeu"));
    }

    @Test
    void provenanceShapesMatchLevels() {
        Diagnostic root = UnknownKeys.check("fog", UnknownKeys.Level.MODIFIER_ROOT,
                "main", "targeting.rate", List.of("conflg")).get(0);
        assertEquals("", root.provenance().behaviorId());
        assertEquals("", root.provenance().listName());

        Diagnostic block = UnknownKeys.check("fog", UnknownKeys.Level.OPTIONS,
                "main", "", List.of("delays")).get(0);
        assertEquals("main", block.provenance().behaviorId());
        assertEquals("options", block.provenance().listName());

        Diagnostic setting = UnknownKeys.check("fog", UnknownKeys.Level.SPEC,
                "", "targeting.rate", List.of("valeu")).get(0);
        assertEquals("", setting.provenance().behaviorId());
        assertEquals("targeting.rate", setting.provenance().listName());
    }

    private static String message(UnknownKeys.Level level, String key) {
        return UnknownKeys.check("fog", level, "main", "targeting.rate",
                List.of(key)).get(0).message();
    }
}
