package com.jruk8.jmhvalidator.core.model;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Catalog honesty: every diagnostic code and title appears in
 * docs/error-catalog.md, so the prose cannot drift from the
 * contract.
 */
class ErrorCatalogTest {
    @Test
    void catalogListsEveryCode() throws Exception {
        String catalog = Files.readString(Paths.get("docs/error-catalog.md"),
                StandardCharsets.UTF_8);
        for (DiagnosticCode code : DiagnosticCode.values()) {
            assertTrue(catalog.contains("`" + code.name() + "`"),
                    "catalog lists " + code.name());
            assertTrue(catalog.contains(code.title()),
                    "catalog titles " + code.name());
        }
    }
}
