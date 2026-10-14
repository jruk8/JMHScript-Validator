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
- `W_DUPLICATE_ID`: Duplicate id. A later same-id file in the
  validated set; the first in engine load order wins and the
  engine skips the rest unparsed. The CLI still sweeps skipped
  files and reports their own findings too. Modifiers and
  presets are separate namespaces; only `.yml` files take part
  and unreadable, unparseable, and misshapen files never occupy
  an id. Example: `Duplicate modifier id 'fog': 'mods/fog.yml'
  wins, skipping 'mods/deep/fog.yml'.`.
- `W_DUPLICATE_SETTING`: Duplicate setting names. Two sibling
  settings share one name, so the engine drops the whole
  `config.settings` block. The level warning names the dotted
  folder path and the drop warning follows; later levels stay
  silent, and style findings are suppressed for the dropped
  block. Example: `Modifier 'fog' has duplicate setting names
  at 'targeting'.`.
- `W_UNKNOWN_SETTING`: Unknown setting. A `<setting:path>` (or
  `<s:path>`) reference names no defined leaf setting; missing
  paths and folder paths both read null at runtime. Blank,
  dynamic (tag-holding), and fatally broken lines stay silent
  for other checks to own. References in a duplicate-dropped
  block warn, since the drop empties the settings. Example:
  `Unknown setting 'base-y', resolves to null at runtime.`.
- `W_UNKNOWN_TRIGGER`: Unknown trigger. A `runs-on` entry names
  no engine-dispatched event; it never matches, so the block
  never fires on it. Matching trims and ignores case. The
  check stays silent when the registry predates the triggers
  array. Example: `Unknown trigger 'ULTIMATE_INTERVAL', it
  never fires.`.
- `W_UNKNOWN_KEY`: Unknown key. A YAML key names nothing the
  engine reads, so it is silently ignored. Checked at the
  file root, `meta`, `config`, behavior blocks with their
  option subtrees, and the settings node shape. Command list
  names, behavior ids, and setting entry names stay open and
  are never flagged. Levels that fail their shape stay silent
  here. Example: `Unknown key 'conflg' in the file root;
  ignoring it.`.
- `W_UNKNOWN_SETTING_TYPE`: Unknown setting type. A setting
  spec names no known setting type; the engine skips clamping
  for it. The type reads upper-cased. Example: `Modifier
  'fog' setting 'rate' has unknown type 'INTEGERR'.`.
- `W_DEFAULT_OUTSIDE_ALLOWED`: Default outside allowed. A
  setting default falls outside its allowed range (or is
  missing, which fills the zero value), so the engine clamps
  it at load. Material names validate only when the registry
  carries the materials array. Example: `Modifier 'fog'
  setting 'rate' default is outside allowed; clamped.`.
- `W_VALUE_INVALID`: Value invalid. A live setting value
  falls outside its type or allowed range, so the engine
  resets it at load. Absent values fill from the default
  silently. Example: `Modifier 'fog' setting 'rate' value
  is invalid; reset to default.`.
- `W_DUPLICATE_KEY`: Duplicate YAML key. One mapping repeats
  a key, so the loader keeps the last value and drops the
  earlier ones. Anchors with merge-plus-override never match
  (the override is explicit), and the share round-trip plus
  the engine-defaults manifest are outside this check.
  Example: `Duplicate YAML key 'config' in the file root
  (line 12); keeping the last.`.
- `W_SHADOWED_ID`: Shadowed bundled id. A scanned file reuses
  an engine-bundled id of the same namespace without being
  the pristine bundled copy, so where both files are present
  the first in load order wins. Needs `--defaults`: files
  byte-identical to the fetched bundled copy stay silent, a
  bundled file missing from the defaults dir still warns,
  and runs without `--defaults` skip the check. Example:
  `Custom modifier 'gear-dice' shares an id with the
  engine-bundled copy; where both files are present the first
  in load order wins.`.
- `W_UNKNOWN_BEHAVIOR`: Unknown behavior. A `<runblock:>`
  names no defined block of the same modifier, so the engine
  skips the hop at runtime (once per match). Blank, dynamic,
  and malformed ids stay silent here (the syntax sweep owns
  the line). Example: `Tag <runblock> hit unknown behavior
  'nope'; skipping.`.
- `W_BEHAVIOR_CYCLE`: Behavior cycle. A `<runblock:>` chain
  recurses into its own call path, so the engine skips the
  re-entering hop at runtime (once per match). Each closing
  edge reports once. Example: `Tag <runblock> hit cyclic
  chain at behavior 'main'; skipping.`.
- `W_AMBIGUOUS_SETTING`: Ambiguous setting. A setting defines
  both a live value and a folder, so the engine uses the
  value and drops the folder (dropped children validate
  nowhere). Example: `Modifier 'fog' setting 'thing'
  defines both value and folder; using the value.`.
- `W_UNCALLED_SETTING`: Uncalled setting. A leaf setting no
  behavior line reads, which usually means its call site has
  a typo. Reads resolve like the reference check against the
  post-drop tree; files with dynamic ids skip the sweep, and
  GUI-only settings still warn (GUI reads are invisible
  here). Example: `Setting 'rate' is never read.`.
- `W_UNCALLED_BEHAVIOR`: Uncalled behavior. A block with no
  `runs-on` trigger and no `<runblock>` call, so it never
  runs. Triggered blocks and called-only targets pass;
  files with dynamic ids skip the sweep. Example:
  `Behavior 'side' is never called.`.

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
