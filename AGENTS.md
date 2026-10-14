# AGENTS.md: guide for AI agents working on JMHScript-Validator

Standalone JMHScript validation core plus CLI, consumed by
JManhunt (Gradle library) and by mod authors (CLI over files).
Behavior stays 1:1 with the engine; the corpus proves it.

## 1. Orientation

| Read | Why |
|---|---|
| `README.md` | Layout, SSOT rules, releases |
| `corpus/README.md` | Golden provenance and refresh |
| `docs/error-catalog.md` | Stable codes |
| `contract/report-schema.json` | JSON shape consumers parse |

## 2. Golden rules

- Core (`core/`) imports `java.*` plus fellow core types only.
  Adapters own SnakeYAML; the engine owns Bukkit/Okaeri.
  `ArchTest` enforces this.
- Behavior moves stay 1:1. Message text is byte-identical to
  the engine; codes are additive metadata. A corpus mismatch
  is a fidelity bug until proven otherwise.
- The core never hardcodes tag names. New checks take the
  `TagRegistry` port; the tag list lives engine-side.
- No em dashes in comments or docs. Conventional Commits with
  `git commit -s`. Never push unless explicitly requested.

## 3. Gates

- `./gradlew build` (tests plus checkstyle) is the gate.
  Match the JManhunt style config in `config/checkstyle/`.
- Java 25 toolchain with release 25, like the engine.
- Tests need no server or network. The corpus parity test
  runs from vendored inputs and goldens only.

## 4. Changing behavior (deliberate only)

1. Update the core plus the ported unit tests.
2. Regen corpus goldens per `corpus/README.md` and review
   every hunk.
3. Update `docs/error-catalog.md` for new codes (the catalog
   test enforces it) and bump `reportVersion` for format
   changes.
4. Follow the README release order (validator, JManhunt).
