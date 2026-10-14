package com.jruk8.jmhvalidator.core;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Hex color validation for modifier broadcast colors. Accepts
 * {@code #ffffff}, {@code ffffff}, {@code 0xffffff}, and short
 * {@code fff}; normalizes to lowercase six digits without prefix.
 * Pure, so unit tests exercise every shape.
 */
public final class HexColors {

    /** Brand fallback when a modifier defines no color. */
    public static final String BRAND = "de7766";

    private static final Pattern HEX = Pattern.compile("[0-9a-f]{6}");

    private HexColors() {
    }

    /**
     * Normalizes one color input, or empty when it is not a hex
     * color. Null and blank read as empty.
     */
    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String text = raw.trim().toLowerCase(Locale.ROOT);
        if (text.startsWith("#")) {
            text = text.substring(1);
        } else if (text.startsWith("0x")) {
            text = text.substring(2);
        }
        if (text.length() == 3 && text.chars().allMatch(HexColors::hexDigit)) {
            StringBuilder expanded = new StringBuilder(6);
            for (char digit : text.toCharArray()) {
                expanded.append(digit).append(digit);
            }
            text = expanded.toString();
        }
        if (text.length() != 6 || !HEX.matcher(text).matches()) {
            return Optional.empty();
        }
        return Optional.of(text);
    }

    /** True when the input is a valid hex color in any accepted shape. */
    public static boolean valid(String raw) {
        return normalize(raw).isPresent();
    }

    /** Normalized color, or the brand fallback when invalid. */
    public static String orBrand(String raw) {
        return normalize(raw).orElse(BRAND);
    }

    private static boolean hexDigit(int code) {
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f');
    }

}
