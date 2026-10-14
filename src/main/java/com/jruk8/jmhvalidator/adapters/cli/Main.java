package com.jruk8.jmhvalidator.adapters.cli;

import com.jruk8.jmhvalidator.adapters.files.DefaultsFetch;
import com.jruk8.jmhvalidator.adapters.files.EngineDefaults;
import com.jruk8.jmhvalidator.adapters.files.ModifierYaml;
import com.jruk8.jmhvalidator.adapters.files.ShareYaml;
import com.jruk8.jmhvalidator.adapters.files.ShareYamlException;
import com.jruk8.jmhvalidator.adapters.files.TagRegistryFiles;
import com.jruk8.jmhvalidator.core.DuplicateIds;
import com.jruk8.jmhvalidator.core.JmhValidator;
import com.jruk8.jmhvalidator.core.NaturalOrder;
import com.jruk8.jmhvalidator.core.RunTriggers;
import com.jruk8.jmhvalidator.core.SettingsNormalize;
import com.jruk8.jmhvalidator.core.script.SettingRefs;
import com.jruk8.jmhvalidator.core.SettingsStyle;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.SettingsTree;
import com.jruk8.jmhvalidator.core.ports.KnownModifiers;
import com.jruk8.jmhvalidator.core.ports.ShareMaterials;
import com.jruk8.jmhvalidator.core.share.ShareCodec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Validator CLI entry. Exit codes:
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
        if (args.length > 0 && args[0].equals("export")) {
            return exportCommand(List.of(args).subList(1, args.length), Paths.get("export"));
        }
        if (args.length > 0 && args[0].equals("import")) {
            return importCommand(List.of(args).subList(1, args.length), Paths.get("imports"));
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
        Map<Path, Diagnostic> duplicates = duplicateIds(files);
        List<ReportJson.FileReport> reports = new ArrayList<>();
        for (Path file : files) {
            reports.add(checkFile(file, registry, known, duplicates.get(file)));
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
            TagRegistryFiles.LoadedRegistry registry, KnownModifiers known,
            Diagnostic duplicate) {
        String stem = stemOf(file);
        ModifierYaml.ReadResult read = ModifierYaml.read(file, stem);
        List<Diagnostic> found = new ArrayList<>();
        if (duplicate != null) {
            found.add(duplicate);
        }
        found.addAll(read.early());
        if (known != null && read.behavior().blocks().isEmpty()
                && !read.presetMembers().isEmpty()) {
            found.addAll(JmhValidator.validatePreset(stem, read.presetMembers(), known));
        } else {
            found.addAll(JmhValidator.validateBehavior(
                    stem, read.behavior(), registry.registry()));
        }
        SettingsNormalize.Result normalized =
                SettingsNormalize.check(stem, read.settings(), registry.materials());
        found.addAll(normalized.findings());
        SettingsTree refTree =
                normalized.dropped() ? SettingsTree.empty() : read.settings();
        found.addAll(SettingRefs.check(stem, read.behavior(), refTree));
        for (Map.Entry<String, BehaviorFile.BehaviorBlock> block
                : read.behavior().blocks().entrySet()) {
            List<String> runsOn = block.getValue() == null ? null : block.getValue().runsOn();
            found.addAll(RunTriggers.check(stem, block.getKey(), runsOn,
                    registry.triggers()));
        }
        found.addAll(read.keys());
        if (!normalized.dropped()) {
            found.addAll(SettingsStyle.check(stem, read.settings()));
        }
        return new ReportJson.FileReport(file.toString(), found);
    }

    private static String stemOf(Path file) {
        return file.getFileName().toString().replaceFirst("\\.[^.]*$", "");
    }

    /**
     * Duplicate-id pass over the discovered files, in engine load
     * order: within each modifier/preset namespace the first file
     * wins and later same-id files warn. Only {@code .yml} files
     * take part (the engine reports any other suffix as an
     * unknown file instead), and unreadable, unparseable, or
     * misshapen files never occupy an id, mirroring the engine.
     */
    private static Map<Path, Diagnostic> duplicateIds(List<Path> files) {
        Map<String, Path> winners = new HashMap<>();
        Map<Path, Diagnostic> skipped = new HashMap<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            if (!isEngineYaml(name)) {
                continue;
            }
            String id = name.substring(0, name.length() - ".yml".length());
            ModifierYaml.ReadResult read = ModifierYaml.read(file, id);
            if (!occupiesId(read)) {
                continue;
            }
            String kind = isPreset(read) ? "preset" : "modifier";
            Path winner = winners.get(kind + "\0" + id);
            if (winner == null) {
                winners.put(kind + "\0" + id, file);
            } else {
                skipped.put(file, new Diagnostic(new Provenance(id, "", "", -1), -1,
                        Severity.WARNING, DiagnosticCode.W_DUPLICATE_ID,
                        DuplicateIds.message(kind, id,
                                winner.toString(), file.toString())));
            }
        }
        return skipped;
    }

    private static boolean occupiesId(ModifierYaml.ReadResult read) {
        for (Diagnostic early : read.early()) {
            if (early.code() == DiagnosticCode.E_FILE_READ
                    || early.code() == DiagnosticCode.E_YAML_PARSE
                    || early.code() == DiagnosticCode.E_YAML_SHAPE) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPreset(ModifierYaml.ReadResult read) {
        return read.behavior().blocks().isEmpty() && !read.presetMembers().isEmpty();
    }

    private static boolean isEngineYaml(String name) {
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(".yml");
    }

    /**
     * Discovery in engine load order: explicit files in argument
     * order, directories walked subdirs-first with natural-sorted
     * names at every level, so duplicate-id winners match the
     * engine. Roots concatenate in argument order.
     */
    private static List<Path> discover(List<Path> paths) {
        List<Path> files = new ArrayList<>();
        for (Path path : paths) {
            if (Files.isRegularFile(path) && isYaml(path)) {
                files.add(path);
            } else if (Files.isDirectory(path)) {
                collect(path, files);
            }
        }
        return files;
    }

    private static void collect(Path dir, List<Path> files) {
        List<Path> children;
        try (Stream<Path> stream = Files.list(dir)) {
            children = stream.toList();
        } catch (IOException ignored) {
            // Unreadable dirs contribute nothing; files still report.
            return;
        }
        List<Path> subdirs = new ArrayList<>();
        List<Path> own = new ArrayList<>();
        for (Path child : children) {
            if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                subdirs.add(child);
            } else if (Files.isRegularFile(child) && isYaml(child)) {
                own.add(child);
            }
        }
        subdirs.sort(byName());
        own.sort(byName());
        for (Path subdir : subdirs) {
            collect(subdir, files);
        }
        files.addAll(own);
    }

    private static Comparator<Path> byName() {
        return Comparator.comparing(
                path -> path.getFileName().toString(), NaturalOrder.comparator());
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
     * engine rev, for upstream preset checks (preset members
     * resolve against the fetched manifest).
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

    /**
     * Exports modifier and preset files to share strings, one
     * {@code <id>.txt} per file under a stamped export folder.
     * The base overload is for tests; the CLI always uses
     * {@code export} under the working directory.
     */
    static int exportCommand(List<String> args, Path base) {
        if (args.isEmpty()) {
            System.err.println("error: export needs PATH...");
            return 2;
        }
        for (String arg : args) {
            if (arg.startsWith("--")) {
                System.err.println("error: unknown option " + arg);
                return 2;
            }
        }
        List<Path> paths = new ArrayList<>();
        for (String arg : args) {
            paths.add(Paths.get(arg));
        }
        List<Path> files = discover(paths);
        if (files.isEmpty()) {
            System.err.println("error: no .yml/.yaml files found");
            return 2;
        }
        Path dir;
        try {
            dir = nextStampedDir(base, LocalDate.now(), "export");
        } catch (IOException failed) {
            System.err.println("error: cannot create export folder: " + failed.getMessage());
            return 2;
        }
        int failed = 0;
        for (Path file : files) {
            if (!exportFile(file, dir)) {
                failed++;
            }
        }
        System.out.println("exported %d, failed %d to %s"
                .formatted(files.size() - failed, failed, dir));
        return failed == 0 ? 0 : 1;
    }

    private static boolean exportFile(Path file, Path dir) {
        ShareYaml.Parsed parsed;
        try {
            parsed = ShareYaml.parse(file);
        } catch (ShareYamlException failed) {
            System.err.println("error: " + file + ": " + failed.getMessage());
            return false;
        }
        String payload;
        try {
            payload = parsed.kind() == ShareCodec.Kind.PRESET
                    ? ShareCodec.exportPreset(parsed.id(), parsed.preset())
                    : ShareCodec.exportModifier(parsed.id(), parsed.modifier());
        } catch (IllegalArgumentException failed) {
            System.err.println("error: " + file + ": " + failed.getMessage());
            return false;
        }
        Path out = dir.resolve(parsed.id() + ".txt");
        try {
            Files.writeString(out, payload, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException exists) {
            System.err.println("error: " + file + ": duplicate id '" + parsed.id() + "'");
            return false;
        } catch (IOException failed) {
            System.err.println("error: " + file + ": cannot write " + out);
            return false;
        }
        System.out.println("exported " + parsed.id() + " -> " + out);
        return true;
    }

    /**
     * Imports share strings to modifier and preset files, one
     * {@code <id>.yml} per string under a stamped imports folder.
     * Each input reads from its file when it names one, else it
     * is taken as an inline string.
     */
    static int importCommand(List<String> args, Path base) {
        if (args.isEmpty()) {
            System.err.println("error: import needs INPUT...");
            return 2;
        }
        Path dir;
        try {
            dir = nextStampedDir(base, LocalDate.now(), "import");
        } catch (IOException failed) {
            System.err.println("error: cannot create imports folder: " + failed.getMessage());
            return 2;
        }
        int failed = 0;
        for (String input : args) {
            if (!importInput(input, dir)) {
                failed++;
            }
        }
        System.out.println("imported %d, failed %d to %s"
                .formatted(args.size() - failed, failed, dir));
        return failed == 0 ? 0 : 1;
    }

    private static boolean importInput(String input, Path dir) {
        String text = input;
        Path file = Paths.get(input);
        boolean fromFile = Files.isRegularFile(file);
        if (fromFile) {
            try {
                text = Files.readString(file, StandardCharsets.UTF_8).strip();
            } catch (IOException failed) {
                System.err.println("error: " + input + ": cannot read file");
                return false;
            }
        }
        String label = fromFile ? input : shortInput(input);
        Optional<ShareCodec.Imported> decoded =
                ShareCodec.decode(text, ShareMaterials.lenient());
        if (decoded.isEmpty()) {
            System.err.println("error: " + label + ": invalid share string");
            return false;
        }
        ShareCodec.Imported imported = decoded.get();
        Map<String, Object> yaml = imported.kind() == ShareCodec.Kind.PRESET
                ? ShareYaml.writePreset(imported.preset())
                : ShareYaml.writeModifier(imported.modifier());
        Path out = dir.resolve(imported.id() + ".yml");
        try {
            Files.writeString(out, ShareYaml.dump(yaml), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException exists) {
            System.err.println("error: " + label + ": duplicate id '" + imported.id() + "'");
            return false;
        } catch (IOException failed) {
            System.err.println("error: " + label + ": cannot write " + out);
            return false;
        }
        System.out.println("imported " + imported.id() + " -> " + out);
        return true;
    }

    /** Truncates inline strings for error lines; paths print whole. */
    private static String shortInput(String input) {
        return input.length() > 64 ? input.substring(0, 64) + "..." : input;
    }

    /**
     * Next free stamped folder: {@code base/YYYY-MM-DD-N-word/}
     * with N from 1. Package-visible for tests.
     */
    static Path nextStampedDir(Path base, LocalDate date, String word) throws IOException {
        Files.createDirectories(base);
        for (int stamp = 1; ; stamp++) {
            Path dir = base.resolve("%s-%d-%s".formatted(date, stamp, word));
            try {
                Files.createDirectory(dir);
                return dir;
            } catch (FileAlreadyExistsException taken) {
                // Taken: try the next number.
            }
        }
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
                       validator export PATH...
                       validator import INPUT...
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
