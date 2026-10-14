package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.ports.KnownMaterials;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Type-aware setting values: zero defaults, default clamping
 * into allowed ranges, and coercion of stored values. Moved
 * 1:1 from the engine inputs class; the engine delegates
 * here, so load repair and out-of-process validation share
 * one implementation. A null materials port (a registry
 * without the materials array) leaves material names
 * unfiltered instead of guessing. Caps sizes so hostile
 * input cannot allocate wildly. Pure.
 */
public final class SettingCoerce {

    /** Hard cap on list entries from any source. */
    public static final int MAX_LIST = 64;

    /** Hard cap on free-text setting input. */
    public static final int MAX_TEXT = 256;

    private SettingCoerce() {
    }

    /** Zero value for one type, used when no default exists. */
    public static Object zero(SettingType type) {
        return switch (type) {
            case BOOLEAN -> Boolean.FALSE;
            case INTEGER -> 0L;
            case DOUBLE -> 0.0;
            case STRING, ENUM, MATERIAL, COLOR -> "";
            case LIST, EFFECTS, ITEMS, WEIGHTED_ITEM -> new ArrayList<>();
            case MESSAGE -> List.of(false, "");
            case SOUND -> List.of(false, "", 1.0, 1.0);
        };
    }

    /** Clamps one default into allowed, or the zero value when null. */
    public static Object clampDefault(SettingType type, Object def, Object allowed,
            KnownMaterials known) {
        if (def == null) {
            return zero(type);
        }
        Object coerced = coerce(type, def, allowed, known);
        return coerced == null ? zero(type) : coerced;
    }

    /**
     * Coerces a stored value to its type, clamping into allowed.
     * Returns the repaired value, or null when it cannot be saved.
     */
    public static Object coerce(SettingType type, Object value, Object allowed,
            KnownMaterials known) {
        if (value == null) {
            return null;
        }
        return switch (type) {
            case BOOLEAN -> coerceBoolean(value);
            case INTEGER -> clampLong(parseLong(value), allowed);
            case DOUBLE -> clampDouble(parseDouble(value), allowed);
            case STRING -> coerceString(value, allowed);
            case ENUM -> coerceEnum(value, allowed);
            case MATERIAL -> coerceMaterial(value, known);
            case COLOR -> coerceColor(value);
            case LIST -> coerceList(value, allowed);
            case EFFECTS -> coerceTriples(value, 3, false, known);
            case ITEMS -> coerceTriples(value, 3, true, known);
            case WEIGHTED_ITEM -> coerceWeighted(value);
            case MESSAGE -> coerceMessage(value);
            case SOUND -> coerceSound(value);
        };
    }

    /**
     * True when two raw values read equal: numbers compare by
     * double value, lists compare element-wise, anything else
     * compares by equals. Decides whether a clamp changed a
     * default or value.
     */
    public static boolean sameRaw(Object first, Object second) {
        if (first == null) {
            return second == null;
        }
        if (first instanceof Number firstNumber && second instanceof Number secondNumber) {
            return firstNumber.doubleValue() == secondNumber.doubleValue();
        }
        if (first instanceof List<?> firstList && second instanceof List<?> secondList) {
            if (firstList.size() != secondList.size()) {
                return false;
            }
            for (int index = 0; index < firstList.size(); index++) {
                if (!sameRaw(firstList.get(index), secondList.get(index))) {
                    return false;
                }
            }
            return true;
        }
        return first.equals(second);
    }

    private static Boolean coerceBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return text.equals("true") ? Boolean.TRUE : text.equals("false") ? Boolean.FALSE : null;
    }

    private static Long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    /**
     * Parses one finite double from a number or string.
     * Public so engine text input shares the one parser.
     */
    public static Double parseDouble(Object value) {
        if (value instanceof Number number) {
            double parsed = number.doubleValue();
            return Double.isFinite(parsed) ? parsed : null;
        }
        try {
            double parsed = Double.parseDouble(String.valueOf(value).trim());
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    private static Long clampLong(Long value, Object allowed) {
        if (value == null) {
            return null;
        }
        double[] range = numberRange(allowed);
        double clamped = Math.min(Math.max(value, range[0]), range[1]);
        return (long) clamped;
    }

    private static Double clampDouble(Double value, Object allowed) {
        if (value == null) {
            return null;
        }
        double[] range = numberRange(allowed);
        return Math.min(Math.max(value, range[0]), range[1]);
    }

    /**
     * Allowed range for numbers: a {min, max} map, open-ended by default.
     * Public so GUI editors share the one bounds source with validation.
     */
    public static double[] numberRange(Object allowed) {
        double min = Double.NEGATIVE_INFINITY;
        double max = Double.POSITIVE_INFINITY;
        if (allowed instanceof Map<?, ?> map) {
            Double low = parseDouble(map.get("min"));
            Double high = parseDouble(map.get("max"));
            if (low != null) {
                min = low;
            }
            if (high != null) {
                max = high;
            }
        }
        return new double[]{min, max};
    }

    /**
     * Coerces free text, capping length. Public so engine text
     * input shares the one text source.
     */
    public static String coerceString(Object value, Object allowed) {
        String text = String.valueOf(value);
        if (text.length() > MAX_TEXT) {
            text = text.substring(0, MAX_TEXT);
        }
        if (allowed instanceof Map<?, ?> map) {
            Double limit = parseDouble(map.get("maxLength"));
            if (limit != null && text.length() > limit) {
                text = text.substring(0, Math.max(0, limit.intValue()));
            }
        }
        return text;
    }

    private static String coerceEnum(Object value, Object allowed) {
        List<String> choices = stringChoices(allowed);
        String text = String.valueOf(value).trim();
        if (choices.isEmpty()) {
            return text;
        }
        for (String choice : choices) {
            if (choice.equalsIgnoreCase(text)) {
                return choice;
            }
        }
        return choices.get(0);
    }

    private static String coerceMaterial(Object value, KnownMaterials known) {
        String text = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        if (known == null) {
            return text;
        }
        return known.isKnown(KnownMaterials.normalize(text)) ? text : null;
    }

    private static String coerceColor(Object value) {
        return HexColors.normalize(String.valueOf(value).trim()).orElse(null);
    }

    /**
     * String choices from an allowed list, capped. Public so
     * engine text input shares the one choice source.
     */
    public static List<String> stringChoices(Object allowed) {
        List<String> choices = new ArrayList<>();
        if (allowed instanceof List<?> list) {
            for (Object item : list) {
                if (choices.size() >= MAX_LIST) {
                    break;
                }
                choices.add(String.valueOf(item));
            }
        }
        return choices;
    }

    private static List<Object> coerceList(Object value, Object allowed) {
        List<String> choices = stringChoices(allowed);
        List<Object> kept = new ArrayList<>();
        for (Object item : asList(value)) {
            if (kept.size() >= MAX_LIST) {
                break;
            }
            if (item instanceof List<?> nested && choices.isEmpty()) {
                kept.add(nested);
                continue;
            }
            String text = String.valueOf(item);
            if (!choices.isEmpty() && choices.stream().noneMatch(choice -> choice.equalsIgnoreCase(text))) {
                continue;
            }
            kept.add(text);
        }
        return kept;
    }

    /**
     * Coerces triple lists; materials checked when requested.
     * Public so panels share the one triple source with validation.
     */
    public static List<Object> coerceTriples(Object value, int width, boolean materials,
            KnownMaterials known) {
        List<Object> kept = new ArrayList<>();
        for (Object item : asList(value)) {
            if (kept.size() >= MAX_LIST) {
                break;
            }
            List<?> parts = asList(item);
            if (parts.size() != width) {
                continue;
            }
            List<Object> triple = coerceTriple(parts, materials, known);
            if (triple != null) {
                kept.add(triple);
            }
        }
        return kept;
    }

    /**
     * Coerces one triple: a non-blank head (material-checked
     * when requested) plus two whole numbers. Public so
     * engine text input shares the one triple source.
     */
    public static List<Object> coerceTriple(List<?> parts, boolean materials,
            KnownMaterials known) {
        String head = String.valueOf(parts.get(0)).trim();
        if (head.isEmpty()) {
            return null;
        }
        if (materials && known != null
                && !known.isKnown(KnownMaterials.normalize(head.toUpperCase(Locale.ROOT)))) {
            return null;
        }
        Long middle = parseLong(parts.get(1));
        Long last = parseLong(parts.get(2));
        if (middle == null || last == null) {
            return null;
        }
        return List.of(head, middle, last);
    }

    /**
     * Coerces weighted-item pairs: a finite weight plus any
     * non-blank item string (payloads may nest, like an item
     * triple). Public so panels share the one pair source with
     * validation. Non-positive weights survive: draws ignore them,
     * so only garbage entries drop.
     */
    public static List<Object> coerceWeighted(Object value) {
        List<Object> kept = new ArrayList<>();
        for (Object item : asList(value)) {
            if (kept.size() >= MAX_LIST) {
                break;
            }
            List<?> parts = asList(item);
            if (parts.size() != 2) {
                continue;
            }
            List<Object> pair = coerceWeightedPair(parts);
            if (pair != null) {
                kept.add(pair);
            }
        }
        return kept;
    }

    /**
     * Coerces one weighted pair: a finite weight plus a
     * non-blank item. Public so engine text input shares the
     * one pair source.
     */
    public static List<Object> coerceWeightedPair(List<?> parts) {
        Double weight = parseDouble(parts.get(0));
        String item = String.valueOf(parts.get(1)).trim();
        if (weight == null || item.isEmpty()) {
            return null;
        }
        return List.of(weight, item);
    }

    private static List<Object> coerceMessage(Object value) {
        List<?> parts = asList(value);
        if (parts.size() != 2) {
            return null;
        }
        Boolean enabled = coerceBoolean(parts.get(0));
        if (enabled == null) {
            return null;
        }
        return List.of(enabled, coerceString(parts.get(1), null));
    }

    private static List<Object> coerceSound(Object value) {
        List<?> parts = asList(value);
        if (parts.size() != 4) {
            return null;
        }
        Boolean enabled = coerceBoolean(parts.get(0));
        Double pitch = parseDouble(parts.get(2));
        Double volume = parseDouble(parts.get(3));
        if (enabled == null || pitch == null || volume == null) {
            return null;
        }
        String id = String.valueOf(parts.get(1)).trim();
        if (id.isEmpty()) {
            return null;
        }
        return List.of(enabled, id, pitch, volume);
    }

    /**
     * Raw value as a list: lists pass through, scalars wrap singly.
     * Public so panels share the one raw view with validation.
     */
    public static List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of(value);
    }
}
