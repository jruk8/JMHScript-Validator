package com.jruk8.jmhvalidator.core.share;

import com.jruk8.jmhvalidator.core.ports.ShareMaterials;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Share-string codec: exact bytes out, strict schema back in.
 * Ported 1:1 from the engine suite; the builders speak the core
 * model instead of the Okaeri entries.
 */
class ShareCodecTest {
    /** One backslash, so expected unicode escapes stay readable. */
    private static final String BS = "\\";
    private static final ShareMaterials STRICT =
            ShareMaterials.of(Set.of("STONE", "TNT", "CHEST"), "STONE");

    @Test
    void tinyModifierEncodesWithExactJson() throws Exception {
        ShareModel.Modifier modifier = new ShareModel.Modifier(
                new ShareModel.Meta("Beef", null, null, null), null, null, null);

        String payload = ShareCodec.exportModifier("beef", modifier);

        assertEquals("{\"type\":\"modifier\",\"id\":\"beef\",\"data\":{\"enabled\":false,"
                + "\"meta\":{\"name\":\"Beef\"}}}",
                framedJson(payload));
        assertChecksumMatches(payload);
    }

    @Test
    void exclusiveGroupRoundTrips() throws Exception {
        ShareModel.Modifier modifier = new ShareModel.Modifier(
                new ShareModel.Meta("Bingo", null, null, null),
                null, Map.of("exclusive-group", "bingo"), null);

        String payload = ShareCodec.exportModifier("bingo", modifier);
        Optional<ShareCodec.Imported> decoded = ShareCodec.decode(payload, STRICT);

        assertTrue(framedJson(payload).contains("\"exclusive-group\":\"bingo\""));
        assertTrue(decoded.isPresent());
        assertEquals("bingo", decoded.get().modifier().advanced().get("exclusive-group"));
    }

    @Test
    void exportForcesEnabledFalse() throws Exception {
        String payload = ShareCodec.exportModifier("loud", namedModifier("Loud"));

        assertTrue(framedJson(payload).contains("\"enabled\":false"));
    }

    @Test
    void rawEnvelopeDecodes() {
        String json = "{\"type\":\"preset\",\"id\":\"pack\",\"data\":{\"meta\":{\"name\":\"Pack\","
                + "\"item\":\"CHEST\"},\"modifiers\":[\"a\"]}}";

        Optional<ShareCodec.Imported> decoded = ShareCodec.decode(envelope(json), STRICT);

        assertTrue(decoded.isPresent());
        assertEquals(ShareCodec.Kind.PRESET, decoded.get().kind());
        assertEquals("pack", decoded.get().id());
        assertEquals("Pack", decoded.get().preset().meta().name());
        assertNull(decoded.get().preset().meta().visibility());
    }

    @Test
    void fullModifierRoundTripsButLandsDisabled() throws Exception {
        ShareModel.Modifier modifier = fullModifier();

        String payload = ShareCodec.exportModifier("gear-dice", modifier);
        Optional<ShareCodec.Imported> decoded = ShareCodec.decode(payload, STRICT);

        assertTrue(decoded.isPresent());
        assertEquals(ShareCodec.Kind.MODIFIER, decoded.get().kind());
        assertEquals("gear-dice", decoded.get().id());
        ShareModel.Modifier back = decoded.get().modifier();
        assertEquals("Gear Dice", back.meta().name());
        assertEquals("Roll for gear", back.meta().description());
        assertEquals("TNT", back.meta().visibility().icon());
        assertEquals("jruk", back.meta().author());
        assertEquals(List.of("INTERVAL", "ON_START"), back.behaviors().get("0").runsOn());
        assertEquals("AFTER", back.behaviors().get("0").preStartOrder());
        assertEquals(15.0, back.behaviors().get("0").options().interval().interval());
        assertEquals(5.0, back.behaviors().get("0").options().interval().deviation());
        assertEquals("PER_EXECUTOR",
                back.behaviors().get("0").options().interval().behavior());
        assertEquals(0.5, back.behaviors().get("0").options().chance().chance());
        assertEquals("PICK_RANDOM", back.behaviors().get("0").options().execution().selection());
        assertEquals(2, back.behaviors().get("0").options().execution().pickRandom().count());
        assertEquals(100L, back.behaviors().get("0").options().delay());
        assertEquals(List.of("give <p> beef 8", "say hi"),
                back.behaviors().get("0").commands().get("player"));
        assertEquals(List.of("say custom"),
                back.behaviors().get("0").commands().get("custom-list"));
        assertEquals(payload, ShareCodec.exportModifier(decoded.get().id(), back));
    }

    @Test
    void tagBracketsEscapeLikeGson() throws Exception {
        String payload = ShareCodec.exportModifier("gear-dice", fullModifier());

        assertTrue(framedJson(payload)
                .contains("give " + BS + "u003cp" + BS + "u003e beef 8"));
    }

    @Test
    void presetRoundTripsExactly() {
        ShareModel.Preset preset = new ShareModel.Preset(
                new ShareModel.Meta("Chaos", "Everything on", "JManhunt",
                        new ShareModel.Visibility("TNT", "ffa000", true)),
                List.of("gear-dice", "beef"), null);

        String payload = ShareCodec.exportPreset("chaos", preset);
        Optional<ShareCodec.Imported> decoded = ShareCodec.decode(payload, STRICT);

        assertTrue(decoded.isPresent());
        assertEquals(ShareCodec.Kind.PRESET, decoded.get().kind());
        assertEquals("chaos", decoded.get().id());
        assertEquals("Chaos", decoded.get().preset().meta().name());
        assertEquals("JManhunt", decoded.get().preset().meta().author());
        assertEquals("TNT", decoded.get().preset().meta().visibility().icon());
        assertEquals("ffa000", decoded.get().preset().meta().visibility().color());
        assertEquals(true, decoded.get().preset().meta().visibility().broadcast());
        assertEquals(List.of("gear-dice", "beef"), decoded.get().preset().modifiers());
        assertEquals(payload, ShareCodec.exportPreset(decoded.get().id(), decoded.get().preset()));
    }

    @Test
    void nestedPresetRoundTripsWithRefs() {
        ShareModel.Preset preset = new ShareModel.Preset(
                new ShareModel.Meta("Outer", "Nests inner", "JManhunt", null),
                List.of("beef"), List.of("inner"));

        String payload = ShareCodec.exportPreset("outer", preset);
        Optional<ShareCodec.Imported> decoded = ShareCodec.decode(payload, STRICT);

        assertTrue(decoded.isPresent());
        assertEquals(List.of("beef"), decoded.get().preset().modifiers());
        assertEquals(List.of("inner"), decoded.get().preset().presets());
        assertEquals(payload, ShareCodec.exportPreset(decoded.get().id(), decoded.get().preset()));
    }

    @Test
    void presetWithoutRefsDecodesToNull() {
        String json = "{\"type\":\"preset\",\"id\":\"pack\",\"data\":{\"meta\":{\"name\":\"Pack\","
                + "\"item\":\"CHEST\"},\"modifiers\":[\"a\"]}}";

        Optional<ShareCodec.Imported> decoded = ShareCodec.decode(envelope(json), STRICT);

        assertTrue(decoded.isPresent());
        assertNull(decoded.get().preset().presets());
    }

    @Test
    void presetWithBlankRefFailsDecode() {
        String json = "{\"type\":\"preset\",\"id\":\"pack\",\"data\":{\"meta\":{\"name\":\"Pack\"},"
                + "\"presets\":[\" \"]}}";

        assertTrue(ShareCodec.decode(envelope(json), STRICT).isEmpty());
    }

    @Test
    void thinModifierImportsWithDefaults() {
        String payload = ShareCodec.exportModifier("thin",
                new ShareModel.Modifier(null, null, null, null));

        Optional<ShareCodec.Imported> decoded = ShareCodec.decode(payload, STRICT);

        assertTrue(decoded.isPresent());
        assertEquals(ShareCodec.DEFAULT_MODIFIER_NAME, decoded.get().modifier().meta().name());
    }

    @Test
    void largePayloadUsesDeflate() {
        Map<String, List<String>> commands = new LinkedHashMap<>();
        commands.put("player", List.of("say " + "x".repeat(2000), "say " + "y".repeat(2000)));
        ShareModel.Behavior behavior = new ShareModel.Behavior(null, null, null, commands);
        ShareModel.Modifier modifier = new ShareModel.Modifier(
                new ShareModel.Meta("Big", null, null, null),
                null, null, Map.of("0", behavior));

        String payload = ShareCodec.exportModifier("big", modifier);

        assertTrue(payload.startsWith(ShareCodec.TAG_DEFLATED));
        assertTrue(ShareCodec.decode(payload, STRICT).isPresent());
    }

    @Test
    void corruptedPayloadsDecodeEmpty() {
        String payload = ShareCodec.exportModifier("beef", namedModifier("Beef"));

        assertTrue(ShareCodec.decode(null, STRICT).isEmpty());
        assertTrue(ShareCodec.decode("", STRICT).isEmpty());
        assertTrue(ShareCodec.decode("JMH1X:" + payload.substring(6), STRICT).isEmpty());
        assertTrue(ShareCodec.decode(payload.substring(0, payload.length() - 4), STRICT).isEmpty());
        assertTrue(ShareCodec.decode("JMH1R:!!!not-base64!!!", STRICT).isEmpty());
        assertTrue(ShareCodec.decode(flip(payload, 6), STRICT).isEmpty());
        assertTrue(ShareCodec.decode(flip(payload, 10), STRICT).isEmpty());
        assertTrue(ShareCodec.decode(flip(payload, payload.length() - 5), STRICT).isEmpty());
    }

    @Test
    void oversizedPayloadDecodesEmpty() {
        Map<String, List<String>> commands = new LinkedHashMap<>();
        commands.put("player", List.of("say " + "z".repeat(1_200_000)));
        ShareModel.Behavior behavior = new ShareModel.Behavior(null, null, null, commands);
        ShareModel.Modifier modifier = new ShareModel.Modifier(
                new ShareModel.Meta("Huge", null, null, null),
                null, null, Map.of("0", behavior));

        String payload = ShareCodec.exportModifier("huge", modifier);

        assertTrue(payload.startsWith(ShareCodec.TAG_DEFLATED));
        assertTrue(ShareCodec.decode(payload, STRICT).isEmpty());
    }

    @Test
    void offSchemaPayloadsDecodeEmpty() {
        assertTrue(ShareCodec.decode(envelope("{\"type\":\"mod\",\"id\":\"a\",\"data\":{}}"), STRICT)
                .isEmpty());
        assertTrue(ShareCodec.decode(
                envelope("{\"type\":\"modifier\",\"id\":\"has space\",\"data\":{}}"), STRICT).isEmpty());
        assertTrue(ShareCodec.decode(envelope("{\"type\":\"modifier\",\"id\":\"a\"}"), STRICT).isEmpty());
        assertTrue(ShareCodec.decode(envelope("not json"), STRICT).isEmpty());
        assertTrue(ShareCodec.decode(envelope("[1,2]"), STRICT).isEmpty());

        String noMeta = "{\"type\":\"modifier\",\"id\":\"a\",\"data\":{\"enabled\":false}}";
        assertTrue(ShareCodec.decode(envelope(noMeta), STRICT).isEmpty());

        String blankName = "{\"type\":\"modifier\",\"id\":\"a\","
                + "\"data\":{\"meta\":{\"name\":\"  \",\"icon\":\"STONE\"}}}";
        assertTrue(ShareCodec.decode(envelope(blankName), STRICT).isEmpty());

        String badVisibilityItem = "{\"type\":\"modifier\",\"id\":\"a\","
                + "\"data\":{\"meta\":{\"name\":\"A\","
                + "\"visibility\":{\"icon\":\"NOPE\"}}}}";
        assertTrue(ShareCodec.decode(envelope(badVisibilityItem), STRICT).isEmpty());

        String badTrigger = "{\"type\":\"modifier\",\"id\":\"a\",\"data\":{\"meta\":{\"name\":\"A\","
                + "\"icon\":\"STONE\"},\"behavior\":{\"0\":{\"runs-on\":[\"nope\"]}}}}";
        assertTrue(ShareCodec.decode(envelope(badTrigger), STRICT).isEmpty());

        String badChance = "{\"type\":\"modifier\",\"id\":\"a\",\"data\":{\"meta\":{\"name\":\"A\","
                + "\"icon\":\"STONE\"},\"behavior\":{\"0\":{\"options\":{\"success-chance\":{\"chance\":2.0}}}}}}";
        assertTrue(ShareCodec.decode(envelope(badChance), STRICT).isEmpty());

        String badSelection = "{\"type\":\"modifier\",\"id\":\"a\",\"data\":{\"meta\":{\"name\":\"A\","
                + "\"icon\":\"STONE\"},\"behavior\":{\"0\":{\"options\":{\"execution\":{\"selection\":\"MAYBE\"}}}}}}";
        assertTrue(ShareCodec.decode(envelope(badSelection), STRICT).isEmpty());

        String badCount = "{\"type\":\"modifier\",\"id\":\"a\",\"data\":{\"meta\":{\"name\":\"A\","
                + "\"icon\":\"STONE\"},\"behavior\":{\"options\":{\"execution\":"
                + "{\"pick-random\":{\"count\":0}}}}}";
        assertTrue(ShareCodec.decode(envelope(badCount), STRICT).isEmpty());

        String badDelay = "{\"type\":\"modifier\",\"id\":\"a\",\"data\":{\"meta\":{\"name\":\"A\","
                + "\"icon\":\"STONE\"},\"behavior\":{\"options\":{\"delay\":-5}}}";
        assertTrue(ShareCodec.decode(envelope(badDelay), STRICT).isEmpty());

        String badPreset = "{\"type\":\"preset\",\"id\":\"a\",\"data\":{\"name\":\"  \"}}";
        assertTrue(ShareCodec.decode(envelope(badPreset), STRICT).isEmpty());
    }

    @Test
    void mistypedEnabledDecodesEmpty() {
        String badEnabled = "{\"type\":\"modifier\",\"id\":\"a\","
                + "\"data\":{\"enabled\":\"yes\",\"meta\":{\"name\":\"A\",\"icon\":\"STONE\"}}}";

        assertTrue(ShareCodec.decode(envelope(badEnabled), STRICT).isEmpty());
    }

    @Test
    void oversizedRawPayloadDecodesEmpty() {
        String json = "{\"type\":\"modifier\",\"id\":\"a\",\"data\":{\"meta\":{\"name\":\"A\","
                + "\"icon\":\"STONE\",\"description\":\"" + "q".repeat(1_200_000) + "\"}}}";

        assertTrue(ShareCodec.decode(envelope(json), STRICT).isEmpty());
    }

    @Test
    void decodeTrimsSurroundingWhitespace() {
        String payload = ShareCodec.exportModifier("beef", namedModifier("Beef"));

        assertTrue(ShareCodec.decode("  " + payload + "\n", STRICT).isPresent());
    }

    @Test
    void lenientMaterialsAcceptUnknownIcons() {
        String json = "{\"type\":\"modifier\",\"id\":\"a\","
                + "\"data\":{\"meta\":{\"name\":\"A\","
                + "\"visibility\":{\"icon\":\"NOPE\"}}}}";

        Optional<ShareCodec.Imported> decoded =
                ShareCodec.decode(envelope(json), ShareMaterials.lenient());

        assertTrue(decoded.isPresent());
        assertEquals("NOPE", decoded.get().modifier().meta().visibility().icon());
    }

    @Test
    void staleTopLevelIconDecodesIgnored() {
        String json = "{\"type\":\"modifier\",\"id\":\"a\","
                + "\"data\":{\"meta\":{\"name\":\"A\",\"icon\":\"NOPE\"}}}";

        Optional<ShareCodec.Imported> decoded = ShareCodec.decode(envelope(json), STRICT);

        assertTrue(decoded.isPresent());
        assertNull(decoded.get().modifier().meta().visibility());
    }

    @Test
    void deepNestingDecodesEmpty() {
        String json = "{\"type\":\"modifier\",\"id\":\"a\",\"data\":{\"meta\":{\"name\":\"A\","
                + "\"icon\":\"STONE\"},\"behavior\":{\"0\":{\"options\":{\"execution\":"
                + "{\"selection\":\"IN_ORDER\",\"deep\":[".repeat(1) + "[".repeat(100) + "]".repeat(101)
                + "}}}}}}";

        assertTrue(ShareCodec.decode(envelope(json), STRICT).isEmpty());
    }

    private static ShareModel.Modifier namedModifier(String name) {
        return new ShareModel.Modifier(
                new ShareModel.Meta(name, null, null, null), null, null, null);
    }

    private static ShareModel.Modifier fullModifier() {
        ShareModel.Meta meta = new ShareModel.Meta("Gear Dice", "Roll for gear", "jruk",
                new ShareModel.Visibility("TNT", null, null));
        ShareModel.Interval interval = new ShareModel.Interval(15.0, 5.0, "PER_EXECUTOR");
        ShareModel.Chance chance = new ShareModel.Chance(0.5, null);
        ShareModel.Execution execution =
                new ShareModel.Execution("PICK_RANDOM", new ShareModel.PickRandom(2, null));
        ShareModel.Options options = new ShareModel.Options(interval, chance, execution, 100L);
        Map<String, List<String>> commands = new LinkedHashMap<>();
        commands.put("player", List.of("give <p> beef 8", "say hi"));
        commands.put("custom-list", List.of("say custom"));
        ShareModel.Behavior behavior =
                new ShareModel.Behavior(List.of("INTERVAL", "ON_START"), "AFTER", options, commands);
        return new ShareModel.Modifier(meta, null, null, Map.of("0", behavior));
    }

    private static String flip(String payload, int index) {
        char replacement = payload.charAt(index) == 'A' ? 'B' : 'A';
        return payload.substring(0, index) + replacement + payload.substring(index + 1);
    }

    /** Wraps hand-built JSON in a raw envelope for negative tests. */
    private static String envelope(String json) {
        byte[] raw = json.getBytes(StandardCharsets.UTF_8);
        CRC32 checksum = new CRC32();
        checksum.update(raw);
        int value = (int) checksum.getValue();
        byte[] framed = new byte[4 + raw.length];
        framed[0] = (byte) (value >>> 24);
        framed[1] = (byte) (value >>> 16);
        framed[2] = (byte) (value >>> 8);
        framed[3] = (byte) value;
        System.arraycopy(raw, 0, framed, 4, raw.length);
        return ShareCodec.TAG_RAW
                + Base64.getUrlEncoder().withoutPadding().encodeToString(framed);
    }

    private static String framedJson(String payload) throws Exception {
        return new String(unframed(payload), StandardCharsets.UTF_8);
    }

    private static void assertChecksumMatches(String payload) throws Exception {
        String tag = payload.startsWith(ShareCodec.TAG_DEFLATED)
                ? ShareCodec.TAG_DEFLATED : ShareCodec.TAG_RAW;
        byte[] framed = Base64.getUrlDecoder().decode(payload.substring(tag.length()));
        int expected = ((framed[0] & 0xFF) << 24) | ((framed[1] & 0xFF) << 16)
                | ((framed[2] & 0xFF) << 8) | (framed[3] & 0xFF);
        CRC32 checksum = new CRC32();
        checksum.update(unframed(payload));
        assertEquals(expected, (int) checksum.getValue());
    }

    private static byte[] unframed(String payload) throws Exception {
        boolean compressed = payload.startsWith(ShareCodec.TAG_DEFLATED);
        String tag = compressed ? ShareCodec.TAG_DEFLATED : ShareCodec.TAG_RAW;
        assertTrue(payload.startsWith(tag));
        byte[] framed = Base64.getUrlDecoder().decode(payload.substring(tag.length()));
        byte[] body = new byte[framed.length - 4];
        System.arraycopy(framed, 4, body, 0, body.length);
        if (!compressed) {
            return body;
        }
        java.util.zip.Inflater inflater = new java.util.zip.Inflater(true);
        try {
            inflater.setInput(body);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0) {
                    break;
                }
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        } finally {
            inflater.end();
        }
    }

    @Test
    void validIdMatchesCodecShape() {
        assertTrue(ShareCodec.validId("good-id_1"));
        assertTrue(ShareCodec.validId("a"));
        assertTrue(ShareCodec.validId("a".repeat(64)));
        assertFalse(ShareCodec.validId("-lead"));
        assertFalse(ShareCodec.validId("has space"));
        assertFalse(ShareCodec.validId("sub/dup"));
        assertFalse(ShareCodec.validId("sub\\dup"));
        assertFalse(ShareCodec.validId("has.dot"));
        assertFalse(ShareCodec.validId(".leading"));
        assertFalse(ShareCodec.validId(".."));
        assertFalse(ShareCodec.validId(""));
        assertFalse(ShareCodec.validId("   "));
        assertFalse(ShareCodec.validId("a".repeat(65)));
        assertFalse(ShareCodec.validId(null));
    }
}
