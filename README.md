<div align="center">

![JMHScript-Validator banner](docs/assets/validator-banner-1280x640.png)

<h3>Lightweight CLI validator for JMHScript syntax.</h3>

### [Releases](https://github.com/jruk8/JMHScript-Validator/releases) · [JMHScript DSL](https://jruk8.github.io/JManhunt/configuration/modifiers/) · [Community Mods](https://discord.com/invite/hkWmCVmWDC)

<br>

</div>

# Quick Start 🫈

1. Download the CLI jar and its `tag-registry.json` from [Releases](https://github.com/jruk8/JMHScript-Validator/releases) (one release, both files; the registry carries the engine version its tags came from).
2. Point it at your mods folder (single files work too):

```sh
java -jar JMHScript-Validator-<version>-cli.jar --registry tag-registry.json mods/
```

**That's it!** Exit 0 means clean. Anything else prints findings per file, each naming the behavior, list, line, and what the engine would do with it at runtime.

# Why Validate?

The modifier GUI checks commands on save, but hand-written or imported mods skip that check, so at runtime a broken line only warns in the console instead of running. The validator runs those same checks offline, with message text byte-identical to the engine, so what you see here is what the server would say there.

# Features

- **Broken syntax** » Unmatched brackets and malformed tag arguments, with the engine's own messages.
- **Unknown tags and triggers** » Tags outside your engine version's table, plus `runs-on` entries that never fire.
- **Dangling refs** » `<setting:>` and `<runblock:>` calls naming nothing, and lists no dispatch key reads.
- **Bad values** » Behavior options, setting defaults, and live values the engine clamps or resets silently.
- **Preset and id checks** » Unknown preset members, duplicate ids, and bundled-id shadows (needs `--defaults`).
- **Bad files** » Unreadable paths, unparseable YAML, key typos, duplicate YAML keys, and wrong shapes.
- **Sharing** » `export` mods to share strings and `import` them back, no registry needed.

Every finding carries a stable code (see the [error catalog](docs/error-catalog.md)). Full CLI details (JSON reports, exit codes, presets, sharing) live in [docs/usage.md](docs/usage.md).

# Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the build, layout, and release guidelines.

### Requirements

- Java Version » **Java 25**
- Tag table » **`tag-registry.json` from the matching validator release**

### Links

- [Releases (CLI jars)](https://github.com/jruk8/JMHScript-Validator/releases)
- [JManhunt](https://github.com/jruk8/JManhunt)
- [JMHScript DSL docs](https://jruk8.github.io/JManhunt/configuration/modifiers/)
- [Discord and Community Mods](https://discord.com/invite/hkWmCVmWDC)

## License

Source code is AGPL-3.0-or-later (see [LICENSE](LICENSE)). Banner and media assets are all rights reserved (see [LICENSE-MEDIA](LICENSE-MEDIA)).
