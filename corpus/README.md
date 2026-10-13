# Corpus: characterization goldens for the validator move

`inputs/` holds frozen modifier snapshots; `goldens/` holds the
expected reports the moved core must reproduce. Goldens were
generated from unmodified JManhunt code, so they pin behavior,
not intent: a golden mismatch after a move is a fidelity bug
until proven otherwise.

## Sources (pinned)

- `inputs/shipped/`: 20 files from JManhunt
  `src/main/resources/mods/modifiers/` at rev `10e2b55`.
- `inputs/jmhmods/`: 15 files from JMH-Mods `mods/` at rev
  `5a906c4` (flattened; stems are unique).
- `inputs/crafted/sweep-coverage.yml`: hand-built lines, one
  per check shape, covering every sweep code.

Presets carry no behavior lines and are excluded. The registry
used at generation is `src/test/resources/tag-registry.json`
(228 tags, JManhunt rev `10e2b55`).

## Coverage

Every sweep code appears at least once: `E_SYNTAX`,
`E_BRACKET_UNMATCHED_CLOSE`, `E_BRACKET_UNCLOSED_OPEN`,
`W_UNKNOWN_TAG`, `W_RANDOM_PICK_ITEM`, `W_ALL_PLAYERS_FILTER`.
Editor-only codes (`E_UNKNOWN_ROOT`, `E_UNKNOWN_ITEM`) are pinned
by the ported `CommandValidationTest` instead. Real-content
findings at capture time (pre-existing, not caused by the move):

- `jmhmods/bounty`: 1 error (`Tag <if> mixes quotes.`).
- `jmhmods/cursed-aqua`: 1 unknown-tag warning (`phaseffect`,
  not an engine tag).
- `jmhmods/spyglass-sniper`: 3 unknown-tag warnings
  (`pisusinghanditem`, not an engine tag).

## Refresh procedure (deliberate behavior changes only)

1. In JManhunt, refresh `src/test/resources/tag-registry.json`
   from live `knownTags` when the table changed.
2. Delete `corpus/goldens/`, run
   `./gradlew test --tests "*CorpusGoldenWriterTest" --rerun-tasks`
   in a JManhunt checkout beside this repo.
3. Review the golden diff line by line; every hunk must trace
   to the intended change.
4. Commit inputs, goldens, and this README in one change.
