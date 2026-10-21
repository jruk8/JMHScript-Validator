package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * World-clock tags for JMHScript: {@code <gamerule:name,value>}
 * and its {@code <rule>} alias, {@code <timeset:value>},
 * {@code <timeadd:value>}, and argless {@code <timeget>},
 * plus {@code <weatherset:mode>} and argless {@code <weatherget>}.
 * Validation carve: evaluation stays engine-side.
 */
public final class TagWorldClock {

    private TagWorldClock() {
    }

    /**
     * Edit-time shape for the clock tags: quote hygiene plus
     * per-head arity. Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        if (name.equals("timeget") || name.equals("weatherget")) {
            if (args != null && !args.isBlank()) {
                return Optional.of("Tag <" + name + "> takes no args like <" + name + ">.");
            }
            return Optional.empty();
        }
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs " + arityText(name) + " like "
                    + example(name) + ".");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (!arityOk(name, parts.size())) {
            return Optional.of("Tag <" + name + "> needs " + arityText(name) + " like "
                    + example(name) + ".");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        if (parts.get(0).isBlank()) {
            return Optional.of("Tag <" + name + "> needs " + arityText(name) + " like "
                    + example(name) + ".");
        }
        if ((name.equals("gamerule") || name.equals("rule")) && parts.get(1).isBlank()) {
            return Optional.of("Tag <" + name + "> needs a name and a value like "
                    + example(name) + ".");
        }
        return Optional.empty();
    }

    /** Arity behind one clock head. */
    static boolean arityOk(String name, int count) {
        return switch (name) {
            case "gamerule", "rule" -> count == 2;
            default -> count == 1;
        };
    }

    /** Arity prose behind one clock head. */
    static String arityText(String name) {
        return switch (name) {
            case "gamerule", "rule" -> "a name and a value";
            default -> "one value";
        };
    }

    /** Usage example behind one clock head. */
    static String example(String name) {
        return switch (name) {
            case "gamerule" -> "<gamerule:doDaylightCycle,false>";
            case "rule" -> "<rule:doDaylightCycle,false>";
            case "timeset" -> "<timeset:midnight>";
            case "weatherset" -> "<weatherset:rain>";
            default -> "<timeadd:-500>";
        };
    }
}
