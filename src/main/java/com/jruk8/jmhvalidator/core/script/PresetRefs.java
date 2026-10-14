package com.jruk8.jmhvalidator.core.script;

import com.jruk8.jmhvalidator.core.ports.KnownModifiers;
import java.util.ArrayList;
import java.util.List;

/**
 * Preset member checks: every preset {@code modifiers} entry must
 * resolve to a known modifier id. The engine drops unknown members
 * at load with a warning and keeps the file list untouched, so a
 * later reload picks members up once their files exist; the message
 * below stays byte-identical to that load warning.
 */
public final class PresetRefs {
    private PresetRefs() {
    }

    /** Engine-identical warning for one unknown preset member. */
    public static String unknownMessage(String presetId, String member) {
        return "Preset '" + presetId + "' references unknown modifier '"
                + member + "'; skipping.";
    }

    /** One warning per unknown member, in member order. */
    public static List<String> unknownMembers(String presetId,
            List<String> members, KnownModifiers known) {
        List<String> warnings = new ArrayList<>();
        if (members == null || known == null) {
            return warnings;
        }
        for (String member : members) {
            if (member != null && !known.isKnown(member)) {
                warnings.add(unknownMessage(presetId, member));
            }
        }
        return warnings;
    }
}
