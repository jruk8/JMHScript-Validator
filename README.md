<div align="center">

![JMHScript-Validator banner](docs/assets/validator-banner-1280x640.png)

<h3>Lightweight CLI validator for JMHScript syntax.</h3>

### [Releases](https://github.com/jruk8/JMHScript-Validator/releases) · [JMHScript DSL](https://jruk8.github.io/JManhunt/configuration/modifiers/) · [Community Mods](https://discord.com/invite/hkWmCVmWDC)

<br>

</div>

# Quick Start ✅

1. Download the validator CLI jar from [GitHub releases](https://github.com/jruk8/JMHScript-Validator/releases), plus the `tag-registry.json` that matches your JManhunt version (it lives at the root of the [JManhunt repo](https://github.com/jruk8/JManhunt); each copy carries the engine version its tags came from).
2. Point it at your mods folder (single files work too):

```sh 
java -jar JMHScript-Validator-<version>-cli.jar --registry tag-registry.json mods/
```

**That's it!** Exit 0 means clean. Anything else prints findings per file, each naming the behavior, list, line, and what the engine would do with it at runtime.

Your mods folder is the `mods/` directory beside your server config (modifiers under `mods/modifiers/`, presets under `mods/presets/`). Files you write by hand or import from Discord skip the GUI save check, so validate the folder before you ship the mod or start the match.

# Why Validate?

The modifier GUI validates commands on save: unbalanced brackets, malformed tags, and unknown items are refused, while unknown tags only warn. But lines edited outside the GUI skip that check, so at runtime a broken line only warns in the console naming the line instead of running. The validator runs those same checks offline, with message text byte-identical to the engine, so what you see here is what the server would say there.

# What It Catches

- **Broken syntax** » Unmatched `<` and `>` brackets plus malformed tag arguments, with the engine's own messages.
- **Unknown tags** » Tags outside your engine version's table, which the engine leaves untouched at runtime.
- **Skipped picks** » Random-pick entries the runtime skips, and weak all-players filters.
- **Unknown preset members** » Preset entries naming no known modifier (needs `--defaults`, see below).
- **Duplicate ids** » Later same-id files the engine skips unparsed, in engine load order.
- **Bundled-id shadows** » Custom ids matching engine-bundled ids (needs `--defaults`), which collide where both copies are present.
- **Key typos** » YAML keys the engine never reads (`conflg`, `behavio`), at every level.
- **Unknown triggers** » `runs-on` entries that never match an event, so the block never fires.
- **Bad option values** » Behavior options the engine clamps or defaults silently (unknown scopes, out-of-range chance, disabling intervals).
- **Dangling setting refs** » `<setting:>` calls naming no defined leaf, which read null at runtime.
- **Dangling runblocks** » `<runblock:>` calls naming no defined block, plus call cycles, both skipped at runtime.
- **Uncalled definitions** » Leaf settings never read and blocks never called, which usually flag a typo'd call site.
- **Duplicate settings** » Repeated sibling setting names, which drop the whole `config.settings` block.
- **Ambiguous settings** » Settings defining both value and folder, where the value wins and the folder drops.
- **Missing folder icons** » Settings folders without a `meta.icon`, which the engine load also reports.
- **Bad defaults and values** » Setting defaults and live values outside their allowed range (clamped or reset at load) and unknown spec types.
- **Settings style** » Overstuffed or misplaced `config.settings` blocks (too many folders or leafs, stray leafs beside folders).
- **Bad files** » Unreadable paths, unparseable YAML, wrong section shapes, and non-string command lines.
- **Tolerated shapes** » Skipped, defaulted, or stringified nodes the engine loads past (scalar commands, empty lists, misshapen settings).
- **Duplicate YAML keys** » Repeated keys in one mapping, which silently drop every value but the last.

Every finding carries a stable code (see [docs/error-catalog.md](docs/error-catalog.md)). Map codes in tooling, never message text.

# Reading the Results

Findings print per file in text form, or as versioned JSON with `--format json` (shape pinned by [contract/report-schema.json](contract/report-schema.json)):

```sh
java -jar JMHScript-Validator-<version>-cli.jar --registry tag-registry.json --format json mods/
```

Exit codes: 0 clean, 1 diagnostics found, 2 usage or setup error. Add `--expect-engine-version <rev>` to fail closed when the registry does not match the engine you target.

# Checking Presets Too

Preset `modifiers` entries must resolve to a known modifier id. The engine owns that universe (its bundled defaults plus the loaded data-folder files), so the CLI checks preset members only when the defaults are supplied:

```sh
java -jar JMHScript-Validator-<version>-cli.jar fetch-defaults \
  --engine-rev <jmanhunt-rev> --out engine-defaults/
java -jar JMHScript-Validator-<version>-cli.jar --registry tag-registry.json \
  --defaults engine-defaults/ mods/ presets/
```

`fetch-defaults` downloads the default modifier and preset files at one JManhunt rev and writes `engine-defaults.json` (format 1: engine, engineVersion, sorted modifier ids, presets with members in file order) beside them. `--defaults` takes that file or its directory; known ids are the manifest plus the scanned modifier stems, mirroring the engine load order. Unknown members warn as `W_UNKNOWN_MODIFIER` with the engine's load message. Without `--defaults`, presets stay silent as before.

# Sharing Mods

`export` converts modifier and preset files to share strings without needing a registry, one `<id>.txt` per file:

```sh
java -jar JMHScript-Validator-<version>-cli.jar export mods/
```

`import` takes each input inline or from a file holding only the string (any extension) and writes one `<id>.yml` per string:

```sh
java -jar JMHScript-Validator-<version>-cli.jar import JMH1D:...
java -jar JMHScript-Validator-<version>-cli.jar import export/2026-10-14-1-export/beef.txt
```

Exports land in `export/YYYY-MM-DD-N-export/`, imports in `imports/YYYY-MM-DD-N-import/`, with N counting from 1 per day.

# Requirements

- Java Version » **Java 25**
- Tag table » **`tag-registry.json` from your JManhunt version** (refresh it from the engine with `./gradlew exportTagRegistry` in a JManhunt checkout when the tag, trigger, or material tables change; registries without the newer arrays skip those checks silently)

### Links

- [Releases (CLI jars)](https://github.com/jruk8/JMHScript-Validator/releases)
- [JManhunt](https://github.com/jruk8/JManhunt)
- [JMHScript DSL docs](https://jruk8.github.io/JManhunt/configuration/modifiers/)
- [Discord and Community Mods](https://discord.com/invite/hkWmCVmWDC)
- [Error catalog](docs/error-catalog.md)

# For Developers

## Layout

- `src/main/.../core/`: the checking core. `model/` (Provenance, Diagnostic, codes, behavior model), `ports/` (TagRegistry, ShareMaterials), `script/` (moved validators), `share/` (share-string codec), `JmhValidator` (public sweep entry). Core imports `java.*` plus fellow core types only (enforced by `ArchTest`).
- `src/main/.../adapters/`: `cli/` (Main, versioned JSON/Text reports), `files/` (modifier YAML plus registry JSON loading).
- `contract/`: the versioned JSON report schema plus a Python subprocess example (no Python port; the jar is the truth).
- `corpus/`: frozen inputs plus goldens from unmodified engine code (see `corpus/README.md`).
- `docs/error-catalog.md`: stable diagnostic codes (pinned by a test).

## Single sources of truth

- Tag list: owned by the engine. The core takes a `TagRegistry` port; JManhunt implements it from live `knownTags`, and the CLI reads the exported `tag-registry.json`. The core never hardcodes tag names.
- Parser: owned by this module (`TagHeads`, `TagLists`, `BracketCheck`). The engine's load path and the validator both call it; no duplicate tag-splitting lives elsewhere.
- Share strings: owned by this module (`ShareCodec`, `ShareFraming`, `ShareJson`). The engine's import and export commands delegate to it; no share logic lives elsewhere. Material validity arrives through the `ShareMaterials` port.
- Messages: byte-identical to the engine, pinned by the corpus plus ported unit tests. Codes are new stable metadata; consumers map codes, never message text.

## Build and test

Requires JDK 25 (Temurin):

```sh
./gradlew build
```

`validator docs --registry tag-registry.json` renders tag-list markdown for the reference docs.

## Releases (fixed order)

1. Tag the validator (`v*`); the release workflow uploads the CLI jar. Refresh `tag-registry.json` in JManhunt first when the tag table changed.
2. JManhunt bumps its validator version.

## Consuming

- JManhunt: JitPack `com.github.jruk8:JMHScript-Validator:<tag>` plus a conditional `includeBuild` for local cross-repo work.

## License

Source code is AGPL-3.0-or-later (see [LICENSE](LICENSE)). Banner and media assets are all rights reserved (see [LICENSE-MEDIA](LICENSE-MEDIA)).
