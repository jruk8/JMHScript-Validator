package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.ports.InterceptableTriggers;
import com.jruk8.jmhvalidator.core.script.CommandSyntax;
import com.jruk8.jmhvalidator.core.script.ErrorLocate;
import com.jruk8.jmhvalidator.core.script.TagHeads;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Intercept-context sweep: an {@code <intercept>} tag only works in
 * a behavior block whose {@code runs-on} names an interceptable
 * event (the engine arms the collector on those dispatches alone).
 * Any other block errors, because the tag would warn and do
 * nothing at runtime. A null interceptable port (a registry
 * without the array) stays silent, like trigger checks without
 * fetched defaults. One finding per block, in file order.
 */
public final class InterceptCheck {

    private InterceptCheck() {
    }

    /** Check one block's lines for out-of-context intercepts. */
    public static List<Diagnostic> check(String modifierId, BehaviorFile.BehaviorBlock block,
            String blockId, InterceptableTriggers known) {
        List<Diagnostic> found = new ArrayList<>();
        if (block == null || block.lists() == null || known == null) {
            return found;
        }
        if (hasInterceptable(block.runsOn(), known)) {
            return found;
        }
        for (Map.Entry<String, List<String>> list : block.lists().entrySet()) {
            if (list.getValue() == null) {
                continue;
            }
            for (int index = 0; index < list.getValue().size(); index++) {
                String line = list.getValue().get(index);
                if (line != null && intercepts(line)) {
                    Provenance provenance =
                            new Provenance(modifierId, blockId, list.getKey(), index);
                    int at = ErrorLocate.charInLine(line, "<intercept>").orElse(-1);
                    found.add(new Diagnostic(provenance, at, Severity.ERROR,
                            DiagnosticCode.E_INTERCEPT_CONTEXT,
                            "Tag <intercept> runs only in an interceptable block "
                                    + triggerList(known) + "."));
                    return found;
                }
            }
        }
        return found;
    }

    /** Parenthesized trigger names in port order, with an or before the last. */
    private static String triggerList(InterceptableTriggers known) {
        List<String> names = new ArrayList<>(known.triggers());
        if (names.isEmpty()) {
            return "(none)";
        }
        if (names.size() == 1) {
            return "(" + names.get(0) + ")";
        }
        return "(" + String.join(", ", names.subList(0, names.size() - 1)) + ", or "
                + names.get(names.size() - 1) + ")";
    }

    /** True when any runs-on trigger is interceptable. */
    private static boolean hasInterceptable(List<String> runsOn, InterceptableTriggers known) {
        if (runsOn == null) {
            return false;
        }
        for (String trigger : runsOn) {
            if (trigger != null && known.isInterceptable(trigger)) {
                return true;
            }
        }
        return false;
    }

    /** True when any tag body on the line heads an intercept tag. */
    private static boolean intercepts(String line) {
        for (String body : CommandSyntax.tagBodies(line)) {
            if ("intercept".equals(TagHeads.tagName(body))) {
                return true;
            }
        }
        return false;
    }
}
