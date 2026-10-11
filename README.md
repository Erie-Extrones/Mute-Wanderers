# RankMute

Client-side [Fabric](https://fabricmc.net/) mod that hides chat messages from players whose rank prefix matches a configured list. Nothing is sent to the server; the mod only decides whether a received message gets drawn in the chat HUD.

Supported Minecraft versions: **26.3** and **26.1.2** (one jar per version). Requires Java 25, Fabric Loader 0.19.5+ and Fabric API.

## Install

1. Install Fabric Loader 0.19.5 or newer for your Minecraft version.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) for that Minecraft version in `mods/`.
3. Download the RankMute jar whose name ends in `+mc<your version>.jar` from the [Releases](../../releases) page and put it in `mods/`.
4. Start the game. A default config is written to `config/rankmute.json` on first launch.

Server-side nothing changes. The mod is marked `"environment": "client"` and is ignored on servers.

## Important: match the server's chat format

Rank rules are built from a **prefix template**. The default, `^[^A-Za-z0-9]*%s\s*:`, matches lines that start with the rank name followed by a colon, allowing an icon glyph or spaces in front, e.g.

```
Wanderer : Meigha hello
```

If your server formats chat differently (`[Wanderer] Steve: hello`, `Wanderer | Steve » hello`, ...), the default rule will never match and nothing will be hidden. Set a template that fits, where `%s` stands for the rank name:

```
/rankmute template ^\[%s\]
```

Then verify with the test command, which runs a sample line through the matcher without sending anything:

```
/rankmute test [Wanderer] Steve: hello
```

Tips:

- Copy a real line from `.minecraft/logs/latest.log` (look for `[CHAT]`) so the format is exact, including `§` colour codes. Colour codes are stripped before matching.
- Rank rules are case-insensitive and only ever match at the **start** of the line, so a player merely typing `[Wanderer]` mid-sentence is not hidden.
- For formats a single template cannot express, add a raw regex rule instead (see below). Regex rules match anywhere in the line and are case-sensitive unless you prefix them with `(?i)`.

## Commands

All commands are client-only and never reach the server.

| Command | Effect |
|---------|--------|
| `/rankmute` | Show status: enabled, number of rules, messages hidden this session, current template. |
| `/rankmute toggle` | Enable or disable filtering. |
| `/rankmute add <rank>` | Hide messages from this rank. |
| `/rankmute remove <rank>` | Stop hiding this rank (case-insensitive). |
| `/rankmute list` | List hidden ranks and regex rules with their indices. |
| `/rankmute regex add <pattern>` | Add a raw Java regex rule. Invalid patterns are rejected. |
| `/rankmute regex remove <index>` | Remove a regex rule by the 1-based index shown in `list`. |
| `/rankmute template <pattern>` | Set the prefix template. Must contain `%s`. |
| `/rankmute reload` | Re-read `config/rankmute.json`. |
| `/rankmute test <text>` | Report whether `<text>` would be hidden, and by which rule. |

Every change prints a confirmation in chat and saves the config immediately.

## Config

File: `config/rankmute.json`. Written with defaults if missing; see [`config.example.json`](config.example.json).

```json
{
  "enabled": true,
  "prefixTemplate": "^[^A-Za-z0-9]*%s\\s*:",
  "showIfMentioned": true,
  "showCounterInActionBar": false,
  "debugLogEvents": false,
  "hiddenRanks": ["Wanderer"],
  "regexRules": []
}
```

| Key | Default | Meaning |
|-----|---------|---------|
| `enabled` | `true` | Master switch. |
| `prefixTemplate` | `^[^A-Za-z0-9]*%s\s*:` | Regex template for rank rules. `%s` is replaced with the (regex-escaped) rank name. Compiled case-insensitive and anchored to the start of the line. |
| `showIfMentioned` | `true` | Never hide a message that contains your own username (whole word, case-insensitive), even if a rule matches. |
| `showCounterInActionBar` | `false` | Briefly show `RankMute: hid N` on the action bar whenever a message is dropped. Handy while setting up. |
| `debugLogEvents` | `false` | Log each hidden message and whether it arrived as a `chat` (signed player chat) or `game` (system) message. Use this to confirm which event your server's chat goes through. |
| `hiddenRanks` | `["Wanderer"]` | Rank names to hide. |
| `regexRules` | `[]` | Raw Java regexes. A message is hidden if any rank rule or regex rule matches. |

`showIfMentioned`, `showCounterInActionBar` and `debugLogEvents` have no command; edit the file and run `/rankmute reload`.

If the file cannot be parsed, a warning is logged and the defaults are used in memory. The broken file is left untouched until the next command that changes settings overwrites it.

## How it works

- Hooks Fabric API's `ClientReceiveMessageEvents.ALLOW_CHAT` (signed player chat) and `ALLOW_GAME` (system messages). Most servers with rank plugins deliver player chat as system messages, so both are hooked. Action bar (overlay) text is never filtered.
- The message is flattened to plain text, `§` formatting codes are stripped, then the rules run in order: rank rules first, regex rules second.
- Only messages received after a rule is added are affected; nothing is removed retroactively.

Out of scope for now: filtering by player name (use vanilla Social Interactions), any server-side component, and a GUI config screen.

## Building from source

Requires JDK 25.

```
./gradlew build                                                       # Minecraft 26.3 (default in gradle.properties)
./gradlew build -Pminecraft_version=26.1.2 -Pfabric_api_version=0.155.3+26.1.2   # Minecraft 26.1.2
```

Jars land in `build/libs/` as `rankmute-<version>+mc<minecraft>.jar`. Unit tests for the matcher run as part of `build` and need no Minecraft installation.

For in-game testing, run a local server and fake rank-prefixed chat with:

```
/tellraw @a "Wanderer : test hello"
```

## Releasing

CI (`.github/workflows/build.yml`) builds and tests every push for all supported Minecraft versions.

To publish a release, bump `mod_version` in `gradle.properties`, commit, then push a matching tag:

```
git tag v1.0.0
git push origin v1.0.0
```

`.github/workflows/release.yml` builds one jar per Minecraft version with the version taken from the tag, and publishes a GitHub Release with the jars attached and generated release notes. Tags with a `-` suffix (e.g. `v1.1.0-beta.1`) are marked as pre-releases. To support another Minecraft version, add it to the matrix in `build.yml`.

## License

[MIT](LICENSE)
