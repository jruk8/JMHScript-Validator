package com.jruk8.jmhvalidator.core;

import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.Severity;
import com.jruk8.jmhvalidator.core.ports.KnownModifiers;
import com.jruk8.jmhvalidator.core.ports.TagRegistry;
import com.jruk8.jmhvalidator.core.script.BracketCheck;
import com.jruk8.jmhvalidator.core.script.CommandSyntax;
import com.jruk8.jmhvalidator.core.script.ErrorLocate;
import com.jruk8.jmhvalidator.core.script.PresetRefs;
import com.jruk8.jmhvalidator.core.script.TagFunctionScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Public sweep entry: validates every behavior line of one
 * modifier, in file order. Shape mirrors the engine's load
 * sweep line for line (brackets, then the fatal error, then
 * warnings with def scope); messages stay byte-identical and
 * each finding gains a stable code. Editor single lines go
 * through {@code CommandValidation.validateLine} instead.
 */
public final class JmhValidator {
    private JmhValidator() {
    }

    /**
     * Check one preset member list against the known modifier
     * ids, in member order. Mirrors the engine load filter: one
     * warning per unknown member, known members stay silent.
     */
    public static List<Diagnostic> validatePreset(String presetId,
            List<String> members, KnownModifiers known) {
        List<Diagnostic> found = new ArrayList<>();
        if (members == null || known == null) {
            return found;
        }
        for (int index = 0; index < members.size(); index++) {
            String member = members.get(index);
            if (member != null && !known.isKnown(member)) {
                found.add(new Diagnostic(
                        new Provenance(presetId, "", "modifiers", index), -1,
                        Severity.WARNING, DiagnosticCode.W_UNKNOWN_MODIFIER,
                        PresetRefs.unknownMessage(presetId, member)));
            }
        }
        return found;
    }

    /** Sweep every behavior list of one modifier, in file order. */
    public static List<Diagnostic> validateBehavior(String modifierId,
            BehaviorFile file, TagRegistry registry) {
        List<Diagnostic> found = new ArrayList<>();
        for (Map.Entry<String, BehaviorFile.BehaviorBlock> block : file.blocks().entrySet()) {
            validateBlock(modifierId, block.getKey(), block.getValue(), registry, found);
        }
        return found;
    }

    private static void validateBlock(String modifierId, String blockId,
            BehaviorFile.BehaviorBlock block, TagRegistry registry, List<Diagnostic> found) {
        if (block == null || block.lists() == null) {
            return;
        }
        for (Map.Entry<String, List<String>> list : block.lists().entrySet()) {
            validateList(modifierId, blockId, list.getKey(), list.getValue(),
                    block.lists(), registry, found);
        }
    }

    private static void validateList(String modifierId, String blockId, String listName,
            List<String> lines, Map<String, List<String>> lists,
            TagRegistry registry, List<Diagnostic> found) {
        if (lines == null) {
            return;
        }
        Set<String> functions = functions(lists, listName);
        for (int index = 0; index < lines.size(); index++) {
            validateLine(modifierId, blockId, listName, index,
                    lines.get(index), functions, registry, found);
        }
    }

    private static void validateLine(String modifierId, String blockId, String listName,
            int index, String line, Set<String> functions,
            TagRegistry registry, List<Diagnostic> found) {
        if (line == null) {
            return;
        }
        Provenance provenance = new Provenance(modifierId, blockId, listName, index);
        Optional<BracketCheck.Fault> fault = BracketCheck.firstFault(line);
        if (fault.isPresent()) {
            BracketCheck.Fault failed = fault.get();
            found.add(new Diagnostic(provenance, failed.index() + 1, Severity.ERROR,
                    bracketCode(failed.kind()), describe(failed.kind())));
            return;
        }
        Optional<String> problem = CommandSyntax.error(line);
        if (problem.isPresent()) {
            int at = ErrorLocate.charInLine(line, problem.get()).orElse(-1);
            found.add(new Diagnostic(provenance, at, Severity.ERROR,
                    DiagnosticCode.E_SYNTAX, problem.get()));
            return;
        }
        for (String warning : CommandSyntax.warnings(line, functions, registry)) {
            int at = ErrorLocate.charInLine(line, warning).orElse(-1);
            found.add(new Diagnostic(provenance, at, Severity.WARNING,
                    warnCode(warning), warning));
        }
    }

    private static Set<String> functions(Map<String, List<String>> lists, String listName) {
        List<String> scopeLines = new ArrayList<>();
        for (String scopeList : TagFunctionScope.functionScopeLists(listName)) {
            List<String> lines = lists.get(scopeList);
            if (lines != null) {
                scopeLines.addAll(lines);
            }
        }
        return TagFunctionScope.definedFunctions(scopeLines);
    }

    private static DiagnosticCode bracketCode(BracketCheck.FaultKind kind) {
        if (kind == BracketCheck.FaultKind.UNMATCHED_CLOSE) {
            return DiagnosticCode.E_BRACKET_UNMATCHED_CLOSE;
        }
        return DiagnosticCode.E_BRACKET_UNCLOSED_OPEN;
    }

    private static String describe(BracketCheck.FaultKind kind) {
        if (kind == BracketCheck.FaultKind.UNMATCHED_CLOSE) {
            return "Unmatched angle bracket";
        }
        return "Unclosed angle bracket";
    }

    private static DiagnosticCode warnCode(String warning) {
        if (warning.startsWith("Unknown tag '")) {
            return DiagnosticCode.W_UNKNOWN_TAG;
        }
        if (warning.startsWith("Skipping invalid <random-pick>")) {
            return DiagnosticCode.W_RANDOM_PICK_ITEM;
        }
        if (warning.endsWith("filter should be letters only.")) {
            return DiagnosticCode.W_ALL_PLAYERS_FILTER;
        }
        throw new IllegalStateException("unmapped warning: " + warning);
    }
}
