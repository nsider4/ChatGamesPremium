# ChatGames-Premium 2.0.0

A full recode. One jar runs on Spigot, Paper and their forks from 1.8.8 to 26.x, and on Folia.

## Upgrading from 1.9.9

Drop the new jar in and restart. On the first start:
- **Your configs are upgraded.** The old files are copied to `plugins/ChatGames-Premium/backups/1.9.9-<date>/` first. Every value you changed is carried over to where it lives now:
  - game settings move to `games.yml`
  - Discord and log settings move to `add-ons.yml`
  - `chatRaces.yml` becomes `races.yml`
  - Shopping List is now Memorize
  Everything you didn't change gets the 2.0.0 default, and the comments are kept.
- **Your player data is upgraded.** Points, toggles and sound settings are kept. Your best time still counts for the overall fastest leaderboard. The old table is kept as `chatgames_1_9_9`.
- An edited `rewards.yml` is kept as it is, and the new games get default rewards.

## New

- **Folia support**, in the same jar.
- **New games:**
  - **Sequence:** complete the number sequence.
  - **Word Association:** three clues, one hidden link.
  - **Memorize:** replaces Shopping List. It can show words, numbers, colours, items, mobs or symbols.
- **Hints** and an **action-bar countdown**, configured per game.
- **Global games:** the whole server works toward one goal on a shared progress bar, and everyone who helped is rewarded. Most chat games and every race can be played this way. The new **Count** game (counting together in chat) is global only.
- **Game events:** a named series of games with event points, a final leaderboard and rewards. Start one by command, from the menu or on a schedule.
- **Daily and weekly challenges**, plus **streaks** with a 7-day reward ladder.
- **Menus:** main, profile, challenges, streaks, leaderboards, admin, and a debug menu for testing games privately. Every item and action is configurable, and you can add your own menus.
- **Leaderboards:** the fastest time per game, and per-event leaderboards.
- **Scheduling:**
  - weighted rotation with no back-to-back repeats
  - a random delay that is re-rolled for every game
  - presets: named sets of enabled games
- **Anti-macro:** answers typed faster than a per-game minimum are ignored and logged.
- **Many new PlaceholderAPI placeholders** for wins, races, streaks, challenges, events and global games. Every 1.9.9 placeholder still works.
- **Translations:** item, block and mob names are now up to date with 26.x, and there are over 140 languages. Outdated files are re-downloaded automatically.
- **Translated config files:** set `lang` to Spanish, French, Portuguese, German, Italian, Dutch, Polish, Russian or Turkish and every config file switches to that language: messages, menus, word lists, trivia and comments. Your settings are kept and the old files are backed up first. `config-language` in config.yml can pick a language or turn this off.
- **MythicMobs:** Hunt races can target a MythicMobs mob with `mythic:<MobType>`. `count-mythic-mobs` in races.yml decides whether MythicMobs mobs also count as their vanilla type.
- **Custom sounds:** any sound from a resource pack (`namespace:sound`) works in game sounds and menus.
- **Developer API** ([ChatGames-API](https://github.com/nsider4/ChatGames-API) on JitPack). Plugins can add:
  - chat games and races that play like the built-in ones: rotation, presets, events, challenges, menus, leaderboards and placeholders;
  - reward actions and challenge types.
  They can also start and stop games, read and change points, and listen to new events for answers, wins, challenges, streaks and points. The API's events replace 1.9.9's `customevents` package.

## Fixed

- Two players answering at the same moment can no longer both take the same place.
- The random delay between games was rolled only once.
- The update checker treated 1.9.9 as newer than 2.0.0.

## Requirements

- Java 8 or newer, or whatever your server version needs (Java 25 for 26.x).
- Optional: PlaceholderAPI. MySQL instead of the default SQLite.
