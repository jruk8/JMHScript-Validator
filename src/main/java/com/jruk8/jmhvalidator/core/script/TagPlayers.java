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
 * {@code <pactionbar.set>} and {@code <pactionbar.clear>} own one
 * player's actionbar line;
 * {@code <pslot>} gets and sets inventory slots by name or raw index;
 * {@code <clearslot>} empties one slot by name or raw index;
 * {@code <pfx.set>}, {@code <pfx.get>}, {@code <pfx.clear>}, and
 * {@code <pfx.duration>} own one named entry in the shared effect ledger;
 * {@code <pdamage>} deals damage with an optional attacker and type;
 * {@code <pmaxhp>} gets and sets max-health id contributions;
 * {@code <pexp.get>}, {@code <pexp.add>}, and {@code <pexp.remove>}
 * read and write vanilla experience levels (never raw xp points);
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
        List<String> parts = args == null || args.isBlank()
                ? List.of() : TagLists.splitTopLevel(args);
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
            case "rstate" -> count == 2;
            case "gstate" -> count == 1;
            case "pstandingon", "rstandingon" -> count == 1;
            case "gstandingon" -> count == 0;
            case "pblock", "rblock" -> count == 1 || count == 2;
            case "gblock" -> count == 0 || count == 1;
            case "pismoving", "rismoving" -> count == 1;
            case "gismoving" -> count == 0;
            case "pisusinghanditem", "risusinghanditem" -> count == 1 || count == 2;
            case "gisusinghanditem" -> count == 0 || count == 1;
            case "premoveitem", "rremoveitem" -> count == 2 || count == 3;
            case "gremoveitem" -> count == 1 || count == 2;
            case "pdamage", "rdamage" -> count >= 2 && count <= 4;
            case "gdamage" -> count >= 1 && count <= 3;
            case "puuid", "ruuid" -> count == 1;
            case "guuid" -> count == 0;
            case "clearslot", "rclearslot" -> count == 2;
            case "gclearslot" -> count == 1;
            case "pfx.set", "rfx.set" -> count >= 2 && count <= 4;
            case "gfx.set" -> count >= 1 && count <= 3;
            case "pfx.get", "rfx.get" -> count == 2;
            case "gfx.get" -> count == 1;
            case "pfx.clear", "rfx.clear" -> count == 1 || count == 2;
            case "gfx.clear" -> count == 0 || count == 1;
            case "pfx.duration", "rfx.duration" -> count == 3;
            case "gfx.duration" -> count == 2;
            case "ptitle", "rtitle" -> count >= 3 && count <= 6;
            case "gtitle" -> count >= 2 && count <= 5;
            case "pactionbar.set", "ractionbar.set" -> count >= 2 && count <= 4;
            case "gactionbar.set" -> count >= 1 && count <= 3;
            case "pactionbar.clear", "ractionbar.clear" -> count == 1;
            case "gactionbar.clear" -> count == 0;
            case "pmaxhp.set", "pmaxhp.modify", "rmaxhp.set", "rmaxhp.modify" -> count == 3;
            case "gmaxhp.set", "gmaxhp.modify" -> count == 2;
            case "pmaxhp.get", "rmaxhp.get" -> count == 2;
            case "gmaxhp.get" -> count == 1;
            case "pmaxhp.clear", "rmaxhp.clear" -> count == 1 || count == 2;
            case "gmaxhp.clear" -> count == 0 || count == 1;
            case "pexp.get" -> count == 1;
            case "pexp.add", "pexp.remove" -> count == 2;
            case "pslot", "rslot" -> count == 2 || count == 3;
            case "gslot" -> count == 1 || count == 2;
            default -> count == 2 || count == 3;
        };
    }

    public static String arityText(String name) {
        return switch (name) {
            case "pstate", "rstate" -> "2 args";
            case "gstate" -> "1 arg";
            case "pstandingon", "rstandingon" -> "1 arg";
            case "gstandingon" -> "no args";
            case "pblock", "rblock" -> "1 or 2 args";
            case "gblock" -> "0 or 1 args";
            case "pismoving", "rismoving" -> "1 arg";
            case "gismoving" -> "no args";
            case "pisusinghanditem", "risusinghanditem" -> "1 or 2 args";
            case "gisusinghanditem" -> "0 or 1 args";
            case "premoveitem", "rremoveitem" -> "2 or 3 args";
            case "gremoveitem" -> "1 or 2 args";
            case "pdamage", "rdamage" -> "2 to 4 args";
            case "gdamage" -> "1 to 3 args";
            case "puuid", "ruuid" -> "1 arg";
            case "guuid" -> "no args";
            case "clearslot", "rclearslot" -> "2 args";
            case "gclearslot" -> "1 arg";
            case "pfx.set", "rfx.set" -> "2 to 4 args";
            case "gfx.set" -> "1 to 3 args";
            case "pfx.get", "rfx.get" -> "2 args";
            case "gfx.get" -> "1 arg";
            case "pfx.clear", "rfx.clear" -> "1 or 2 args";
            case "gfx.clear" -> "0 or 1 args";
            case "pfx.duration", "rfx.duration" -> "3 args";
            case "gfx.duration" -> "2 args";
            case "ptitle", "rtitle" -> "3 to 6 args";
            case "gtitle" -> "2 to 5 args";
            case "pactionbar.set", "ractionbar.set" -> "2 to 4 args";
            case "gactionbar.set" -> "1 to 3 args";
            case "pactionbar.clear", "ractionbar.clear" -> "1 arg";
            case "gactionbar.clear" -> "no args";
            case "pmaxhp.set", "pmaxhp.modify", "rmaxhp.set", "rmaxhp.modify" -> "3 args";
            case "gmaxhp.set", "gmaxhp.modify" -> "2 args";
            case "pmaxhp.get", "rmaxhp.get" -> "2 args";
            case "gmaxhp.get" -> "1 arg";
            case "pmaxhp.clear", "rmaxhp.clear" -> "1 or 2 args";
            case "gmaxhp.clear" -> "0 or 1 args";
            case "pexp.get" -> "1 arg";
            case "pexp.add", "pexp.remove" -> "2 args";
            default -> "2 or 3 args";
        };
    }

    public static String example(String name) {
        return switch (name) {
            case "pstate" -> "<pstate:player,state>";
            case "rstate" -> "<rstate:role,state>";
            case "gstate" -> "<gstate:state>";
            case "pstandingon" -> "<pstandingon:player>";
            case "rstandingon" -> "<rstandingon:role>";
            case "gstandingon" -> "<gstandingon>";
            case "pblock" -> "<pblock:player,type>";
            case "rblock" -> "<rblock:role,type>";
            case "gblock" -> "<gblock:type>";
            case "pismoving" -> "<pismoving:player>";
            case "rismoving" -> "<rismoving:role>";
            case "gismoving" -> "<gismoving>";
            case "pisusinghanditem" -> "<pisusinghanditem:player,item>";
            case "risusinghanditem" -> "<risusinghanditem:role,item>";
            case "gisusinghanditem" -> "<gisusinghanditem:item>";
            case "premoveitem" -> "<premoveitem:player,item,count>";
            case "rremoveitem" -> "<rremoveitem:role,item,count>";
            case "gremoveitem" -> "<gremoveitem:item,count>";
            case "pdamage" -> "<pdamage:player,amount>";
            case "rdamage" -> "<rdamage:role,amount>";
            case "gdamage" -> "<gdamage:amount>";
            case "puuid" -> "<puuid:player>";
            case "ruuid" -> "<ruuid:role>";
            case "guuid" -> "<guuid>";
            case "clearslot" -> "<clearslot:player,slot>";
            case "rclearslot" -> "<rclearslot:role,slot>";
            case "gclearslot" -> "<gclearslot:slot>";
            default -> exampleLedger(name);
        };
    }

    /** Examples for the ledger, title, max-health, slot, actionbar, and exp twins. */
    private static String exampleLedger(String name) {
        return switch (name) {
            case "pfx.set" -> "<pfx.set:player,id,effects>";
            case "rfx.set" -> "<rfx.set:role,id,effects>";
            case "gfx.set" -> "<gfx.set:id,effects>";
            case "pfx.get" -> "<pfx.get:player,id>";
            case "rfx.get" -> "<rfx.get:role,id>";
            case "gfx.get" -> "<gfx.get:id>";
            case "pfx.clear" -> "<pfx.clear:player,id>";
            case "rfx.clear" -> "<rfx.clear:role,id>";
            case "gfx.clear" -> "<gfx.clear:id>";
            case "pfx.duration" -> "<pfx.duration:player,id,effect>";
            case "rfx.duration" -> "<rfx.duration:role,id,effect>";
            case "gfx.duration" -> "<gfx.duration:id,effect>";
            case "ptitle" -> "<ptitle:player,title,subtitle>";
            case "rtitle" -> "<rtitle:role,title,subtitle>";
            case "gtitle" -> "<gtitle:title,subtitle>";
            case "pactionbar.set" -> "<pactionbar.set:Steve,Go!>";
            case "ractionbar.set" -> "<ractionbar.set:hunter,Go!>";
            case "gactionbar.set" -> "<gactionbar.set:Go!>";
            case "pactionbar.clear" -> "<pactionbar.clear:Steve>";
            case "ractionbar.clear" -> "<ractionbar.clear:hunter>";
            case "gactionbar.clear" -> "<gactionbar.clear>";
            case "pmaxhp.set" -> "<pmaxhp.set:player,id,amount>";
            case "rmaxhp.set" -> "<rmaxhp.set:role,id,amount>";
            case "gmaxhp.set" -> "<gmaxhp.set:id,amount>";
            case "pmaxhp.modify" -> "<pmaxhp.modify:player,id,amount>";
            case "rmaxhp.modify" -> "<rmaxhp.modify:role,id,amount>";
            case "gmaxhp.modify" -> "<gmaxhp.modify:id,amount>";
            case "pmaxhp.get" -> "<pmaxhp.get:player,id>";
            case "rmaxhp.get" -> "<rmaxhp.get:role,id>";
            case "gmaxhp.get" -> "<gmaxhp.get:id>";
            case "pmaxhp.clear" -> "<pmaxhp.clear:player,id>";
            case "rmaxhp.clear" -> "<rmaxhp.clear:role,id>";
            case "gmaxhp.clear" -> "<gmaxhp.clear:id>";
            case "pexp.get" -> "<pexp.get:Steve>";
            case "pexp.add", "pexp.remove" -> "<" + name + ":Steve,5>";
            case "rslot" -> "<rslot:role,slot>";
            case "gslot" -> "<gslot:slot>";
            default -> "<pslot:player,slot>";
        };
    }
}
