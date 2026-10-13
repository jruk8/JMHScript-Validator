package com.jruk8.jmhvalidator.core.script;

import com.jruk8.jmhvalidator.adapters.files.TagRegistryFiles;
import com.jruk8.jmhvalidator.core.ports.TagRegistry;
import java.nio.file.Paths;

/**
 * Test registries: the pinned engine table for ported tests.
 * Ported unknown-tag assertions need the full table, not a
 * hand subset, so they load the same resource the corpus
 * parity test uses.
 */
final class TestRegistries {
    private TestRegistries() {
    }

    /** Full pinned table (228 tags at characterization). */
    static TagRegistry pinned() {
        try {
            return TagRegistryFiles.read(
                    Paths.get("src/test/resources/tag-registry.json")).registry();
        } catch (Exception failed) {
            throw new IllegalStateException("test registry missing", failed);
        }
    }
}
