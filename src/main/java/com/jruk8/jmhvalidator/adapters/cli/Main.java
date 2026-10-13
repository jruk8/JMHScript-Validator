package com.jruk8.jmhvalidator.adapters.cli;

/**
 * Validator CLI entry. Exit codes mirror the JMH-Mods tester:
 * 0 clean, 1 diagnostics found, 2 usage or setup error.
 */
public final class Main {
    private Main() {
    }

    /** CLI entry: version and help work; validation lands with the core move. */
    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equals("--version")) {
                System.out.println(version());
                return;
            }
            if (arg.equals("--help") || arg.equals("-h")) {
                System.out.println(usage());
                return;
            }
        }
        System.out.println(usage());
    }

    /** Implementation version from the jar manifest, dev when unset. */
    static String version() {
        String found = Main.class.getPackage().getImplementationVersion();
        return found != null ? found : "dev";
    }

    private static String usage() {
        return """
                JMHScript-Validator CLI.
                Usage: validator [--version] [--help]
                Full file validation arrives with the core move.
                """;
    }
}
