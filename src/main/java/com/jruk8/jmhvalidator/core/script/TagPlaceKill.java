package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Placement and attributed-kill tags for JMHScript:
 * {@code <place:loc,target>} and {@code <kill:player,by?,force?>}.
 * The place loc shares the summon grammar (located lists,
 * triples, and bare coords); the place target is a block
 * material or {@code structure:<id>}. The kill force flag is an
 * optional boolean, default false. Masked nested tags abstain
 * (runtime resolves them).
 * Validation carve: evaluation stays engine-side.
 */
public final class TagPlaceKill {

    private TagPlaceKill() {
    }

    /**
     * Edit-time arity for the place and kill tags: top-level
     * split, quote hygiene, plus the per-head shape. Mirrors the
     * runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
        return switch (name) {
            case "place" -> placeError(args);
            case "kill" -> killError(args);
            default -> Optional.empty();
        };
    }

    /** Place: loc plus target, or a bare triple plus target. */
    private static Optional<String> placeError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <place> needs a loc and a target like "
                    + "<place:0 64 0,stone>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        String target;
        if (parts.size() == 2) {
            if (TagHeads.parsePickItem(parts.get(0)).isEmpty()
                    && !parts.get(0).isBlank()) {
                return Optional.of("Tag <place> mixes quotes.");
            }
            if (parts.get(0).isBlank()) {
                return Optional.of("Tag <place> needs a loc like "
                        + "<place:0 64 0,stone>.");
            }
            target = parts.get(1);
        } else if (parts.size() == 4 && bareTriple(parts)) {
            target = parts.get(3);
        } else {
            return Optional.of("Tag <place> needs a loc and a target like "
                    + "<place:0 64 0,stone>.");
        }
        if (TagHeads.parsePickItem(target).isEmpty() && !target.isBlank()) {
            return Optional.of("Tag <place> mixes quotes.");
        }
        if (target.isBlank()) {
            return Optional.of("Tag <place> needs a target like "
                    + "<place:0 64 0,stone>.");
        }
        return Optional.empty();
    }

    /** Kill: player, optional killer, optional force flag. */
    private static Optional<String> killError(String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <kill> needs a player like "
                    + "<kill:Steve,Alex,true>.");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 1 || parts.size() > 3) {
            return Optional.of("Tag <kill> needs 1 to 3 args like "
                    + "<kill:Steve,Alex,true>.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty() && !part.isBlank()) {
                return Optional.of("Tag <kill> mixes quotes.");
            }
        }
        if (parts.get(0).isBlank()) {
            return Optional.of("Tag <kill> needs a player like "
                    + "<kill:Steve,Alex,true>.");
        }
        if (parts.size() >= 2 && parts.get(1).isBlank()) {
            return Optional.of("Tag <kill> needs a killer like <kill:Steve,Alex>.");
        }
        if (parts.size() == 3) {
            Optional<String> flag = TagHeads.parsePickItem(parts.get(2));
            String value = flag.map(item -> item.strip()).orElse("");
            if (!value.contains("?") && !value.equalsIgnoreCase("true")
                    && !value.equalsIgnoreCase("false")) {
                return Optional.of("Tag <kill> force wants true or false.");
            }
        }
        return Optional.empty();
    }

    /** True when the head three segments all read as coord components. */
    private static boolean bareTriple(List<String> parts) {
        return coordShaped(parts.get(0)) && coordShaped(parts.get(1))
                && coordShaped(parts.get(2));
    }

    /** Coord component: blank-safe digits, tildes, or carets. */
    private static boolean coordShaped(String part) {
        String text = part.strip();
        if (text.isEmpty()) {
            return false;
        }
        char head = text.charAt(0);
        if (head == '~' || head == '^') {
            return true;
        }
        return head == '-' || head == '+' || head == '.'
                || (head >= '0' && head <= '9');
    }
}
