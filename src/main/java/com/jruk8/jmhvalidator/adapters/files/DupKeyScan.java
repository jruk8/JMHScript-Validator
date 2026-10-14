package com.jruk8.jmhvalidator.adapters.files;

import com.jruk8.jmhvalidator.core.DupKeys;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/**
 * Duplicate-key scan over raw YAML text: composes (parses
 * without constructing) and walks every mapping literal,
 * recording each repeated key with its path and line. The
 * composed tree is walked, never the loaded maps, because the
 * load itself discards the repeats. Aliases are skipped (the
 * anchored mapping is walked once at its anchor), so
 * merge-plus-override never false-positives. Any compose
 * failure yields no hits: the load path owns parse errors.
 */
public final class DupKeyScan {

    private DupKeyScan() {
    }

    /** Scan one document; never throws for content reasons. */
    public static List<DupKeys.Hit> scan(String text) {
        List<DupKeys.Hit> hits = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return hits;
        }
        Node root;
        try {
            root = new Yaml().compose(new StringReader(text));
        } catch (YAMLException failed) {
            return hits;
        }
        if (root == null) {
            return hits;
        }
        walk(root, "", hits);
        return hits;
    }

    private static void walk(Node node, String path, List<DupKeys.Hit> hits) {
        if (node instanceof MappingNode mapping) {
            List<NodeTuple> tuples = mapping.getValue();
            Set<String> seen = new HashSet<>();
            for (NodeTuple tuple : tuples) {
                String key = keyText(tuple.getKeyNode());
                if (!seen.add(key)) {
                    hits.add(new DupKeys.Hit(path, key, lineOf(tuple.getKeyNode())));
                }
            }
            for (NodeTuple tuple : tuples) {
                walk(tuple.getValueNode(), extend(path, keyText(tuple.getKeyNode())), hits);
            }
        } else if (node instanceof SequenceNode sequence) {
            for (Node child : sequence.getValue()) {
                walk(child, path, hits);
            }
        }
    }

    private static String extend(String path, String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    private static String keyText(Node key) {
        if (key instanceof ScalarNode scalar) {
            return scalar.getValue();
        }
        return String.valueOf(key);
    }

    private static int lineOf(Node key) {
        if (key == null || key.getStartMark() == null) {
            return 1;
        }
        return key.getStartMark().getLine() + 1;
    }
}
