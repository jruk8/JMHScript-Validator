# JMHScript-Validator

Standalone JMHScript validation, moved 1:1 out of JManhunt. The
engine and the JMH-Mods tester both consume this module; no
validation logic lives anywhere else.

## Layout

- `src/main/.../core/`: the checking core. `model/` (Provenance,
  Diagnostic, codes, behavior model), `ports/` (TagRegistry),
  `script/` (moved validators), `JmhValidator` (public sweep
  entry). Core imports `java.*` plus fellow core types only
  (enforced by `ArchTest`).
- `src/main/.../adapters/`: `cli/` (Main, versioned JSON/Text
  reports), `files/` (modifier YAML plus registry JSON loading).
- `contract/`: the versioned JSON report schema plus a Python
  subprocess example (no Python port; the jar is the truth).
- `corpus/`: frozen inputs plus goldens from unmodified engine
  code (see `corpus/README.md`).
- `docs/error-catalog.md`: stable diagnostic codes (pinned by
  a test).

## Single sources of truth

- Tag list: owned by the engine. The core takes a `TagRegistry`
  port; JManhunt implements it from live `knownTags`, and the
  CLI reads the exported `tag-registry.json`. The core never
  hardcodes tag names.
- Parser: owned by this module (`TagHeads`, `TagLists`,
  `BracketCheck`). The engine's load path and the validator
  both call it; no duplicate tag-splitting lives elsewhere.
- Messages: byte-identical to the engine, pinned by the corpus
  plus ported unit tests. Codes are new stable metadata;
  consumers map codes, never message text.

## Build and test

Requires JDK 25 (Temurin):

```sh
./gradlew build
```

## CLI

```sh
java -jar JMHScript-Validator-<version>-cli.jar --registry tag-registry.json mods/
java -jar JMHScript-Validator-<version>-cli.jar --registry tag-registry.json --format json mods/
validator docs --registry tag-registry.json   # tag-list markdown for the reference docs
```

Exit 0 clean, 1 diagnostics found, 2 usage or setup error.
`--expect-engine-version` fails closed when the registry does
not match the targeted engine.

## Engine defaults (preset checks)

Preset `modifiers` entries must resolve to a known modifier id.
The engine owns that universe (its bundled defaults plus the
loaded data-folder files), so the CLI checks preset members
only when the defaults are supplied:

```sh
java -jar JMHScript-Validator-<version>-cli.jar fetch-defaults \
  --engine-rev <jmanhunt-rev> --out engine-defaults/
java -jar JMHScript-Validator-<version>-cli.jar --registry tag-registry.json \
  --defaults engine-defaults/ mods/ presets/
```

`fetch-defaults` downloads the default modifier and preset
files at one JManhunt rev and writes `engine-defaults.json`
(format 1: engine, engineVersion, sorted modifier ids, presets
with members in file order) beside them. `--defaults` takes
that file or its directory; known ids are the manifest plus
the scanned modifier stems, mirroring the engine load order.
Unknown members warn as `W_UNKNOWN_MODIFIER` with the
engine's load message. Without `--defaults`, presets stay
silent as before.

## Consuming

- JManhunt: JitPack `com.github.jruk8:JMHScript-Validator:<tag>`
  plus a conditional `includeBuild` for local cross-repo work.
- JMH-Mods: fetch the versioned `-cli.jar` from GitHub releases
  plus `tag-registry.json` from the JManhunt release at the
  pinned tag; assert both versions on every run. For preset
  reference checks, run `fetch-defaults` at the pinned engine
  rev and resolve preset members against the fetched manifest
  plus the repo's own `mods/` ids.

## Releases (fixed order)

1. Tag the validator (`v*`); the release workflow uploads the
   CLI jar. Refresh `tag-registry.json` in JManhunt first when
   the tag table changed.
2. JManhunt bumps its validator version.
3. JMH-Mods bumps its CLI and engine pins.
