package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Modifier YAML reader: parses one file into the core behavior
 * model. Structural problems become file diagnostics (the sweep
 * never sees them); files without a behavior section (presets)
 * read as empty. Map order follows the file, so provenance
 * matches the engine's load order.
 */
public final class ModifierYaml {
    /** One read: the model plus any file-level diagnostics. */
    public record ReadResult(BehaviorFile behavior, List<Diagnostic> early) {
    }

    private ModifierYaml() {
    }

    /** Read one modifier file; never throws for content reasons. */
    public static ReadResult read(Path file, String modifierId) {
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException failed) {
            return resultWith(BehaviorFile.empty(), fileDiagnostic(modifierId,
                    DiagnosticCode.E_FILE_READ, "cannot read file: " + failed.getMessage()));
        }
        Object doc;
        try {
            doc = new Yaml().load(text);
        } catch (YAMLException failed) {
            return resultWith(BehaviorFile.empty(), fileDiagnostic(modifierId,
                    DiagnosticCode.E_YAML_PARSE, "unparseable YAML: " + oneLine(failed)));
        }
        if (!(doc instanceof Map<?, ?> map)) {
            return resultWith(BehaviorFile.empty(), fileDiagnostic(modifierId,
                    DiagnosticCode.E_YAML_PARSE, "expected one mapping document"));
        }
        return readBehavior(modifierId, map);
    }

    private static ReadResult readBehavior(String modifierId, Map<?, ?> doc) {
        Object behavior = doc.get("behavior");
        if (behavior == null) {
            return new ReadResult(BehaviorFile.empty(), List.of());
        }
        if (!(behavior instanceof Map<?, ?> blocks)) {
            return resultWith(BehaviorFile.empty(), fileDiagnostic(modifierId,
                    DiagnosticCode.E_YAML_SHAPE, "'behavior' must be a map"));
        }
        Map<String, BehaviorFile.BehaviorBlock> out = new LinkedHashMap<>();
        List<Diagnostic> early = new ArrayList<>();
        for (Map.Entry<?, ?> block : blocks.entrySet()) {
            out.put(String.valueOf(block.getKey()),
                    readBlock(modifierId, String.valueOf(block.getKey()), block.getValue(), early));
        }
        return new ReadResult(new BehaviorFile(out), early);
    }

    private static BehaviorFile.BehaviorBlock readBlock(String modifierId,
            String blockId, Object raw, List<Diagnostic> early) {
        Map<String, List<String>> lists = new LinkedHashMap<>();
        if (!(raw instanceof Map<?, ?> block)) {
            early.add(shaped(modifierId, blockId,
                    "'behavior." + blockId + "' must be a map"));
            return new BehaviorFile.BehaviorBlock(lists);
        }
        Object commands = block.get("commands");
        if (commands == null) {
            return new BehaviorFile.BehaviorBlock(lists);
        }
        if (!(commands instanceof Map<?, ?> groups)) {
            early.add(shaped(modifierId, blockId,
                    "'behavior." + blockId + ".commands' must be a map"));
            return new BehaviorFile.BehaviorBlock(lists);
        }
        for (Map.Entry<?, ?> list : groups.entrySet()) {
            lists.put(String.valueOf(list.getKey()),
                    readLines(modifierId, blockId, String.valueOf(list.getKey()),
                            list.getValue(), early));
        }
        return new BehaviorFile.BehaviorBlock(lists);
    }

    private static List<String> readLines(String modifierId, String blockId,
            String listName, Object raw, List<Diagnostic> early) {
        List<String> lines = new ArrayList<>();
        if (!(raw instanceof List<?> items)) {
            early.add(shaped(modifierId, blockId,
                    "'behavior." + blockId + ".commands." + listName + "' must be a list"));
            return lines;
        }
        for (int index = 0; index < items.size(); index++) {
            Object entry = items.get(index);
            if (entry instanceof String line) {
                lines.add(line);
            } else {
                lines.add(null);
                early.add(new Diagnostic(
                        new Provenance(modifierId, blockId, listName, index), -1,
                        Severity.ERROR, DiagnosticCode.E_YAML_LINE_TYPE,
                        "command line must be a string"));
            }
        }
        return lines;
    }

    private static Diagnostic shaped(String modifierId, String blockId, String message) {
        return new Diagnostic(new Provenance(modifierId, blockId, "", -1), -1,
                Severity.ERROR, DiagnosticCode.E_YAML_SHAPE, message);
    }

    private static Diagnostic fileDiagnostic(String modifierId,
            DiagnosticCode code, String message) {
        return new Diagnostic(new Provenance(modifierId, "", "", -1), -1,
                Severity.ERROR, code, message);
    }

    private static ReadResult resultWith(BehaviorFile behavior, Diagnostic early) {
        return new ReadResult(behavior, List.of(early));
    }

    private static String oneLine(Exception failed) {
        return String.join(" ", String.valueOf(failed.getMessage()).split("\\s+"));
    }
}
