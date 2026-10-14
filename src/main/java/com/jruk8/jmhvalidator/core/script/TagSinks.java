package com.jruk8.jmhvalidator.core.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Side-effect delivery tags: messages and sounds in their global,
 * player, and role flavors. Each sends or plays through the context
 * sinks and returns empty. No Bukkit types.
  * Validation carve: evaluation stays engine-side.
*/
public final class TagSinks {

    private TagSinks() {
    }


    /** Audience plus array elements behind the player and role forms. */
    public record AudienceArray(String audience, List<String> elements) {
    }

    /**
     * Array elements when the whole args are one list literal (global
     * forms), else empty so callers keep the individual-arg path.
     */
    public static Optional<List<String>> globalArray(String args) {
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1 || !TagLists.isList(parts.get(0))) {
            return Optional.empty();
        }
        return Optional.of(arrayElements(parts.get(0)));
    }

    /**
     * Audience plus array elements when the args are an audience plus
     * one list literal (player and role forms), else empty so
     * callers keep the individual-arg path.
     */
    public static Optional<AudienceArray> audienceArray(String args) {
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 2 || !TagLists.isList(parts.get(1))) {
            return Optional.empty();
        }
        return Optional.of(new AudienceArray(parts.get(0), arrayElements(parts.get(1))));
    }

    /**
     * Elements of one list literal, split quote-aware so quoted
     * commas inside text survive, unlike the plain list splitter.
     * Blank lists yield no elements.
     */
    private static List<String> arrayElements(String literal) {
        String inner = literal.strip();
        inner = inner.substring(1, inner.length() - 1);
        if (inner.isBlank()) {
            return List.of();
        }
        List<String> elements = new ArrayList<>();
        for (String element : TagLists.splitTopLevel(inner)) {
            elements.add(element.strip());
        }
        return elements;
    }


    /**
     * Edit-time shape for {@code <gmessage>}: one text, or an
     * {@code [enabled,text]} array. Mirrors the runtime warns.
     */
    public static Optional<String> messageError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs one text.");
        }
        Optional<List<String>> array = globalArray(args);
        if (array.isPresent()) {
            return messageArrayError(name, array.get());
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of("Tag <" + name + "> needs one text.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

    /**
     * Edit-time shape for {@code <pmessage>}: a player plus a text,
     * or a player plus an {@code [enabled,text]} array. Mirrors the
     * runtime warns.
     */
    public static Optional<String> playerMessageError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs a player and a text.");
        }
        Optional<AudienceArray> array = audienceArray(args);
        if (array.isPresent()) {
            Optional<String> elements = messageArrayError(name, array.get().elements());
            if (elements.isPresent()) {
                return elements;
            }
            return audienceHygiene(name, array.get().audience());
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 2) {
            return Optional.of("Tag <" + name + "> needs a player and a text.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

    /**
     * Edit-time shape for {@code <psound>}: a player plus an id,
     * pitch, and volume, quotes parsed, or a player plus an
     * {@code [enabled,id,pitch,volume]} array. Mirrors the runtime
     * warns.
     */
    public static Optional<String> playerSoundError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs a player plus a sound id.");
        }
        Optional<AudienceArray> array = audienceArray(args);
        if (array.isPresent()) {
            Optional<String> elements = soundArrayError(name, array.get().elements());
            if (elements.isPresent()) {
                return elements;
            }
            return audienceHygiene(name, array.get().audience());
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 2 || parts.size() > 4) {
            return Optional.of("Tag <" + name + "> needs a player plus an id, pitch, and volume.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

    /**
     * Edit-time shape for {@code <gsound>}: an id, pitch, and volume,
     * quotes parsed, or an {@code [enabled,id,pitch,volume]} array.
     * Mirrors the runtime warns.
     */
    public static Optional<String> soundError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs a sound id.");
        }
        Optional<List<String>> array = globalArray(args);
        if (array.isPresent()) {
            return soundArrayError(name, array.get());
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() < 1 || parts.size() > 3) {
            return Optional.of("Tag <" + name + "> needs an id plus pitch and volume.");
        }
        for (String part : parts) {
            if (TagHeads.parsePickItem(part).isEmpty()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

    /** Edit-time shape for message arrays: enabled plus a text. */
    public static Optional<String> messageArrayError(String name, List<String> elements) {
        if (elements.size() != 2) {
            return Optional.of("Tag <" + name + "> array needs enabled plus a text.");
        }
        return elementsHygiene(name, elements);
    }

    /** Edit-time shape for sound arrays: enabled plus id, pitch, volume. */
    public static Optional<String> soundArrayError(String name, List<String> elements) {
        if (elements.size() < 2 || elements.size() > 4) {
            return Optional.of("Tag <" + name + "> array needs enabled plus an id, pitch, "
                    + "and volume.");
        }
        return elementsHygiene(name, elements);
    }

    /** Quote hygiene across array elements; blank elements stay legal. */
    private static Optional<String> elementsHygiene(String name, List<String> elements) {
        for (String element : elements) {
            if (TagHeads.parsePickItem(element).isEmpty() && !element.isBlank()) {
                return Optional.of("Tag <" + name + "> mixes quotes.");
            }
        }
        return Optional.empty();
    }

    /**
     * Edit-time shape for {@code <gteleport>}: one location
     * {@code [x, y, z, world]} or
     * {@code [x, y, z, world, pitch, yaw]}. Mirrors the runtime
     * warns.
     */
    public static Optional<String> teleportError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs a location "
                    + "[x, y, z, world, pitch, yaw].");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 1) {
            return Optional.of("Tag <" + name + "> needs a location "
                    + "[x, y, z, world, pitch, yaw].");
        }
        if (TagHeads.parsePickItem(parts.get(0)).isEmpty()
                && !parts.get(0).isBlank()) {
            return Optional.of("Tag <" + name + "> mixes quotes.");
        }
        return teleportLocationError(name, parts.get(0));
    }

    /**
     * Edit-time shape for {@code <pteleport>}: a player plus one
     * location. Mirrors the runtime warns.
     */
    public static Optional<String> playerTeleportError(String name, String args) {
        if (args == null || args.isBlank()) {
            return Optional.of("Tag <" + name + "> needs a player and a location "
                    + "[x, y, z, world, pitch, yaw].");
        }
        List<String> parts = TagLists.splitTopLevel(args);
        if (parts.size() != 2) {
            return Optional.of("Tag <" + name + "> needs a player and a location "
                    + "[x, y, z, world, pitch, yaw].");
        }
        Optional<String> audience = audienceHygiene(name, parts.get(0));
        if (audience.isPresent()) {
            return audience;
        }
        return teleportLocationError(name, parts.get(1));
    }

    /**
     * Edit-time shape for one teleport location: a 4- or 6-element
     * list, or a collapsed nested tag the editor cannot see through
     * (runtime validates it).
     */
    public static Optional<String> teleportLocationError(String name, String loc) {
        if (loc.strip().equals("?")) {
            return Optional.empty();
        }
        if (!TagLists.isList(loc)) {
            return Optional.of("Tag <" + name + "> needs a location "
                    + "[x, y, z, world, pitch, yaw].");
        }
        int elements = TagLists.parse(loc).size();
        if (elements != 4 && elements != 6) {
            return Optional.of("Tag <" + name + "> needs a location "
                    + "[x, y, z, world, pitch, yaw].");
        }
        return Optional.empty();
    }

    /** Quote hygiene for the audience head of an array form. */
    private static Optional<String> audienceHygiene(String name, String audience) {
        if (TagHeads.parsePickItem(audience).isEmpty()) {
            return Optional.of("Tag <" + name + "> mixes quotes.");
        }
        return Optional.empty();
    }

}
