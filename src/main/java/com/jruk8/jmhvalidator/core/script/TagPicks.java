package com.jruk8.jmhvalidator.core.script;

import java.util.List;

/**
 * Random-pick candidate items for validation: a bracketed list
 * parses with list semantics, anything else with the quote-aware
 * comma split. Draws stay engine-side.
 */
public final class TagPicks {


    private TagPicks() {
    }


    /**
     * Candidate items for a pick: a bracketed list literal parses with the
     * shared list splitter, anything else with the quote-aware comma split.
     */
    static List<String> pickItems(String args) {
        if (TagLists.isList(args)) {
            return TagLists.parse(args);
        }
        return TagHeads.splitPickArgs(args);
    }
}
