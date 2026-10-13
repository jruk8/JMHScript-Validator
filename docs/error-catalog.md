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

## File diagnostics

- `E_FILE_READ`: Unreadable file. The CLI cannot read the path.
- `E_YAML_PARSE`: Unparseable YAML. The file is not valid YAML.
- `E_YAML_SHAPE`: Unexpected YAML shape. A behavior section
  with the wrong shape (map or list expected).
- `E_YAML_LINE_TYPE`: Non-string command line. A behavior
  command entry that is not a string.
