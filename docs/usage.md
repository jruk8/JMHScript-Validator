# CLI usage

Your mods folder is the `mods/` directory beside your server config
(modifiers under `mods/modifiers/`, presets under
`mods/presets/`). Files you write by hand or import from Discord
skip the GUI save check, so validate the folder before you ship
the mod or start the match.

The tag table comes from `tag-registry.json`, exported from the
engine. Each copy carries the engine version its tags came from;
refresh it from a JManhunt checkout with `./gradlew
exportTagRegistry` when the tag, trigger, or material tables
change. Registries without the newer arrays skip those checks
silently.

## Reading the results

Findings print per file in text form, or as versioned JSON with
`--format json` (shape pinned by
[contract/report-schema.json](../contract/report-schema.json)):

```sh
java -jar JMHScript-Validator-<version>-cli.jar --registry tag-registry.json --format json mods/
```

Exit codes: 0 clean, 1 diagnostics found, 2 usage or setup
error. Add `--expect-engine-version <rev>` to fail closed when
the registry does not match the engine you target.

## Checking presets too

Preset `modifiers` entries must resolve to a known modifier
id. The engine owns that universe (its bundled defaults plus
the loaded data-folder files), so the CLI checks preset members
only when the defaults are supplied:

```sh
java -jar JMHScript-Validator-<version>-cli.jar fetch-defaults \
  --engine-rev <jmanhunt-rev> --out engine-defaults/
java -jar JMHScript-Validator-<version>-cli.jar --registry tag-registry.json \
  --defaults engine-defaults/ mods/ presets/
```

`fetch-defaults` downloads the default modifier and preset files
at one JManhunt rev and writes `engine-defaults.json` (format 1:
engine, engineVersion, sorted modifier ids, presets with members
in file order) beside them. `--defaults` takes that file or its
directory; known ids are the manifest plus the scanned modifier
stems, mirroring the engine load order. Unknown members warn as
`W_UNKNOWN_MODIFIER` with the engine's load message. Without
`--defaults`, presets stay silent as before.

## Sharing mods

`export` converts modifier and preset files to share strings
without needing a registry, one `<id>.txt` per file:

```sh
java -jar JMHScript-Validator-<version>-cli.jar export mods/
```

`import` takes each input inline or from a file holding only the
string (any extension) and writes one `<id>.yml` per string:

```sh
java -jar JMHScript-Validator-<version>-cli.jar import JMH1D:...
java -jar JMHScript-Validator-<version>-cli.jar import export/2026-10-14-1-export/beef.txt
```

Exports land in `export/YYYY-MM-DD-N-export/`, imports in
`imports/YYYY-MM-DD-N-import/`, with N counting from 1 per day.
