# Error catalog

Stable diagnostic codes for the JSON contract. Codes never
change meaning; new checks add codes, retired checks keep
theirs reserved. Consumers map codes, never message text.
Message text stays byte-identical to the engine.

## Fatal errors

- `E_BRACKET_UNMATCHED_CLOSE`: Unmatched angle bracket. A `>`
  with no opener. Example: `say done>>`.
- `E_BRACKET_UNCLOSED_OPEN`: Unclosed angle bracket. A `<`
  with no matching close. Example: `say <count`.
- `E_SYNTAX`: Malformed tag arguments. Shape, arity, quote,
  and type failures. Example: `Tag <min> needs two numbers.`
- `E_UNKNOWN_ROOT`: Unknown command. The first token names no
  known command (editor checks only).
- `E_UNKNOWN_ITEM`: Unknown item. A give-shape item names no
  known material (editor checks only).

## Warnings

- `W_UNKNOWN_TAG`: Unknown tag. Outside the registry and def
  scope; the engine leaves it untouched at runtime.
- `W_RANDOM_PICK_ITEM`: Skipped pick item. A random-pick entry
  the runtime skips.
- `W_ALL_PLAYERS_FILTER`: Weak filter. An all-players filter
  outside letters-only.
- `W_UNKNOWN_MODIFIER`: Unknown modifier. A preset member names
  no known modifier id (fetched engine defaults plus scanned
  modifiers). The engine drops the member at load and keeps the
  file list, so a later reload picks it up once its file exists.
  Example: `Preset 'horror-manhunt' references unknown modifier
  'fog'; skipping.`.
- `W_SETTINGS_FOLDERS`: Too many folders. A settings level
  holds more than 5 folders. Example: `10 folders (max 5):
  targeting, rules, ...`.
- `W_SETTINGS_STRAY_LEAF`: Misplaced settings leaf. A leaf
  beside folders that is not `enabled` (matched
  case-insensitively, `-` and `_` unified), or a bare leaf at
  a folderless settings root. Example: `stray leaf 'sounds'
  beside 2 folder(s); only 'enabled' may sit beside folders`.
- `W_SETTINGS_LEAFS`: Too many leafs. A settings level
  holds more than 7 leafs. Example: `9 leafs (max 7):
  message, sound, ...`.

Settings style findings carry the dotted settings path in
the `list` field with an empty `behavior`, line 0, and a
null char; they are CLI-only, the engine load sweep stays
silent on style. Preset findings carry `modifiers` in the
`list` field with an empty `behavior`; `line` is the 1-based
member position and char is null. They appear only when the
CLI runs with fetched engine defaults (see `--defaults`).

## File diagnostics

- `E_FILE_READ`: Unreadable file. The CLI cannot read the path.
- `E_YAML_PARSE`: Unparseable YAML. The file is not valid YAML.
- `E_YAML_SHAPE`: Unexpected YAML shape. A behavior section
  with the wrong shape (map or list expected).
- `E_YAML_LINE_TYPE`: Non-string command line. A behavior
  command entry that is not a string.
