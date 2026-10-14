package com.jruk8.jmhvalidator.core;

import java.util.Arrays;
import java.util.Optional;

/**
 * Bundled-id shadows: a scanned file reuses an engine-bundled
 * id without being the pristine bundled copy. Pristine copies
 * (byte-identical to the fetched bundled file) stay silent, so
 * data folders validate clean; everything else sharing the id
 * warns, because where both files are present the first in
 * load order wins. Single source of truth for the CLI finding.
 */
public final class BundledShadow {

    private BundledShadow() {
    }

    /**
     * Message when the scanned bytes are not the pristine
     * bundled copy. A null bundled copy (missing from the
     * defaults dir) still warns: the id match is certain,
     * only pristine-ness is unverifiable. Scanned bytes are
     * never null (unreadable files skip before this call).
     */
    public static Optional<String> check(String kind, String id, byte[] scanned,
            byte[] bundled) {
        if (bundled != null && Arrays.equals(scanned, bundled)) {
            return Optional.empty();
        }
        return Optional.of(message(kind, id));
    }

    /** Canonical shadow message for one kind and id. */
    public static String message(String kind, String id) {
        return "Custom " + kind + " '" + id + "' shares an id with the engine-bundled copy; "
                + "where both files are present the first in load order wins.";
    }
}
