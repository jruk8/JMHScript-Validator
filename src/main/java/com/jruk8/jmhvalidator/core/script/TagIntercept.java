package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Intercept tag for JMHScript: {@code <intercept:item>} offers one
 * item payload (a single item or a nested list) to the event's drop
 * collector. Shape only; whether the enclosing block may intercept
 * is the context rule in {@code InterceptCheck}.
 * Validation carve: evaluation stays engine-side.
 */
public final class TagIntercept {

    private TagIntercept() {
    }

    /**
     * Edit-time arity for the intercept tag: exactly one arg, quote
     * hygiene, plus literal tag-length checks (literals only; dynamic
     * values validate at runtime). Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <intercept> needs 1 arg like " + example() + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of("Tag <intercept> needs 1 arg like " + example() + ".");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <intercept> mixes quotes.");
            }
        }
        return TagItemOps.tagLengthError("intercept", parts.get(0));
    }

    public static String example() {
        return "<intercept:item>";
    }
}
