# Troubleshooting

### `IllegalStateException: ChatGames isn't enabled`

`ChatGamesAPI.get()` was called before ChatGames enabled, or without it on the server.

- Add `depend: [ChatGames-Premium]` (or `softdepend`) to plugin.yml. The name must be exactly
  `ChatGames-Premium`.
- Call the API from `onEnable` or later, never from a constructor, a static initialiser or `onLoad`.
- Check that the console shows ChatGames enabling without errors.

### `NoClassDefFoundError: org/nsider/chatgames/api/...`

ChatGames isn't on the server, and your plugin uses the API in a class that loaded anyway. With
`softdepend`, keep every API call in a separate class that you only touch after checking
`isPluginEnabled("ChatGames-Premium")` ([Getting Started](Getting-Started#optional-support-softdepend)).

### Strange errors, or my game "doesn't exist" for ChatGames

You probably **shaded** the API into your jar. Your plugin then has its own copy of `GameType`, and
ChatGames never sees your registrations. Use `provided` (Maven) or `compileOnly` (Gradle), and
check that your jar has no `org/nsider/chatgames` folder.

### `IllegalArgumentException` when registering

- **"is a /chatgames command"**: the id is a sub-command like `top`, `menu` or `stop`. Pick another.
- **"already ... exists" / "already taken"**: another plugin (or a built-in game) uses that id, or
  your plugin registered it twice. On `/reload` your old registrations are removed first, so this
  is usually two registrations in one `onEnable`.
- **"must be 2-32 lower-case letters..."**: ids use `a-z`, `0-9` and `_`, start with a letter, and
  are 2 to 32 characters long.

### My game never runs in the rotation

- Is `enable: true` in its games.yml (or races.yml) section? Owners can turn it off.
- Is `weight` above 0?
- Is a **preset** active (presets.yml `active:`)? The rotation then only picks the preset's games.
- Races only run when `enable-races: true` is set at the top of races.yml.
- `/chatgames <id>` starts it right away, which helps you test the game alone.

### The game starts but nobody can win

- Check the answers: players type them, so they must be plain text without colour codes.
  Spaces and capitals are ignored, accents aren't.
- `case-sensitive: true` in games.yml makes capitals matter.
- Answers faster than `min-answer-ms` (400 ms) are ignored as bots. Test like a player would.
- For a click-to-answer game, the click command must be exactly `"/chatgames " + answer`
  ([Hover and click](Chat-Games#hover-and-click)).

### My race never counts

- Keep the `GameType` that `registerRace` returned and pass that one.
- The target you pass must match the running race's target (case doesn't matter), or be `null`.
- Players in creative don't count unless `creative-enabled: true`, and players in disabled worlds
  or with games turned off never count.
- Use `/chatgames <race> <target> <amount>` to start the exact race you are testing.

### "Unknown reward action" in the console

A reward line uses an action no plugin registered: a typo in the owner's file, or your plugin
isn't enabled. Names are case-insensitive, and written with brackets: `[coins] 100`.

### Console warnings about my game at startup

ChatGames holds back "unknown game" and "unknown type" warnings until the server has finished
starting, because your plugin enables after it. Warnings **after** startup mean the name really
doesn't exist: a typo in a config, or your plugin failed to enable.

### My generator's exception is in the console

When a generator throws, the round doesn't start, and the message is logged. Throw
`IllegalStateException` with a message the owner can act on, like
`"no questions in games.yml capitals.questions"`.

### Still stuck?

Compare with the [example plugin](https://github.com/nsider4/ChatGames-API/tree/main/example), which
is tested on Spigot 1.8.8, Paper 1.21 and Folia, or open an issue on the
[ChatGames-API repository](https://github.com/nsider4/ChatGames-API/issues).
