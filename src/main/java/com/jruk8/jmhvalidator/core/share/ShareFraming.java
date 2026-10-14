package com.jruk8.jmhvalidator.core.share;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Share-string framing: minified JSON, a 4-byte CRC32 of the
 * JSON, raw Deflate when it shrinks the payload, and unpadded
 * URL-safe Base64 under a versioned tag. Decoding enforces a
 * 1 MB inflation cap; anything corrupt decodes to empty.
 */
public final class ShareFraming {

    /** Tag for deflated payloads. */
    public static final String TAG_DEFLATED = "JMH1D:";
    /** Tag for raw payloads. */
    public static final String TAG_RAW = "JMH1R:";
    /** Hard ceiling on decompressed JSON bytes. */
    public static final int MAX_JSON_BYTES = 1024 * 1024;

    private ShareFraming() {
    }

    /** Frames minified JSON under the versioned tag. */
    public static String encode(String json) {
        byte[] raw = json.getBytes(StandardCharsets.UTF_8);
        CRC32 checksum = new CRC32();
        checksum.update(raw);
        byte[] deflated = deflate(raw);
        boolean compressed = deflated.length < raw.length;
        byte[] body = compressed ? deflated : raw;
        byte[] framed = new byte[4 + body.length];
        int value = (int) checksum.getValue();
        framed[0] = (byte) (value >>> 24);
        framed[1] = (byte) (value >>> 16);
        framed[2] = (byte) (value >>> 8);
        framed[3] = (byte) value;
        System.arraycopy(body, 0, framed, 4, body.length);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(framed);
        return (compressed ? TAG_DEFLATED : TAG_RAW) + encoded;
    }

    /**
     * Unframes one payload, enforcing the tag, the checksum, and
     * the size cap. Empty when anything is corrupt.
     */
    public static Optional<String> decode(String payload) {
        boolean compressed;
        if (payload.startsWith(TAG_DEFLATED)) {
            compressed = true;
        } else if (payload.startsWith(TAG_RAW)) {
            compressed = false;
        } else {
            return Optional.empty();
        }
        byte[] framed;
        try {
            framed = Base64.getUrlDecoder().decode(payload.substring(TAG_DEFLATED.length()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        if (framed.length < 4) {
            return Optional.empty();
        }
        int expected = ((framed[0] & 0xFF) << 24) | ((framed[1] & 0xFF) << 16)
                | ((framed[2] & 0xFF) << 8) | (framed[3] & 0xFF);
        byte[] body = Arrays.copyOfRange(framed, 4, framed.length);
        byte[] json;
        try {
            json = compressed ? inflateCapped(body) : body;
        } catch (DataFormatException exception) {
            return Optional.empty();
        }
        if (json.length > MAX_JSON_BYTES) {
            return Optional.empty();
        }
        CRC32 checksum = new CRC32();
        checksum.update(json);
        if ((int) checksum.getValue() != expected) {
            return Optional.empty();
        }
        return Optional.of(new String(json, StandardCharsets.UTF_8));
    }

    private static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        try {
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length);
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                out.write(buffer, 0, deflater.deflate(buffer));
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /**
     * Inflates with the size cap enforced during inflation, so a
     * decompression bomb aborts before its full output exists.
     */
    private static byte[] inflateCapped(byte[] body) throws DataFormatException {
        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(body);
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(body.length * 2, 8192));
            byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0) {
                    if (inflater.needsInput()) {
                        break;
                    }
                    throw new DataFormatException("Invalid deflated payload");
                }
                out.write(buffer, 0, count);
                if (out.size() > MAX_JSON_BYTES) {
                    throw new DataFormatException("Payload exceeds size cap");
                }
            }
            return out.toByteArray();
        } finally {
            inflater.end();
        }
    }
}
