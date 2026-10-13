package com.jruk8.jmhvalidator.core.script;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Editor line validation: the fatal check first, then the unknown
 * root and give item checks over injected data. The engine
 * supplies roots and materials from the live server.
 */
public final class CommandValidation {

    private CommandValidation() {
    }

    /**
     * Full line check: the placeholder fatal error first, then (only
     * when the editor setting allows) the unknown root and give item
     * checks. Pure for tests.
     */
    public static Optional<String> validateLine(String raw, boolean validateCommands,
            Set<String> knownRoots, Predicate<String> knownMaterial,
            Collection<String> materialNames) {
        Optional<String> problem = CommandSyntax.error(raw);
        if (problem.isPresent() || !validateCommands) {
            return problem;
        }
        String substituted = EngineEscapes.substitute(raw);
        Optional<String> root = SyntaxSuggest.unknownRoot(substituted, knownRoots);
        if (root.isPresent()) {
            return root.map(EngineEscapes::restore);
        }
        return SyntaxSuggest.giveItemCheck(substituted, knownMaterial, materialNames)
                .map(EngineEscapes::restore);
    }


}
