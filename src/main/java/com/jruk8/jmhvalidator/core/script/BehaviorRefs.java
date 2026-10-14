package com.jruk8.jmhvalidator.core.script;

import com.jruk8.jmhvalidator.core.model.BehaviorFile;
import com.jruk8.jmhvalidator.core.model.Diagnostic;
import com.jruk8.jmhvalidator.core.model.DiagnosticCode;
import com.jruk8.jmhvalidator.core.model.Provenance;
import com.jruk8.jmhvalidator.core.model.Severity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Behavior-reference sweep: every {@code <runblock:id>} (plus
 * the {@code <runblock:id,args>} form) in behavior lines must
 * name a defined block of the same modifier, and the call
 * graph must be acyclic. Unknown ids and cycles both warn;
 * the engine skips both at runtime, once per match. Ids
 * resolve like the engine: first top-level comma part, one
 * quote layer stripped, exact match. Blank, dynamic, and
 * malformed ids stay silent here (the syntax sweep owns the
 * line), as do lines with fatal bracket or syntax errors.
 */
public final class BehaviorRefs {

    private BehaviorRefs() {
    }

    /** Check every behavior line of one modifier, in file order. */
    public static List<Diagnostic> check(String modifierId, BehaviorFile file) {
        List<Diagnostic> found = new ArrayList<>();
        if (file == null || file.blocks() == null) {
            return found;
        }
        Set<String> ids = file.blocks().keySet();
        Map<String, List<Edge>> edges = new LinkedHashMap<>();
        for (Map.Entry<String, BehaviorFile.BehaviorBlock> block : file.blocks().entrySet()) {
            if (block.getValue() == null || block.getValue().lists() == null) {
                continue;
            }
            for (Map.Entry<String, List<String>> list : block.getValue().lists().entrySet()) {
                checkList(modifierId, block.getKey(), list.getKey(),
                        list.getValue(), ids, edges, found);
            }
        }
        found.addAll(cycles(modifierId, edges));
        return found;
    }

    private static void checkList(String modifierId, String blockId, String listName,
            List<String> lines, Set<String> ids, Map<String, List<Edge>> edges,
            List<Diagnostic> found) {
        if (lines == null) {
            return;
        }
        for (int index = 0; index < lines.size(); index++) {
            checkLine(modifierId, blockId, listName, index,
                    lines.get(index), ids, edges, found);
        }
    }

    /**
     * Collected runblock calls in one file: resolved target
     * blocks plus whether any id builds dynamically
     * (unresolvable).
     */
    public record BlockCalls(Set<String> targets, boolean dynamic) {
    }

    /** Collect every runblock call in one file, in first-call order. */
    public static BlockCalls collectedCalls(BehaviorFile file) {
        Set<String> targets = new LinkedHashSet<>();
        boolean dynamic = false;
        if (file == null || file.blocks() == null) {
            return new BlockCalls(targets, false);
        }
        Set<String> ids = file.blocks().keySet();
        for (Map.Entry<String, BehaviorFile.BehaviorBlock> block : file.blocks().entrySet()) {
            if (block.getValue() == null || block.getValue().lists() == null) {
                continue;
            }
            for (List<String> lines : block.getValue().lists().values()) {
                if (lines == null) {
                    continue;
                }
                for (String line : lines) {
                    dynamic |= collectLine(line, ids, targets);
                }
            }
        }
        return new BlockCalls(targets, dynamic);
    }

    /** Collect one line's calls; true when a dynamic id appears. */
    private static boolean collectLine(String line, Set<String> ids, Set<String> targets) {
        if (!LineGates.checkable(line)) {
            return false;
        }
        boolean dynamic = false;
        for (int[] span : TagHeads.allTagSpans(line)) {
            String body = line.substring(span[0] + 1, span[1] - 1);
            if (!TagHeads.tagName(body).equals("runblock")) {
                continue;
            }
            Optional<String> first = firstPart(body);
            if (first.isPresent()
                    && (first.get().indexOf('<') >= 0 || first.get().indexOf('>') >= 0)) {
                dynamic = true;
                continue;
            }
            Optional<String> target = idOf(body);
            if (target.isPresent() && ids.contains(target.get())) {
                targets.add(target.get());
            }
        }
        return dynamic;
    }

    private static void checkLine(String modifierId, String blockId, String listName,
            int index, String line, Set<String> ids, Map<String, List<Edge>> edges,
            List<Diagnostic> found) {
        if (!LineGates.checkable(line)) {
            return;
        }
        for (int[] span : TagHeads.allTagSpans(line)) {
            String body = line.substring(span[0] + 1, span[1] - 1);
            if (!TagHeads.tagName(body).equals("runblock")) {
                continue;
            }
            Optional<String> target = idOf(body);
            if (target.isEmpty()) {
                continue;
            }
            if (!ids.contains(target.get())) {
                found.add(new Diagnostic(
                        new Provenance(modifierId, blockId, listName, index),
                        span[0] + 1, Severity.WARNING,
                        DiagnosticCode.W_UNKNOWN_BEHAVIOR,
                        "Tag <runblock> hit unknown behavior '" + target.get()
                                + "'; skipping."));
                continue;
            }
            edges.computeIfAbsent(blockId, key -> new ArrayList<>())
                    .add(new Edge(listName, index, span[0] + 1, target.get()));
        }
    }

    /** One resolved call edge: source line plus target block. */
    private record Edge(String list, int line, int opener, String target) {
    }

    /**
     * Static target id of one runblock body, like the engine:
     * first top-level comma part with one quote layer
     * stripped. Blank, dynamic, and malformed ids stay empty.
     */
    private static Optional<String> idOf(String body) {
        Optional<String> first = firstPart(body);
        if (first.isEmpty()) {
            return Optional.empty();
        }
        String raw = first.get();
        if (raw.indexOf('<') >= 0 || raw.indexOf('>') >= 0) {
            return Optional.empty();
        }
        return TagHeads.parsePickItem(raw).filter(id -> !id.isBlank());
    }

    /** Raw first top-level comma part of one runblock body. */
    private static Optional<String> firstPart(String body) {
        int separator = body.indexOf(':');
        if (separator < 0) {
            return Optional.empty();
        }
        List<String> parts = TagLists.splitTopLevel(body.substring(separator + 1));
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(parts.get(0));
    }

    /**
     * Cycle findings over the resolved edges. The walk mirrors
     * the engine's path set: a target already on the current
     * path closes a cycle. Each closing edge reports once.
     */
    private static List<Diagnostic> cycles(String modifierId,
            Map<String, List<Edge>> edges) {
        List<Diagnostic> found = new ArrayList<>();
        Set<String> reported = new HashSet<>();
        for (String root : edges.keySet()) {
            walk(modifierId, root, edges, new ArrayList<>(), reported, found);
        }
        return found;
    }

    private static void walk(String modifierId, String current,
            Map<String, List<Edge>> edges, List<String> path,
            Set<String> reported, List<Diagnostic> found) {
        path.add(current);
        List<Edge> outs = edges.get(current);
        if (outs != null) {
            for (Edge edge : outs) {
                if (path.contains(edge.target())) {
                    if (reported.add(current + "\0" + edge.target())) {
                        found.add(new Diagnostic(
                                new Provenance(modifierId, current, edge.list(), edge.line()),
                                edge.opener(), Severity.WARNING,
                                DiagnosticCode.W_BEHAVIOR_CYCLE,
                                "Tag <runblock> hit cyclic chain at behavior '"
                                        + edge.target() + "'; skipping."));
                    }
                } else {
                    walk(modifierId, edge.target(), edges, path, reported, found);
                }
            }
        }
        path.remove(path.size() - 1);
    }
}
