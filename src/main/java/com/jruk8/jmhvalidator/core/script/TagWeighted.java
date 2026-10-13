package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Weighted draws for JMHScript: {@code <weighted-pick:list>} returns
 * one payload, {@code <weighted-picks:list,n,unique>} returns n
 * payloads as a list. Entries are {@code [weight, payload]} pairs;
 * weights are finite doubles and payloads ride verbatim (nested
 * lists like {@code [diamond, 3, 5]} survive unflattened). Zero and
 * negative weights never win. Unique draws remove each winner from
 * the pool; repeat draws roll independently. Uses the scope random
 * source, so seeded scopes stay deterministic.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagWeighted {

    private TagWeighted() {
    }


    private static String arityText(String name) {
        return name.equals("weighted-picks") ? "2 to 3 args" : "1 arg";
    }

    private static String example(String name) {
        return name.equals("weighted-picks")
                ? "<weighted-picks:[[10.0, dirt], [2.0, diamond]],2,true>"
                : "<weighted-pick:[[10.0, dirt], [2.0, diamond]]>";
    }

    /**
     * Edit-time arity for the weighted tags: top-level split, quote
     * hygiene, plus the per-head shape. Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        int minArity = name.equals("weighted-picks") ? 2 : 1;
        int maxArity = name.equals("weighted-picks") ? 3 : 1;
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs " + arityText(name) + " like "
                    + example(name) + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < minArity || parts.size() > maxArity) {
            return Optional.of("Tag <" + name + "> needs " + arityText(name) + " like "
                    + example(name) + ".");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }
}
