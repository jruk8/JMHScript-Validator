# Contributing to JMHScript-Validator

Thank you for your interest in contributing to
JMHScript-Validator! This document covers the layout, build,
single sources of truth, versioning, and pull request
guidelines.

## Layout

- `src/main/.../core/`: the checking core. `model/`
  (Provenance, Diagnostic, codes, behavior model), `ports/`
  (TagRegistry, ShareMaterials), `script/` (moved validators),
  `share/` (share-string codec), `JmhValidator` (public sweep
  entry). Core imports `java.*` plus fellow core types only
  (enforced by `ArchTest`).
- `src/main/.../adapters/`: `cli/` (Main, versioned JSON/Text
  reports), `files/` (modifier YAML plus registry JSON
  loading).
- `contract/`: the versioned JSON report schema plus a Python
  subprocess example (no Python port; the jar is the truth).
- `corpus/`: frozen inputs plus goldens from unmodified engine
  code (see `corpus/README.md`).
- `docs/error-catalog.md`: stable diagnostic codes (pinned by a
  test).

## Single Sources of Truth

- Tag list: owned by the engine. The core takes a `TagRegistry`
  port; JManhunt implements it from live `knownTags`, and the
  CLI reads the exported `tag-registry.json`. The core never
  hardcodes tag names.
- Parser: owned by this module (`TagHeads`, `TagLists`,
  `BracketCheck`). The engine's load path and the validator both
  call it; no duplicate tag-splitting lives elsewhere.
- Share strings: owned by this module (`ShareCodec`,
  `ShareFraming`, `ShareJson`). The engine's import and export
  commands delegate to it; no share logic lives elsewhere.
  Material validity arrives through the `ShareMaterials` port.
- Messages: byte-identical to the engine, pinned by the corpus
  plus ported unit tests. Codes are new stable metadata;
  consumers map codes, never message text.

## Build and Test

Requires JDK 25 (Temurin):

```sh
./gradlew build
```

Tests need no server or network. The corpus parity test runs
from vendored inputs and goldens only.

`validator docs --registry tag-registry.json` renders tag-list
markdown for the reference docs.

## Local Development

JManhunt consumes the validator through JitPack
(`com.github.jruk8:JMHScript-Validator:<tag>`). For local
cross-repo work, it uses a conditional `includeBuild`.

## Versioning and Releases

Releases run in a fixed order:

1. Tag the validator (`v*`); the release workflow uploads the
   CLI jar. Refresh `tag-registry.json` in JManhunt first when
   the tag table changed.
2. JManhunt bumps its validator version.

## Coding Conventions

- **Java 25** is the target, like the engine. Match the
  JManhunt style config in `config/checkstyle/`.
- **Checkstyle** is enforced. Run `./gradlew build` (tests plus
  checkstyle) before submitting.
- **Core stays dependency-free:** `core/` imports `java.*`
  plus fellow core types only. Adapters own SnakeYAML; the
  engine owns Bukkit/Okaeri.
- **Behavior stays 1:1.** Message text is byte-identical to the
  engine; codes are additive metadata. A corpus mismatch is a
  fidelity bug until proven otherwise.
- **No em dashes** in comments or docs.

## Testing Expectations

- **Logic-based tests** are required for new behavior. Port the
  engine's unit tests for moved checks.
- Behavior changes regen corpus goldens per `corpus/README.md`;
  review every hunk.
- New codes update `docs/error-catalog.md` (the catalog test
  enforces it). Format changes bump `reportVersion`.

## Commit Message Style

Use [Conventional Commits](https://www.conventionalcommits.org/)
with `git commit -s`:

```
<type>[optional scope]: <description>

[optional body]
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.
Describe the change only: no `Phase N`, agent, session, or other
process remarks in commit messages. Never push unless explicitly
requested.

## Pull Request Guidelines

1. **Branch** from `main` with a descriptive name (e.g.
   `feat/new-check`). Never commit directly on `main`.
2. **Commit** using the style above. Squash fixup commits before
   submitting.
3. **Build**: ensure `./gradlew build` passes with zero
   warnings.
4. **Describe**: write a clear PR description explaining what
   and why.
5. **Review**: respond to feedback promptly. CI must pass before
   merge.

## Code of Conduct

Be respectful and constructive in all interactions. Harassment or
discriminatory behavior of any kind will not be tolerated.
