package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.ports.KnownTriggers;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs-on sweep: every trigger in a behavior block's
 * {@code runs-on} list must name an engine-dispatched event.
 * Unknown triggers pass through and never match an event, so
 * each warns; removed or renamed triggers warn apart so the
 * author can tell a stale mod from a typo. A null trigger
 * port (a registry without the triggers array) stays silent,
 * like preset checks without fetched defaults.
 */
public final class RunTriggers {

    private RunTriggers() {
    }

    /** Check one block's triggers, in list order. */
    public static List<Diagnostic> check(String modifierId, String blockId,
            List<String> runsOn, KnownTriggers known) {
        List<Diagnostic> found = new ArrayList<>();
        if (runsOn == null || known == null) {
            return found;
        }
        for (String trigger : runsOn) {
            if (trigger == null || known.isKnown(trigger)) {
                continue;
            }
            if (known.isRemoved(trigger)) {
                found.add(new Diagnostic(
                        new Provenance(modifierId, blockId, "", -1), -1,
                        Severity.WARNING, DiagnosticCode.W_REMOVED_TRIGGER,
                        "Trigger '" + trigger.trim()
                                + "' was removed or renamed; it never fires."));
                continue;
            }
            found.add(new Diagnostic(
                    new Provenance(modifierId, blockId, "", -1), -1,
                    Severity.WARNING, DiagnosticCode.W_UNKNOWN_TRIGGER,
                    "Unknown trigger '" + trigger.trim() + "', it never fires."));
        }
        return found;
    }
}
