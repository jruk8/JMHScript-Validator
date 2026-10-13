package com.jruk8.jmhvalidator.core.model;

/**
 * Where one tag line comes from: the modifier id, the behavior
 * id in that modifier, the command list name, and the 0-based
 * line index in that list. Ported from the engine's provenance
 * record; {@link #describe} output is byte-identical so old
 * location clauses render unchanged.
 */
public record Provenance(String modifierId, String behaviorId, String listName, int lineIndex) {
    /** Base provenance before any line runs: line unknown. */
    public static Provenance of(String modifierId, String behaviorId, String listName) {
        return new Provenance(modifierId, behaviorId, listName, -1);
    }

    /** Same source with the 0-based line index filled in. */
    public Provenance withLine(int lineIndex) {
        return new Provenance(modifierId, behaviorId, listName, lineIndex);
    }

    /**
     * One-line source for logs: blank path parts drop out,
     * lines are 1-based like editors, unknown lines stay
     * silent.
     */
    public String describe() {
        return describe(-1);
    }

    /**
     * Same plus a 1-based char after the line; a
     * non-positive char stays line-level.
     */
    public String describe(int charOneBased) {
        StringBuilder described = new StringBuilder("modifier '" + modifierId + "': ");
        described.append(modifierId);
        if (!behaviorId.isBlank()) {
            described.append("/").append(behaviorId);
        }
        if (!listName.isBlank()) {
            described.append("/").append(listName);
        }
        if (lineIndex >= 0) {
            described.append(" @ ").append(lineIndex + 1);
            if (charOneBased > 0) {
                described.append(":").append(charOneBased);
            }
        }
        return described.toString();
    }
}
