# Troubleshooting and FAQ

**No game ever starts by itself.**
- `time_minutes` is `-1`, or the rotation was turned off with `/chatgames toggleAll`.
- Fewer than `min_players_online` players can play. With `count-all-players: false` only players
  with `chatgames.play` count, and vanished players never do.
- A game event is running (the rotation waits), or every game has `enable: false` or `weight: 0`.
- A preset is active whose games are all races, and `enable-races` is off.

**A player can't win.**
- They're in a world in `disabled-worlds`, or they turned games off (`/chatgames toggle`).
- `use-permission-toplay: true` and they lack `chatgames.play`.
- `case-sensitive: true` and the capitals are wrong (they get the `wrong_case` message).
- They answered faster than `min-answer-ms`: the console says so. Lower it if real players hit it.
- They joined a moment ago: their data loads in the background first, which takes a moment.

**Rewards don't arrive.**
- Check the console: an unknown reward action or item name is logged there.
- A line ending in `has:<permission>` only runs for players with that permission.
- With `chance`/`%~`, the draw can pick nothing when the chances add up to less than 100.
- For races, the winner only gets rewards listed for exactly that target and amount.

**The plugin disables itself on start.** The database couldn't be reached. The line before it
in the console says why: check `Storage` and the `MySQL` section in config.yml. Table names
may only use letters, digits and underscores.

**A race target is skipped.** It doesn't exist on your server version (a 1.21 block on 1.8). The
console names it. Modern and legacy names both work.

**Discord posts nothing.**
- `enabled: true` must be set at the top of discord.yml and in the section.
- The section's `channels` must name a channel that has a webhook or a DiscordSRV channel.
- Run `/chatgames discord test all`: errors (an invalid webhook, a message Discord refuses) go to
  the console.

**Placeholders show nothing.** PlaceholderAPI must be installed. Leaderboard placeholders refresh
every `placeholders.refresh-seconds`; an empty place shows `placeholders.empty-value`.

**I changed a file and nothing happened.** Run `/chatgames reload`. If a file has a YAML mistake,
the console says which file and line.

**Where are the logs?** Every game is logged to the console and `game_log.txt` (see
`add-ons.yml`). `/chatgames logs [amount]` shows the latest in game.

**Does it work on Folia?** Yes, the same jar runs on Spigot, Paper and Folia.

**Can I run it on several servers with one leaderboard?** Point every server at the same MySQL
database. Keep a player on one server at a time, since each server holds the player's data
while they're online.
