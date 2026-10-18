package com.jruk8.jmhvalidator.adapters.cli;

import com.jruk8.jmhvalidator.adapters.files.EngineDefaults;
import com.jruk8.jmhvalidator.adapters.files.ModifierYaml;
import com.jruk8.jmhvalidator.adapters.files.ShareYaml;
import com.jruk8.jmhvalidator.adapters.files.ShareYamlException;
import com.jruk8.jmhvalidator.adapters.files.TagRegistryFiles;
import com.jruk8.jmhvalidator.core.BundledShadow;
import com.jruk8.jmhvalidator.core.CommandLists;
import com.jruk8.jmhvalidator.core.DuplicateIds;
import com.jruk8.jmhvalidator.core.JmhValidator;
import com.jruk8.jmhvalidator.core.NaturalOrder;
import com.jruk8.jmhvalidator.core.PaidContent;
import com.jruk8.jmhvalidator.core.RunTriggers;
import com.jruk8.jmhvalidator.core.SettingsNormalize;
import com.jruk8.jmhvalidator.core.TriggerParams;
import com.jruk8.jmhvalidator.core.script.BehaviorRefs;
import com.jruk8.jmhvalidator.core.script.SettingRefs;
import com.jruk8.jmhvalidator.core.script.UncalledSweep;
import com.jruk8.jmhvalidator.core.SettingsStyle;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.OptionsNode;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
            return FetchDefaultsCommand.fetchDefaults(List.of(args).subList(1, args.length));
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
        Universes universes = null;
        if (options.defaults != null) {
            universes = universes(options, files);
            if (universes == null) {
                return 2;
            }
        }
        KnownModifiers known = universes == null ? null : universes.known();
        ShadowUniverse shadow = universes == null ? null : universes.shadow();
        IdPass pass = duplicateIds(files, shadow);
        List<ReportJson.FileReport> reports = new ArrayList<>();
        List<Boolean> premiums = new ArrayList<>();
        for (Path file : files) {
            CheckedFile checked = checkFile(file, registry, known, pass.duplicates().get(file),
                    pass.shadows().get(file), options.free());
            reports.add(checked.report());
            premiums.add(checked.premium());
        }
        if (options.format.equals("json")) {
            System.out.print(ReportJson.report(version(), registry.engine(),
                    registry.engineVersion(), reports));
        } else {
            System.out.print(ReportText.renderText(reports, premiums));
        }
        return exitFor(reports);
    }

    /** One checked file: its report plus the premium marker behind the text output. */
    private record CheckedFile(ReportJson.FileReport report, boolean premium) {
    }

    private static int exitFor(List<ReportJson.FileReport> reports) {
        for (ReportJson.FileReport report : reports) {
            if (!report.diagnostics().isEmpty()) {
                return 1;
            }
        }
        return 0;
    }

    /** Both defaults-derived universes, read once. Null fields never occur. */
    private record Universes(KnownModifiers known, ShadowUniverse shadow) {
    }

    /**
     * Bundled id sets plus the defaults root for pristine
     * comparison. The root mirrors the fetch cache layout
     * ({@code modifiers/*.yml}, {@code presets/*.yml}).
     */
    private record ShadowUniverse(Set<String> modifiers, Set<String> presets, Path root) {
        boolean isBundled(String kind, String id) {
            if (id == null) {
                return false;
            }
            return "preset".equals(kind) ? presets.contains(id) : modifiers.contains(id);
        }

        byte[] bundledBytes(String kind, String id) {
            Path file = root.resolve("preset".equals(kind) ? "presets" : "modifiers")
                    .resolve(id + ".yml");
            try {
                return Files.readAllBytes(file);
            } catch (IOException unfound) {
                return null;
            }
        }
    }

    /**
     * Resolve the preset check universe (fetched engine defaults
     * plus the scanned modifier stems, mirroring the engine load
     * order) and the shadow universe (bundled-only ids plus the
     * defaults root). Null when the manifest cannot be read
     * (already reported, exit 2).
     */
    private static Universes universes(Options options, List<Path> files) {
        Path manifest = options.defaults;
        Path root;
        if (Files.isDirectory(manifest)) {
            root = manifest;
            manifest = manifest.resolve(EngineDefaults.MANIFEST_NAME);
        } else {
            root = manifest.toAbsolutePath().getParent();
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
        Set<String> bundledPresets = new HashSet<>();
        for (EngineDefaults.Preset preset : defaults.presets()) {
            if (preset.id() != null) {
                bundledPresets.add(preset.id());
            }
        }
        return new Universes(KnownModifiers.of(ids),
                new ShadowUniverse(Set.copyOf(defaults.modifiers().ids()),
                        Set.copyOf(bundledPresets), root));
    }

    private static CheckedFile checkFile(Path file,
            TagRegistryFiles.LoadedRegistry registry, KnownModifiers known,
            Diagnostic duplicate, Diagnostic shadow, boolean freeEdition) {
        String stem = stemOf(file);
        ModifierYaml.ReadResult read = ModifierYaml.read(file, stem);
        List<Diagnostic> found = new ArrayList<>();
        if (duplicate != null) {
            found.add(duplicate);
        }
        if (shadow != null) {
            found.add(shadow);
        }
        found.addAll(read.early());
        if (known != null && read.behavior().blocks().isEmpty()
                && !read.presetMembers().isEmpty()) {
            found.addAll(JmhValidator.validatePreset(stem, read.presetMembers(), known));
        } else {
            found.addAll(JmhValidator.validateBehavior(
                    stem, read.behavior(), registry.registry(), freeEdition));
        }
        SettingsNormalize.Result normalized =
                SettingsNormalize.check(stem, read.settings(), registry.materials());
        found.addAll(normalized.findings());
        SettingsTree refTree =
                normalized.dropped() ? SettingsTree.empty() : read.settings();
        found.addAll(SettingRefs.check(stem, read.behavior(), refTree));
        found.addAll(BehaviorRefs.check(stem, read.behavior()));
        found.addAll(blockFindings(stem, read, registry));
        found.addAll(read.keys());
        found.addAll(read.shapes());
        if (!normalized.dropped()) {
            found.addAll(SettingsStyle.check(stem, read.settings()));
        }
        found.addAll(UncalledSweep.check(stem, read.behavior(), refTree));
        boolean premium = PaidContent.isPremium(read.behavior(), registry.registry());
        return new CheckedFile(new ReportJson.FileReport(file.toString(), found), premium);
    }

    /** Per-block trigger, list, and intercept findings behind one file. */
    private static List<Diagnostic> blockFindings(String stem, ModifierYaml.ReadResult read,
            TagRegistryFiles.LoadedRegistry registry) {
        List<Diagnostic> found = new ArrayList<>();
        for (Map.Entry<String, BehaviorFile.BehaviorBlock> block
                : read.behavior().blocks().entrySet()) {
            List<String> runsOn = block.getValue() == null ? null : block.getValue().runsOn();
            found.addAll(RunTriggers.check(stem, block.getKey(), runsOn,
                    registry.triggers()));
            OptionsNode options =
                    block.getValue() == null ? null : block.getValue().options();
            found.addAll(TriggerParams.check(stem, block.getKey(), options));
            Set<String> lists =
                    block.getValue() == null || block.getValue().lists() == null
                            ? null : block.getValue().lists().keySet();
            found.addAll(CommandLists.check(stem, block.getKey(), lists));
            found.addAll(com.jruk8.jmhvalidator.core.InterceptCheck.check(stem,
                    block.getValue(), block.getKey(), registry.interceptable()));
        }
        return found;
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
    /** Both id-level passes over the discovered files. */
    private record IdPass(Map<Path, Diagnostic> duplicates, Map<Path, Diagnostic> shadows) {
    }

    private static IdPass duplicateIds(List<Path> files, ShadowUniverse shadow) {
        Map<String, Path> winners = new HashMap<>();
        Map<Path, Diagnostic> skipped = new HashMap<>();
        Map<Path, Diagnostic> shadows = new HashMap<>();
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
                shadowFinding(file, kind, id, shadow, shadows);
            } else {
                skipped.put(file, new Diagnostic(new Provenance(id, "", "", -1), -1,
                        Severity.WARNING, DiagnosticCode.W_DUPLICATE_ID,
                        DuplicateIds.message(kind, id,
                                winner.toString(), file.toString())));
            }
        }
        return new IdPass(skipped, shadows);
    }

    /**
     * Shadow finding for one winning file: bundled id without
     * pristine bytes. Null shadow (no {@code --defaults}) and
     * unreadable files stay silent.
     */
    private static void shadowFinding(Path file, String kind, String id,
            ShadowUniverse shadow, Map<Path, Diagnostic> shadows) {
        if (shadow == null || !shadow.isBundled(kind, id)) {
            return;
        }
        byte[] scanned;
        try {
            scanned = Files.readAllBytes(file);
        } catch (IOException unreadable) {
            return;
        }
        Optional<String> message = BundledShadow.check(kind, id, scanned,
                shadow.bundledBytes(kind, id));
        if (message.isPresent()) {
            shadows.put(file, new Diagnostic(new Provenance(id, "", "", -1), -1,
                    Severity.WARNING, DiagnosticCode.W_SHADOWED_ID, message.get()));
        }
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
                         [--expect-engine-version V] [--defaults FILE|DIR] [--free] PATH...
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
            boolean help, boolean version, boolean free) {
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
        private boolean free;

        int take(List<String> args, int index) {
            String arg = args.get(index);
            if (arg.equals("--help") || arg.equals("-h")) {
                help = true;
            } else if (arg.equals("--version")) {
                version = true;
            } else if (arg.equals("--free")) {
                free = true;
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
                        paths, help, version, free);
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
                    paths, false, false, free);
        }
    }
}
