package com.jruk8.jmhvalidator.adapters.cli;

import com.jruk8.jmhvalidator.adapters.files.DefaultsFetch;
import com.jruk8.jmhvalidator.adapters.files.EngineDefaults;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** The {@code fetch-defaults} subcommand: fetch engine defaults at one rev. */
final class FetchDefaultsCommand {
    private FetchDefaultsCommand() {
    }

    /**
     * Fetch the engine default modifiers and presets at one
     * validator release tag, for upstream preset checks
     * (preset members resolve against the fetched manifest).
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
}
