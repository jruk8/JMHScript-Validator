package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.ports.TagRegistry;
import com.jruk8.jmhvalidator.core.script.CommandSyntax;
import com.jruk8.jmhvalidator.core.script.EngineEscapes;
import com.jruk8.jmhvalidator.core.script.TagHeads;
import com.jruk8.jmhvalidator.core.script.TagPaidArgs;
import java.util.List;

/**
 * Premium-content probe behind the CLI marker: a modifier is
 * premium when any behavior line uses a paid head or a
 * statically visible paid arg (Bukkit stat keys, JMH
 * {@code tag}/{@code keep} values, static {@code schematic:}
 * place targets). Mirrors the engine load refusal 1:1, so a
 * marked mod is one free would refuse whole. Pure and never
 * throws.
 */
public final class PaidContent {

    private PaidContent() {
    }

    /** True when any line of the file uses paid content. */
    public static boolean isPremium(BehaviorFile file, TagRegistry registry) {
        if (file == null || file.blocks() == null || registry == null) {
            return false;
        }
        for (BehaviorFile.BehaviorBlock block : file.blocks().values()) {
            if (block == null || block.lists() == null) {
                continue;
            }
            for (List<String> lines : block.lists().values()) {
                if (lines == null) {
                    continue;
                }
                for (String line : lines) {
                    if (isPremiumLine(line, registry)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** True when one line uses paid heads or paid args. */
    static boolean isPremiumLine(String line, TagRegistry registry) {
        if (line == null) {
            return false;
        }
        try {
            for (String body : CommandSyntax.tagBodies(EngineEscapes.substitute(line))) {
                String name = TagHeads.tagName(body);
                if (registry.isPaid(name)) {
                    return true;
                }
                int separator = body.indexOf(':');
                String args = separator < 0 ? null : body.substring(separator + 1);
                if (TagPaidArgs.hasPaidArgs(name, args)) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
            // Defensive: validators report real syntax faults; the probe never breaks a run.
        }
        return false;
    }
}
