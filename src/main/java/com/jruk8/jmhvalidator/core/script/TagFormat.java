package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * {@code <format:string,params>}: plain {@code {0}}, {@code {1}}
 * substitution with no conversion specifiers. Params accept a
 * {@code [...]} list or a bare single value. Indexes that do not
 * exist, non-numeric braces, and stray braces stay verbatim with no
 * warning; replacement text is never rescanned. Malformed tag shape
 * (wrong arity, bad quotes) warns plus {@code "null"}. No Bukkit
 * types.
  * Validation carve: evaluation stays engine-side.
*/
final class TagFormat {

    private TagFormat() {
    }


    /** Syntax check mirroring the runtime shape rules. */
    public static Optional<String> syntaxError(String name, String args) {
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 2) {
            return Optional.of("Tag <format> needs a string plus params like "
                    + "<format:\"{0} found {1}\",[Alex,gold]>");
        }
        if (TagHeads.parsePickItem(parts.get(0)).isEmpty()
                || TagHeads.parsePickItem(parts.get(1)).isEmpty()) {
            return Optional.of("Tag <format> needs a string plus params like "
                    + "<format:\"{0} found {1}\",[Alex,gold]>");
        }
        return Optional.empty();
    }
}
