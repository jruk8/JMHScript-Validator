package com.jruk8.jmhvalidator.core.script;

/**
 * Shared line gate for reference sweeps: reference checks only
 * read lines that parse, leaving broken lines to the checks
 * that own them (brackets, then syntax, then references).
 */
public final class LineGates {

    private LineGates() {
    }

    /** Whether reference checks may read one command line. */
    public static boolean checkable(String line) {
        return line != null && BracketCheck.firstFault(line).isEmpty()
                && CommandSyntax.error(line).isEmpty();
    }
}
