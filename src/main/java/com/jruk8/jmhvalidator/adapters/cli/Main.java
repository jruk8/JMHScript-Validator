package com.jruk8.jmhvalidator.adapters.cli;

import com.jruk8.jmhvalidator.adapters.files.ModifierYaml;
import com.jruk8.jmhvalidator.adapters.files.TagRegistryFiles;
import com.jruk8.jmhvalidator.core.JmhValidator;
import com.jruk8.jmhvalidator.core.SettingsStyle;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Validator CLI entry. Exit codes mirror the JMH-Mods tester:
 * 0 clean, 1 diagnostics found, 2 usage or setup error.
 */
public final class Main {
    private Main() {
    }

    /** CLI entry: validate files, render docs, or show help. */
    public static void main(String[] args) {
        System.exit(run(args));
    }

    /** Runnable entry returning the exit code (kept testable). */
    static int run(String[] args) {
        if (args.length > 0 && args[0].equals("docs")) {
            return docs(List.of(args).subList(1, args.length));
        }
        Options options;
        try {
            options = Options.parse(List.of(args));
        } catch (IllegalArgumentException failed) {
            System.err.println("error: " + failed.getMessage());
            return 2;
        }
        if (options.help) {
            System.out.println(usage());
            return 0;
        }
        if (options.version) {
            System.out.println(version());
            return 0;
        }
        return validate(options);
    }

    private static int validate(Options options) {
        TagRegistryFiles.LoadedRegistry registry;
        try {
            registry = TagRegistryFiles.read(options.registry);
        } catch (IOException failed) {
            System.err.println("error: " + failed.getMessage());
            return 2;
        }
        if (options.expectEngine != null
                && !options.expectEngine.equals(registry.engineVersion())) {
            System.err.println("error: registry engine " + registry.engineVersion()
                    + " does not match expected " + options.expectEngine);
            return 2;
        }
        List<Path> files = discover(options.paths);
        if (files.isEmpty()) {
            System.err.println("error: no .yml/.yaml files found");
            return 2;
        }
        List<ReportJson.FileReport> reports = new ArrayList<>();
        for (Path file : files) {
            reports.add(checkFile(file, registry));
        }
        if (options.format.equals("json")) {
            System.out.print(ReportJson.report(version(), registry.engine(),
                    registry.engineVersion(), reports));
        } else {
            System.out.print(renderText(reports));
        }
        return exitFor(reports);
    }

    private static int exitFor(List<ReportJson.FileReport> reports) {
        for (ReportJson.FileReport report : reports) {
            if (!report.diagnostics().isEmpty()) {
                return 1;
            }
        }
        return 0;
    }

    private static ReportJson.FileReport checkFile(Path file,
            TagRegistryFiles.LoadedRegistry registry) {
        String stem = file.getFileName().toString().replaceFirst("\\.[^.]*$", "");
        ModifierYaml.ReadResult read = ModifierYaml.read(file, stem);
        List<Diagnostic> found = new ArrayList<>(read.early());
        found.addAll(JmhValidator.validateBehavior(
                stem, read.behavior(), registry.registry()));
        found.addAll(SettingsStyle.check(stem, read.settings()));
        return new ReportJson.FileReport(file.toString(), found);
    }

    private static List<Path> discover(List<Path> paths) {
        List<Path> files = new ArrayList<>();
        for (Path path : paths) {
            if (Files.isRegularFile(path) && isYaml(path)) {
                files.add(path);
            } else if (Files.isDirectory(path)) {
                try (Stream<Path> walk = Files.walk(path)) {
                    walk.filter(Files::isRegularFile).filter(Main::isYaml).forEach(files::add);
                } catch (IOException ignored) {
                    // Unreadable dirs contribute nothing; files still report.
                }
            }
        }
        files.sort(Comparator.comparing(Path::toString));
        return files;
    }

    private static boolean isYaml(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }

    private static String renderText(List<ReportJson.FileReport> reports) {
        StringBuilder out = new StringBuilder();
        int errors = 0;
        int warnings = 0;
        int dirty = 0;
        for (ReportJson.FileReport report : reports) {
            if (!report.diagnostics().isEmpty()) {
                dirty++;
            }
            for (Diagnostic found : report.diagnostics()) {
                if (found.severity() == Severity.ERROR) {
                    errors++;
                } else {
                    warnings++;
                }
                out.append(textLine(report.file(), found)).append("\n");
            }
        }
        out.append("%d files: %d clean, %d with diagnostics (errors: %d, warnings: %d)%n"
                .formatted(reports.size(), reports.size() - dirty, dirty, errors, warnings));
        return out.toString();
    }

    private static String textLine(String file, Diagnostic found) {
        String where = file;
        if (!found.provenance().behaviorId().isBlank()) {
            where += " " + found.provenance().behaviorId()
                    + "/" + found.provenance().listName()
                    + ":" + (found.provenance().lineIndex() + 1);
            if (found.charOneBased() > 0) {
                where += ":" + found.charOneBased();
            }
        } else if (!found.provenance().listName().isBlank()) {
            where += " " + found.provenance().listName();
        }
        return where + " " + found.severity().name().toLowerCase()
                + " " + found.code().name() + ": " + found.message();
    }

    /** Tag-list markdown for the reference docs, from a registry file. */
    static int docs(List<String> args) {
        Path registry = null;
        Path out = null;
        for (int index = 0; index < args.size(); index++) {
            if (args.get(index).equals("--registry") && index + 1 < args.size()) {
                registry = Paths.get(args.get(index + 1));
                index++;
            } else if (args.get(index).equals("--out") && index + 1 < args.size()) {
                out = Paths.get(args.get(index + 1));
                index++;
            }
        }
        if (registry == null) {
            System.err.println("error: docs needs --registry PATH");
            return 2;
        }
        TagRegistryFiles.LoadedRegistry loaded;
        try {
            loaded = TagRegistryFiles.read(registry);
        } catch (IOException failed) {
            System.err.println("error: " + failed.getMessage());
            return 2;
        }
        String markdown = tagListMarkdown(loaded);
        if (out == null) {
            System.out.print(markdown);
            return 0;
        }
        try {
            Files.writeString(out, markdown, StandardCharsets.UTF_8);
        } catch (IOException failed) {
            System.err.println("error: cannot write " + out + ": " + failed.getMessage());
            return 2;
        }
        return 0;
    }

    private static String tagListMarkdown(TagRegistryFiles.LoadedRegistry loaded) {
        List<String> tags = new ArrayList<>(loaded.registry().tags());
        tags.sort(String::compareTo);
        StringBuilder out = new StringBuilder();
        out.append("<!-- GENERATED from tag-registry.json (engine ")
                .append(loaded.engineVersion()).append("); do not hand-edit. -->\n");
        for (String tag : tags) {
            out.append("- `").append(tag).append("`\n");
        }
        return out.toString();
    }

    /** Implementation version from the jar manifest, dev when unset. */
    static String version() {
        String found = Main.class.getPackage().getImplementationVersion();
        return found != null ? found : "dev";
    }

    private static String usage() {
        return """
                JMHScript-Validator CLI.
                Usage: validator --registry REGISTRY [--format text|json]
                         [--expect-engine-version V] PATH...
                       validator docs --registry REGISTRY [--out FILE]
                       validator [--version] [--help]
                Exits 0 clean, 1 diagnostics found, 2 usage or setup error.
                """;
    }

    /** Parsed CLI options. */
    private record Options(Path registry, String format,
            String expectEngine, List<Path> paths, boolean help, boolean version) {
        static Options parse(List<String> args) {
            Path registry = null;
            String format = "text";
            String expectEngine = null;
            List<Path> paths = new ArrayList<>();
            boolean help = false;
            boolean version = false;
            for (int index = 0; index < args.size(); index++) {
                String arg = args.get(index);
                if (arg.equals("--registry") && index + 1 < args.size()) {
                    registry = Paths.get(args.get(index + 1));
                    index++;
                } else if (arg.equals("--format") && index + 1 < args.size()) {
                    format = args.get(index + 1);
                    index++;
                } else if (arg.equals("--expect-engine-version") && index + 1 < args.size()) {
                    expectEngine = args.get(index + 1);
                    index++;
                } else if (arg.equals("--help") || arg.equals("-h")) {
                    help = true;
                } else if (arg.equals("--version")) {
                    version = true;
                } else if (arg.startsWith("--")) {
                    throw new IllegalArgumentException("unknown option " + arg);
                } else {
                    paths.add(Paths.get(arg));
                }
            }
            if (help || version) {
                return new Options(registry, format, expectEngine, paths, help, version);
            }
            if (registry == null) {
                throw new IllegalArgumentException("missing --registry PATH");
            }
            if (!format.equals("text") && !format.equals("json")) {
                throw new IllegalArgumentException("format must be text or json");
            }
            if (paths.isEmpty()) {
                throw new IllegalArgumentException("no paths to validate");
            }
            return new Options(registry, format, expectEngine, paths, false, false);
        }
    }
}
