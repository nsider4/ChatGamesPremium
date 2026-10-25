# Installation

## Requirements

- A Spigot, Paper (or fork) server from **1.8.8 to 26.x**, or **Folia**.
- **Java 8** or newer, or whatever your server version needs (Java 25 for 26.x).
- Optional: [PlaceholderAPI](https://www.spigotmc.org/resources/6245/) for placeholders,
  [DiscordSRV](https://www.spigotmc.org/resources/18494/) to post through your Discord bot,
  [MythicMobs](https://www.spigotmc.org/resources/5702/) for Hunt races on custom mobs.

## Installing

1. Put `ChatGames-Premium-2.0.0.jar` in your `plugins/` folder.
2. Start (or restart) the server. Don't use `/reload` or PlugMan: restart.
3. The files appear in `plugins/ChatGames-Premium/`. A game starts by itself after
   `time_minutes` (25 by default) when at least `min_players_online` players are on.
4. Try it right away: `/chatgames unscramble` starts a game now.

On first start the plugin downloads the item, block and mob names for your `lang` into
`translations/` (see [Languages](Languages)).

## Storage: SQLite or MySQL

Player data (points, wins, times, challenge progress, streaks) is saved in a database.

**SQLite** (the default) needs no setup: the data goes in `chatgames-data.db` in the plugin folder.

**MySQL / MariaDB**, for sharing data between servers or for large servers:

```yaml
Storage: 'MySQL'
MySQL:
  host: 'localhost'
  port: '3306'
  user: 'root'
  password: 'your-password'
  database: 'chatgame'
  connection-parameters: '?autoReconnect=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8'
```

The database must exist; the tables are created on start. `table_name` and
`fastest_times_table` name the tables. They may only use letters, digits and underscores. If the
connection fails, the console says why and the plugin disables itself, so check the host,
port and password.

Player data is kept in memory while players are online and saved in the background (when they
leave, every 5 minutes, and on shutdown), so the database is never in the way of the game.

## Upgrading from 1.9.9

Drop the new jar in place of the old one and restart. On the first start:

- **Configs are upgraded.** The old files are copied to `backups/1.9.9-<date>/` first. Every
  value you changed is moved to where it lives now (game settings to `games.yml`, Discord to
  `discord.yml`, logs to `add-ons.yml`, `chatRaces.yml` becomes `races.yml`, Shopping List
  becomes Memorize). Everything you didn't change gets the 2.0.0 default.
- **Player data is upgraded.** Points, toggles and sound settings are kept, and each player's
  best time still counts for the overall fastest leaderboard. The old table is kept, renamed to
  `chatgames_1_9_9`.
- An edited `rewards.yml` is kept as it is; the new games get default rewards.
- Every 1.9.9 placeholder still works.

## First things to set

| Setting | File | Why |
|---|---|---|
| `lang` | config.yml | The language for item/block/mob names, and (with `config-language: auto`) all config files |
| `time_minutes` or `randomDelay` | config.yml | How often games start |
| `min_players_online` | config.yml | Players needed for a game to start by itself |
| `disabled-worlds` | config.yml | Worlds where nobody can play |
| `enable` per game | games.yml / races.yml | Which games run |
| The rewards | rewards.yml | The defaults give iron blocks; change them to your economy |
| `prefix` | config.yml | The `%prefix%` in messages |

Then run `/chatgames reload`.
