# ChatGames-Premium 2.0.2

A small update with fixes and improvements. Nothing to change in your config files.

- **Accents don't matter:** answers ignore accents on letters, so `cafe` wins for `café` and `arana` for `araña`. Players don't need the accent keys of the config language. Marks that make a different word in other scripts (Japanese, Cyrillic) still count.
- **Friendlier answers:** one trailing `!` or `?` is now allowed, as one trailing full stop already was.
- **Better update notices:** the update check runs every 12 hours instead of only at startup. The join notice names the new version and the one you run, links the Spigot page, and goes to everyone with `chatgames.admin` (operators by default).
- **Quieter console:** the "SLF4J: No SLF4J providers were found" lines on startup, and the Paper "Nag author" warning that came with them, are gone.

# ChatGames-Premium 2.0.1

A small update with fixes and improvements. Nothing to change in your config files.

- **Help shows only what you can use:** `/chatgames help` hides the commands the sender has no permission for, along with the ADMIN header when none of its commands are left. Lines you added yourself are always shown.
- **Global games and away players:** a question moves on once everyone who is playing has answered. A player who hasn't answered any question in that game no longer counts, so one player who is away doesn't make every question run its full time.
- **Quieter console:** targets that the default races.yml lists for other versions (like OCELOT in the Tame race on 1.14+) are skipped without a console note. Targets you added yourself still get one.
- **Translations:** the global game results line ("Top: ...") is now translated in German, Spanish, French, Italian, Dutch, Polish, Portuguese, Russian and Turkish. Fish, Furnace and Tame in the word lists are now translated in Chinese (Simplified and Traditional), Japanese, Korean, Ukrainian, Czech, Swedish, Hungarian, Vietnamese and Indonesian.

# ChatGames-Premium 2.0.0

A full recode. One jar runs on Spigot, Paper and their forks from 1.8.8 to 26.x, and on Folia.

## Upgrading from 1.9.9

Drop the new jar in and restart. On the first start:
- **Your configs are upgraded.** The old files are copied to `plugins/ChatGames-Premium/backups/1.9.9-<date>/` first. Every value you changed is carried over to where it lives now:
  - game settings move to `games.yml`
  - Discord settings move to `discord.yml`, log settings to `add-ons.yml`
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
  - **Tame** and **Shear** races: tame wolves, cats, horses, parrots... (only mobs tameable on your version are picked), or shear sheep and mooshrooms.
- **New Discord integration**, set up in its own `discord.yml`:
  - **Posts much more:** chat games, races, global games, game events, streaks, challenges, and a leaderboard at set times. Turn each one on or off, and send each to its own channels.
  - **Live posts:** a game is posted when it starts and the same post turns into the result. A global game's post shows its progress bar while it runs. Each post shows a countdown in every reader's own time.
  - **Every message is yours to edit:** text, embed colour, author, title, description, fields, pictures, footer and timestamp, with placeholders (PlaceholderAPI too) and emoji written as `:trophy:`. Each game has its own colour and emoji, and the winner's head is shown.
  - **Webhook or DiscordSRV:** post through a channel webhook (nothing to install), through your DiscordSRV bot, or both. With DiscordSRV, winners can be shown with their linked Discord account and, if you want, pinged.
  - **Safe pings:** nothing pings except the roles you allow. `@everyone` or a mention typed by a player never does.
  - **`/chatgames discord test <message|all>`** posts any message with sample values, so you can see how it looks; **`/chatgames discord leaderboard`** posts the leaderboard now (permission `chatgames.discord`).
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
- **Translated config files:** set `lang` to Spanish, French, Portuguese, German, Italian, Dutch, Polish, Russian, Turkish, Chinese (Simplified or Traditional), Japanese, Korean, Ukrainian, Czech, Swedish, Hungarian, Vietnamese or Indonesian and every config file switches to that language: messages, menus, word lists, trivia and comments. Your settings are kept and the old files are backed up first. `config-language` in config.yml can pick a language or turn this off. Any other `lang` still gets translated item, block and mob names.
- **MythicMobs:** Hunt races can target a MythicMobs mob with `mythic:<MobType>`. `count-mythic-mobs` in races.yml decides whether MythicMobs mobs also count as their vanilla type.
- **Custom sounds:** any sound from a resource pack (`namespace:sound`) works in game sounds and menus.
- **Developer API** ([ChatGames-API](https://github.com/nsider4/ChatGames-API) on JitPack). Plugins can add:
  - chat games and races that play like the built-in ones: rotation, presets, events, challenges, menus, leaderboards and placeholders;
  - reward actions and challenge types.
  They can also start and stop games, read and change points, and use `GameType` (including `TAME`, `SHEAR` and `isMobRace()`) and listen to new events for answers, wins, challenges, streaks and points. The API's events replace 1.9.9's `customevents` package.

## Fixed

- Two players answering at the same moment can no longer both take the same place.
- The random delay between games was rolled only once.
- The update checker treated 1.9.9 as newer than 2.0.0.

## Requirements

- Java 8 or newer, or whatever your server version needs (Java 25 for 26.x).
- Optional: PlaceholderAPI. MySQL instead of the default SQLite.
