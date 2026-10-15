package com.jruk8.jmhvalidator.core.script;

import java.util.List;
import java.util.Optional;

/**
 * Player body and inventory tags: {@code <pstate>} reads sneaking,
 * sprinting, gliding, swimming, grounded, and idle; {@code <pstandingon>}
 * reads the block below the feet; {@code <pblock>} reads the block
 * at the feet, head, or below; {@code <pismoving>} reads movement
 * since the modifier's own last check; {@code <pisusinghanditem>}
 * reads held item use with an optional material filter;
 * {@code <premoveitem>} removes
 * items all-or-nothing; {@code <ptitle>} sends a center-screen title;
 * {@code <pslot>} gets and sets inventory slots by name or raw index;
 * {@code <clearslot>} empties one slot by name or raw index;
 * {@code <pdamage>} deals damage with an optional attacker and type;
 * {@code <pmaxhp>} gets and sets max-health id contributions;
 * {@code <puuid>} reads the Mojang id as four ints. Reads resolve
 * {@code null} silently for offline or unknown players; writes warn.
 * No Bukkit types: every read and write runs through the context
 * backends.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagPlayers {


    private TagPlayers() {
    }


    /**
     * Edit-time arity for the player tags: top-level split, quote
     * hygiene, plus the per-tag shape. Mirrors the runtime warns.
     */
    public static Optional<String> opError(String name, String args) {
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
        return Optional.empty();
    }

    public static boolean arityOk(String name, int count) {
        return switch (name) {
            case "pstate" -> count == 2;
            case "pstandingon" -> count == 1;
            case "pblock" -> count == 1 || count == 2;
            case "pismoving" -> count == 1;
            case "pisusinghanditem" -> count == 1 || count == 2;
            case "premoveitem" -> count == 2 || count == 3;
            case "pdamage" -> count >= 2 && count <= 4;
            case "puuid" -> count == 1;
            case "clearslot" -> count == 2;
            case "ptitle" -> count >= 3 && count <= 6;
            case "pmaxhp.set", "pmaxhp.modify" -> count == 3;
            case "pmaxhp.get" -> count == 2;
            case "pmaxhp.clear" -> count == 1 || count == 2;
            default -> count == 2 || count == 3;
        };
    }

    public static String arityText(String name) {
        return switch (name) {
            case "pstate" -> "2 args";
            case "pstandingon" -> "1 arg";
            case "pblock" -> "1 or 2 args";
            case "pismoving" -> "1 arg";
            case "pisusinghanditem" -> "1 or 2 args";
            case "premoveitem" -> "2 or 3 args";
            case "pdamage" -> "2 to 4 args";
            case "puuid" -> "1 arg";
            case "clearslot" -> "2 args";
            case "ptitle" -> "3 to 6 args";
            case "pmaxhp.set", "pmaxhp.modify" -> "3 args";
            case "pmaxhp.get" -> "2 args";
            case "pmaxhp.clear" -> "1 or 2 args";
            default -> "2 or 3 args";
        };
    }

    public static String example(String name) {
        return switch (name) {
            case "pstate" -> "<pstate:player,state>";
            case "pstandingon" -> "<pstandingon:player>";
            case "pblock" -> "<pblock:player,type>";
            case "pismoving" -> "<pismoving:player>";
            case "pisusinghanditem" -> "<pisusinghanditem:player,item>";
            case "premoveitem" -> "<premoveitem:player,item,count>";
            case "pdamage" -> "<pdamage:player,amount>";
            case "puuid" -> "<puuid:player>";
            case "clearslot" -> "<clearslot:player,slot>";
            case "ptitle" -> "<ptitle:player,title,subtitle>";
            case "pmaxhp.set" -> "<pmaxhp.set:player,id,amount>";
            case "pmaxhp.modify" -> "<pmaxhp.modify:player,id,amount>";
            case "pmaxhp.get" -> "<pmaxhp.get:player,id>";
            case "pmaxhp.clear" -> "<pmaxhp.clear:player,id>";
            default -> "<pslot:player,slot>";
        };
    }
}
