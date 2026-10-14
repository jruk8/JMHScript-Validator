package com.jruk8.jmhvalidator.adapters.files;

/**
 * One modifier or preset file the share adapter cannot read:
 * unreadable, unparseable, or off-shape. Messages stay single
 * line so the CLI can print them per file.
 */
public final class ShareYamlException extends Exception {
    ShareYamlException(String message) {
        super(message);
    }
}
