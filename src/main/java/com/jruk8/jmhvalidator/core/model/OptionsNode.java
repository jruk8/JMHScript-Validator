package com.jruk8.jmhvalidator.core.model;

/**
 * Raw behavior option values for one block: the scalars under
 * {@code options} and {@code on-start}, exactly as the file
 * adapter read them. Null marks an absent (or explicitly
 * null) value, which the engine defaults silently. Maps and
 * lists pass through untouched for the shape checks to own.
 */
public record OptionsNode(Object interval, Object deviation, Object intervalBehavior,
        Object chance, Object chanceBehavior, Object selection,
        Object pickCount, Object pickBehavior, Object delay, Object preStartOrder) {

    /** Empty options for blocks without an options subtree. */
    public static OptionsNode empty() {
        return new OptionsNode(null, null, null, null, null, null, null, null, null, null);
    }
}
