package com.jruk8.jmhvalidator.adapters.cli;

import com.jruk8.jmhvalidator.adapters.files.DefaultsFetch;
import com.jruk8.jmhvalidator.adapters.files.EngineDefaults;
import com.jruk8.jmhvalidator.adapters.files.ModifierYaml;
import com.jruk8.jmhvalidator.adapters.files.TagRegistryFiles;
import com.jruk8.jmhvalidator.core.JmhValidator;
import com.jruk8.jmhvalidator.core.SettingsStyle;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.ports.KnownModifiers;
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
        if (args.length > 0 && args[0].equals("fetch-defaults")) {
            return fetchDefaults(List.of(args).subList(1, args.length));
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
        KnownModifiers known = null;
        if (options.defaults != null) {
            known = knownModifiers(options, files);
            if (known == null) {
                return 2;
            }
        }
        List<ReportJson.FileReport> reports = new ArrayList<>();
        for (Path file : files) {
            reports.add(checkFile(file, registry, known));
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

    /**
     * Resolve the preset check universe: fetched engine defaults
     * plus the scanned modifier stems, mirroring the engine load
     * order (modifiers resolve before presets). Null when the
     * manifest cannot be read (already reported, exit 2).
     */
    private static KnownModifiers knownModifiers(Options options, List<Path> files) {
        Path manifest = options.defaults;
        if (Files.isDirectory(manifest)) {
            manifest = manifest.resolve(EngineDefaults.MANIFEST_NAME);
        }
        EngineDefaults.LoadedDefaults defaults;
        try {
            defaults = EngineDefaults.read(manifest);
        } catch (IOException failed) {
            System.err.println("error: " + failed.getMessage());
            return null;
        }
        List<String> ids = new ArrayList<>(defaults.modifiers().ids());
        for (Path file : files) {
            String stem = stemOf(file);
            ModifierYaml.ReadResult read = ModifierYaml.read(file, stem);
            if (!read.behavior().blocks().isEmpty()) {
                ids.add(stem);
            }
        }
        return KnownModifiers.of(ids);
    }

    private static ReportJson.FileReport checkFile(Path file,
            TagRegistryFiles.LoadedRegistry registry, KnownModifiers known) {
        String stem = stemOf(file);
        ModifierYaml.ReadResult read = ModifierYaml.read(file, stem);
        List<Diagnostic> found = new ArrayList<>(read.early());
        if (known != null && read.behavior().blocks().isEmpty()
                && !read.presetMembers().isEmpty()) {
            found.addAll(JmhValidator.validatePreset(stem, read.presetMembers(), known));
        } else {
            found.addAll(JmhValidator.validateBehavior(
                    stem, read.behavior(), registry.registry()));
        }
        found.addAll(SettingsStyle.check(stem, read.settings()));
        return new ReportJson.FileReport(file.toString(), found);
    }

    private static String stemOf(Path file) {
        return file.getFileName().toString().replaceFirst("\\.[^.]*$", "");
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

    /**
     * Fetch the engine default modifiers and presets at one
     * engine rev, for upstream preset checks (JMH-Mods resolves
     * its preset members against the fetched manifest).
     */
    static int fetchDefaults(List<String> args) {
        Path out = null;
        String rev = null;
        String repo = DefaultsFetch.DEFAULT_REPO;
        for (int index = 0; index < args.size(); index++) {
            if (args.get(index).equals("--engine-rev") && index + 1 < args.size()) {
                rev = args.get(index + 1);
                index++;
            } else if (args.get(index).equals("--out") && index + 1 < args.size()) {
                out = Paths.get(args.get(index + 1));
                index++;
            } else if (args.get(index).equals("--repo") && index + 1 < args.size()) {
                repo = args.get(index + 1);
                index++;
            }
        }
        if (rev == null || rev.isBlank()) {
            System.err.println("error: fetch-defaults needs --engine-rev REV");
            return 2;
        }
        if (out == null) {
            System.err.println("error: fetch-defaults needs --out DIR");
            return 2;
        }
        DefaultsFetch.Fetched fetched;
        try {
            fetched = DefaultsFetch.fetch(repo, rev, out);
        } catch (IOException failed) {
            System.err.println("error: " + failed.getMessage());
            return 2;
        }
        System.out.println("defaults: " + fetched.modifiers().size() + " modifiers, "
                + fetched.presets().size() + " presets from " + repo + "@" + rev);
        System.out.println("manifest: "
                + fetched.dir().resolve(EngineDefaults.MANIFEST_NAME));
        return 0;
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
                         [--expect-engine-version V] [--defaults FILE|DIR] PATH...
                       validator fetch-defaults --engine-rev REV --out DIR
                         [--repo OWNER/NAME]
                       validator docs --registry REGISTRY [--out FILE]
                       validator [--version] [--help]
                Exits 0 clean, 1 diagnostics found, 2 usage or setup error.
                """;
    }

    /** Parsed CLI options. */
    private record Options(Path registry, String format,
            String expectEngine, Path defaults, List<Path> paths,
            boolean help, boolean version) {
        static Options parse(List<String> args) {
            Parser parser = new Parser();
            for (int index = 0; index < args.size(); index++) {
                index = parser.take(args, index);
            }
            return parser.build();
        }
    }

    /** Mutable option accumulator behind the parse loop. */
    private static final class Parser {
        private Path registry;
        private String format = "text";
        private String expectEngine;
        private Path defaults;
        private final List<Path> paths = new ArrayList<>();
        private boolean help;
        private boolean version;

        int take(List<String> args, int index) {
            String arg = args.get(index);
            if (arg.equals("--help") || arg.equals("-h")) {
                help = true;
            } else if (arg.equals("--version")) {
                version = true;
            } else if (arg.startsWith("--")) {
                return takeValued(args, index);
            } else {
                paths.add(Paths.get(arg));
            }
            return index;
        }

        private int takeValued(List<String> args, int index) {
            String arg = args.get(index);
            if (index + 1 >= args.size()) {
                throw new IllegalArgumentException("unknown option " + arg);
            }
            if (arg.equals("--registry")) {
                registry = Paths.get(args.get(index + 1));
            } else if (arg.equals("--format")) {
                format = args.get(index + 1);
            } else if (arg.equals("--expect-engine-version")) {
                expectEngine = args.get(index + 1);
            } else if (arg.equals("--defaults")) {
                defaults = Paths.get(args.get(index + 1));
            } else {
                throw new IllegalArgumentException("unknown option " + arg);
            }
            return index + 1;
        }

        Options build() {
            if (help || version) {
                return new Options(registry, format, expectEngine, defaults,
                        paths, help, version);
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
            return new Options(registry, format, expectEngine, defaults,
                    paths, false, false);
        }
    }
}
