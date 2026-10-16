# Corpus: characterization goldens for the validator move

`inputs/` holds frozen modifier snapshots; `goldens/` holds the
expected reports the moved core must reproduce. Goldens were
generated from unmodified JManhunt code, so they pin behavior,
not intent: a golden mismatch after a move is a fidelity bug
until proven otherwise.

## Sources (pinned)

- `inputs/shipped/`: 20 files from JManhunt
  `src/main/resources/mods/modifiers/` at rev `10e2b55`.
- `inputs/crafted/sweep-coverage.yml`: hand-built lines, one
  per check shape, covering every sweep code.

Presets carry no behavior lines and are excluded. The registry
used at generation is `src/test/resources/tag-registry.json`
(258 tags and 25 triggers: the tag table frozen at rev
`10e2b55` plus the live trigger table with `ON_CLEANUP`,
`engineVersion` still pinned at `rev-10e2b55`, plus 2154
materials from the current export). Shipped inputs stay frozen
at `10e2b55` even as the engine converts its own copies, so
the three retired-list inputs now pin the `W_UNKNOWN_LIST`
warnings those lists produce, and every shipped input pins
the `W_UNKNOWN_KEY` warning for the retired `meta.icon`.

## Coverage

The line-sweep codes appear at least once: `E_SYNTAX`,
`E_BRACKET_UNMATCHED_CLOSE`, `E_BRACKET_UNCLOSED_OPEN`,
`W_UNKNOWN_TAG`, `W_REMOVED_TAG`, `W_RANDOM_PICK_ITEM`, `W_ALL_PLAYERS_FILTER`,
`W_SETTINGS_FOLDERS`, `W_SETTINGS_STRAY_LEAF`,
`W_SETTINGS_LEAFS`. Editor-only codes (`E_UNKNOWN_ROOT`,
`E_UNKNOWN_ITEM`) are pinned by the ported
`CommandValidationTest` instead. The file-set codes
(`W_DUPLICATE_ID`, `W_DUPLICATE_SETTING`, `W_UNKNOWN_SETTING`,
`W_UNKNOWN_TRIGGER`,
`W_UNKNOWN_SETTING_TYPE`, `W_DEFAULT_OUTSIDE_ALLOWED`,
`W_VALUE_INVALID`, `W_DUPLICATE_KEY`, `W_SHADOWED_ID`,
`W_UNKNOWN_BEHAVIOR`, `W_BEHAVIOR_CYCLE`,
`W_AMBIGUOUS_SETTING`, `W_UNCALLED_SETTING`,
`W_OPTION_VALUE`, `W_SHAPE`) stay silent on every corpus
input by design; their pins live in the core unit tests
and the CLI tests. `W_UNCALLED_BEHAVIOR` fires once, on
the crafted input's intentionally dead block.
`W_UNKNOWN_KEY` fires once on every shipped input by
design: frozen files carry the retired top-level
`meta.icon`, so the goldens pin the nudge to drop it.
`W_REDUNDANT_VALUE` fires on the shipped inputs by
design: frozen files carry the old value-plus-default
convention, so the goldens pin the nudge to omit the key.
`E_VALUE_DIFFERS` stays silent (no shipped fixture carries
an override); its pins live in the core unit tests.
`W_UNKNOWN_LIST` fires once each on the three frozen
retired-list inputs (`perma-night`, `get-stronger-on-kill`,
`speedrunner-health-advantage`).

## Refresh procedure (deliberate behavior changes only)

1. In JManhunt, refresh `src/test/resources/tag-registry.json`
   from live `knownTags` when the tag table changed, and copy
   fresh `triggers` and `materials` arrays from an
   `exportTagRegistry` run when those tables changed.
2. Delete `corpus/goldens/`, run
   `./gradlew test --tests "*CorpusGoldenWriterTest" --rerun-tasks`
   in a JManhunt checkout beside this repo.
3. Review the golden diff line by line; every hunk must trace
   to the intended change.
4. Commit inputs, goldens, and this README in one change.
