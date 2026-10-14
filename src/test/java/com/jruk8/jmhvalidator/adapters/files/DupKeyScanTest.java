package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.DupKeys;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compose-walk pins: repeats report path plus 1-based line,
 * merges and broken documents stay silent for other owners.
 */
class DupKeyScanTest {

    @Test
    void rootRepeatReportsKeyAndLine() {
        List<DupKeys.Hit> hits = DupKeyScan.scan("config:\n  a: 1\nconfig:\n  a: 2\n");

        assertEquals(List.of(new DupKeys.Hit("", "config", 3)), hits);
    }

    @Test
    void nestedRepeatReportsDottedPath() {
        String text = "config:\n  settings:\n    rate: 1\n    rate: 2\n";
        List<DupKeys.Hit> hits = DupKeyScan.scan(text);

        assertEquals(List.of(new DupKeys.Hit("config.settings", "rate", 4)), hits);
    }

    @Test
    void tripleRepeatReportsEachDiscardedLine() {
        List<DupKeys.Hit> hits = DupKeyScan.scan("a: 1\na: 2\na: 3\n");

        assertEquals(List.of(new DupKeys.Hit("", "a", 2), new DupKeys.Hit("", "a", 3)),
                hits);
    }

    @Test
    void sameKeyInSiblingsPasses() {
        String text = "one:\n  rate: 1\ntwo:\n  rate: 2\n";

        assertTrue(DupKeyScan.scan(text).isEmpty());
    }

    @Test
    void mergePlusOverridePasses() {
        String text = "base: &base\n  rate: 1\nchild:\n  <<: *base\n  rate: 2\n";

        assertTrue(DupKeyScan.scan(text).isEmpty());
    }

    @Test
    void brokenAndEmptyDocumentsPass() {
        assertTrue(DupKeyScan.scan("a: [1,\n").isEmpty());
        assertTrue(DupKeyScan.scan("").isEmpty());
        assertTrue(DupKeyScan.scan("   \n").isEmpty());
        assertTrue(DupKeyScan.scan("- just\n- a\n- list\n").isEmpty());
        assertTrue(DupKeyScan.scan(null).isEmpty());
    }
}
