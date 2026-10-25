# Discord

ChatGames posts games, races, global games, events, streaks, challenges and leaderboards to
Discord. Everything is in `discord.yml`.

## Connecting

Messages go to **channels** you name yourself. Each channel posts through a webhook, through the
[DiscordSRV](https://www.spigotmc.org/resources/18494/) bot, or both:

```yaml
enabled: true
channels:
  main:
    webhook: 'https://discord.com/api/webhooks/...'
    discordsrv: ''
    username: 'Chat Games'
    avatar: 'https://...'
```

- **Webhook** (nothing to install): in Discord, *Edit Channel → Integrations → Webhooks → New
  Webhook → Copy Webhook URL*. Keep it secret: anyone with the URL can post in that channel.
- **DiscordSRV:** a channel name from the `Channels` section of DiscordSRV's config.yml (e.g.
  `global`), or a channel ID. With DiscordSRV, `%winner_mention%` shows a player's linked Discord
  account, and `ping-linked-players: true` also pings them.

Then try every message with `/chatgames discord test all`, or one with
`/chatgames discord test <message>`. They're posted with sample values.

## What gets posted

| Section | Posted when | Off by default |
|---|---|---|
| `games` | a chat game starts and is won (or runs out) | |
| `races` | a race starts and ends | |
| `global` | a global game starts, progresses and ends | |
| `events` | a game event starts and ends | |
| `streaks` | a player reaches one of `days: [7, 14, 30, ...]` | yes |
| `challenges` | a player completes a challenge | yes |
| `leaderboard` | at the `times:` you set, and on `/chatgames discord leaderboard` | yes |

Each section has `enabled` and `channels` (the channel names it posts to). `ignored-games` lists
games that are never posted.

**Live posts:** with `live: true`, a game is posted when it starts and the same post turns into
the result. A global game's post shows its progress bar while it runs. `<t:%ends_unix%:R>` shows
a countdown in every reader's own time. Memorize never shows its list (players could read it
there).

## Editing messages

Every message is an embed you can change: colour, author, title, description, fields,
thumbnail, image, footer and timestamp. Placeholders (and PlaceholderAPI ones) work, and emoji
are written as `:trophy:` (the names you can use are at the end of discord.yml).

```yaml
games:
  win:
    embed:
      color: '%game_color%'
      author: '%game_emoji%  %game%'
      title: ':trophy:  %winner% got it!'
      thumbnail: '%winner_head%'
```

- `styles` gives each game its `%game_color%` and `%game_emoji%`.
- `lists` sets how winners, contributors and leaderboards are written.
- `player-head-url` is the picture for `%winner_head%` / `%player_head%`.
- `server-name` is `%server%`.

Don't paste emoji characters into discord.yml: 1.8 servers can't read a file that has them. Use
the `:name:` form, or a server emoji as `<:name:ID>`.

## Pings

Nothing pings except the role IDs in `allowed-role-pings` (write `<@&ID>` in a text). Never
`@everyone`, and never anything in a player's name or answer. To get a role ID, turn on Developer
Mode in Discord (*Settings → Advanced*), then right-click the role.
