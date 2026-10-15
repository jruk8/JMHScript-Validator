package com.jruk8.jmhvalidator.core.script;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Role and global twin arity: every twin mirrors its runtime shape. */
class TwinSyntaxTest {

    @Test
    void twinReadsPass() {
        assertTrue(CommandSyntax.error("say <rstate:hunter,state> <gstate:state> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rstandingon:hunter> <gstandingon> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rblock:hunter,feet> <gblock> <gblock:feet> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rismoving:hunter> <gismoving> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rdamage:hunter,5> <gdamage:5> x").isEmpty());
        assertTrue(CommandSyntax.error("say <puuid:Steve> <ruuid:hunter> <guuid> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rslot:hunter,helmet> <gslot:helmet> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rclearslot:hunter,helmet> <gclearslot:helmet> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rfx.set:hunter,glow,speed,20> <gfx.set:glow,speed,20> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rfx.get:hunter,glow> <gfx.get:glow> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rfx.clear:hunter> <gfx.clear> <gfx.clear:glow> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rtitle:hunter,title,Hello> <gtitle:title,Hello> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rmaxhp.set:hunter,glow,8> <gmaxhp.set:glow,8> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rmaxhp.get:hunter,glow> <gmaxhp.get:glow> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rmaxhp.clear:hunter> <gmaxhp.clear> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rgive:hunter,[STONE,1]> <ggive:[STONE,1]> x").isEmpty());
        assertTrue(CommandSyntax.error("say <ritemtag:hunter,helmet> <gitemtag:helmet> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rhasitem:hunter,stone> <ghasitem:stone> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rcountitem:hunter,stone> <gcountitem:stone> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rheld:hunter> <gheld> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rlocation:hunter> <glocation> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rworld:hunter> <gworld> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rx:hunter> <gx> <ry:hunter> <gy> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rz:hunter> <gz> <ryaw:hunter> <gyaw> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rpitch:hunter> <gpitch> x").isEmpty());
        assertTrue(CommandSyntax.error("say <pdirx:Steve> <rdirx:hunter> <gdirx> x").isEmpty());
        assertTrue(CommandSyntax.error("say <pdiry:Steve> <rdiry:hunter> <gdiry> x").isEmpty());
        assertTrue(CommandSyntax.error("say <pdirz:Steve> <rdirz:hunter> <gdirz> x").isEmpty());
        assertTrue(CommandSyntax.error("say <reliminated:hunter> <geliminated> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rdir:hunter> <gdir> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rlos:hunter,Alex> <glos:Alex> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rhaseffect:hunter,speed> <ghaseffect:speed> x").isEmpty());
        assertTrue(CommandSyntax.error("say <rswitch:hunter,SPEEDRUNNER> <gswitch:SPEEDRUNNER> x").isEmpty());
        assertTrue(CommandSyntax.error("say <gfx.set:glow> x").isEmpty());
    }

    @Test
    void twinReadsFail() {
        assertTrue(CommandSyntax.error("say <gheld:Steve> x").isPresent());
        assertTrue(CommandSyntax.error("say <rheld> x").isPresent());
        assertTrue(CommandSyntax.error("say <gstate> x").isPresent());
        assertTrue(CommandSyntax.error("say <rstate:hunter> x").isPresent());
        assertTrue(CommandSyntax.error("say <gdamage> x").isPresent());
        assertTrue(CommandSyntax.error("say <rdamage:hunter> x").isPresent());
        assertTrue(CommandSyntax.error("say <rcooldown:hunter,key> x").isPresent());
        assertTrue(CommandSyntax.error("say <ggive> x").isPresent());
        assertTrue(CommandSyntax.error("say <rgive:hunter> x").isPresent());
        assertTrue(CommandSyntax.error("say <ghasitem> x").isPresent());
        assertTrue(CommandSyntax.error("say <rhasitem:hunter> x").isPresent());
        assertTrue(CommandSyntax.error("say <gcountitem> x").isPresent());
        assertTrue(CommandSyntax.error("say <rslot:hunter> x").isPresent());
        assertTrue(CommandSyntax.error("say <gslot> x").isPresent());
        assertTrue(CommandSyntax.error("say <gfx.set> x").isPresent());
        assertTrue(CommandSyntax.error("say <gfx.clear:a,b> x").isPresent());
        assertTrue(CommandSyntax.error("say <gtitle:title> x").isPresent());
        assertTrue(CommandSyntax.error("say <gmaxhp.set:glow> x").isPresent());
        assertTrue(CommandSyntax.error("say <gmaxhp.get> x").isPresent());
        assertTrue(CommandSyntax.error("say <gdir:Steve> x").isPresent());
        assertTrue(CommandSyntax.error("say <rdir> x").isPresent());
        assertTrue(CommandSyntax.error("say <rlos:hunter> x").isPresent());
        assertTrue(CommandSyntax.error("say <glos> x").isPresent());
        assertTrue(CommandSyntax.error("say <ghaseffect> x").isPresent());
        assertTrue(CommandSyntax.error("say <rhaseffect:hunter> x").isPresent());
        assertTrue(CommandSyntax.error("say <rswitch:hunter> x").isPresent());
        assertTrue(CommandSyntax.error("say <gswitch> x").isPresent());
        assertTrue(CommandSyntax.error("say <rswitch:hunter,ref> x").isPresent());
        assertTrue(CommandSyntax.error("say <gswitch:ref> x").isPresent());
    }
}
