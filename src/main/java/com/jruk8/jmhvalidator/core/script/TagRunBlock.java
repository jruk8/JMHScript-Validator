package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Block chaining behind {@code <runblock:id>}: runs another
 * behavior block of the same modifier inline, in the current run
 * context (same executor). The one-part form inherits the caller
 * event args; {@code <runblock:id,arg1,arg2>} rebinds
 * {@code <args:N>} inside the target (override-all). Args arrive
 * tag-resolved by the inner passes, so loop items and line flags
 * in arg position evaluate in the caller context before the hop;
 * one surrounding quote layer is stripped, inner quotes pass
 * through, and the caller list itself is never mutated. Each hop
 * consumes one
 * shared line step, so runaway chains trip the standard loop
 * limit; cycle and unknown-id policy lives in the match sink.
 * Returns empty.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagRunBlock {

    private TagRunBlock() {
    }

    /**
     * Edit-time shape for {@code <runblock>}: one behavior id plus
     * optional args, quotes parsed. Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs a behavior id like "
                    + "<runblock:broadcast>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.isEmpty()) {
            return Optional.of("Tag <" + name + "> needs a behavior id like "
                    + "<runblock:broadcast>.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

}
