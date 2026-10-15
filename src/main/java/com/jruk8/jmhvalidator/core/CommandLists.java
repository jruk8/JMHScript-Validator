package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Commands-list sweep: every list in a behavior block's
 * {@code commands} map must name a dispatched list. The
 * loader keeps unknown names (the map is open) but
 * dispatch reads four fixed keys and the GUI shows the
 * same four, so unknown lists never run and never display:
 * each warns. Matching is exact, like the dispatch
 * lookup. The synthetic list the loader invents for
 * list-shaped commands stays silent here (the shape
 * check owns that block). The retired cleanup lists
 * (`console-cleanup`, `player-cleanup`) warn here like any
 * unknown list; ON_CLEANUP behaviors replaced them.
 */
public final class CommandLists {

    /** Dispatched list names, owned by the dispatch code. */
    private static final Set<String> KNOWN = Set.of("console", "player", "hunter",
            "speedrunner");

    private CommandLists() {
    }

    /** Check one block's list names, in map order. */
    public static List<Diagnostic> check(String modifierId, String blockId,
            Iterable<String> lists) {
        List<Diagnostic> found = new ArrayList<>();
        if (lists == null) {
            return found;
        }
        for (String list : lists) {
            if (list == null || KNOWN.contains(list)
                    || ShapeMessages.SYNTHETIC_LIST.equals(list)) {
                continue;
            }
            found.add(new Diagnostic(
                    new Provenance(modifierId, blockId, list, -1), -1,
                    Severity.WARNING, DiagnosticCode.W_UNKNOWN_LIST,
                    "Unknown commands list '" + list + "' in behavior '"
                            + blockId + "'; it never runs."));
        }
        return found;
    }
}
