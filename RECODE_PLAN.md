# ChatGames-Premium 2.0.0 Recode: Status & Plan

Analysis date: 2026-10-20. The baseline is `Downloads/ChatGames-Premium-1.9.9.jar`, decompiled. The recode is at commit `9cc611b` (2025-08-07) plus uncommitted work dated about 2025-08-13.

---

## 0. Target: what ChatGames-Premium 2.0.0 will be

This is the finished feature set the plan builds toward. Items marked *(new)* weren't in 1.9.9. Items marked *(my call)* are design decisions I made where your brief left a detail open. Section 0.1 at the end checks this list against your feature checklist of 2026-10-20.

**Platforms**
- Spigot, Paper and their forks, Minecraft 1.8.8 → 26.x, from one jar.
- Folia 26.x, from the same jar. *(new)*
- SQLite or MySQL storage, with a connection pool.
- Automatic upgrade from 1.9.9: player data and config files are migrated on first start, with backups kept. *(new)*

**Chat games (14)**
- Answer in chat. Each game has its own settings in `games.yml`:
  - on/off, rotation weight, number of winners, time limit, case sensitivity
  - title/subtitle, and its own messages and rewards
- The games:
  - **Unscramble**, **Unreverse**, **Reaction**, **Fill-out** (letters hidden by difficulty %): word lists in `words.yml`.
  - **Hoverable:** hover the chat text to see the word.
  - **Clickable:** first to click wins.
  - **Random:** type a random string. Length range and character set are configurable, including a custom set.
  - **Math:** configurable operators and difficulty. Two modes:
    - *solve*: `7 x 3 - 4 = ?`
    - *missing value*: `3 + ? - 4 = 10`. *(This is what the recode's Sequence game currently does; it becomes a Math mode.)*
  - **Sequence** *(new)*: complete a number sequence, e.g. `3, 6, 12, 24, ?`.
    - Patterns: add, multiply, squares, alternating two steps, and Fibonacci-style.
    - Difficulty controls which patterns are used and how big the numbers get.
  - **Variable:** symbol equations such as ★ + ★ = 10.
  - **Guess the Number:** a range is shown.
  - **Trivia:** questions with multiple accepted answers.
  - **Memorize** *(generalised from Shopping List)*: a list is shown, chat is cleared, and players type the list back.
    - Content comes from pools: words, numbers, colours, blocks/items (translated), mobs, symbols, or "shopping list" (items with amounts, the 1.9.9 game).
    - Configurable list length, display time, and whether order matters.
  - **Word Association** *(new)*: three clues, one hidden link. Example: `Obsidian • Flint • Steel` → *Nether Portal*.
    - Each round accepts several answers and aliases (`nether portal`, `portal`).
    - Hard rounds reveal the clues one at a time. Answering with fewer clues shown counts as a faster place.
    - Ships with about 60 Minecraft associations in `associations.yml`, at easy/medium/hard levels.
- Players who are right except for capitalisation get a "wrong case" hint, with its own sound.
- **Hints** *(new)*: fully configurable per game in `games.yml`:
  - `hints.enable`
  - `hints.after-percent`: when the first hint appears, e.g. 50% of the time
  - `hints.max`: how many hints at most
  - the hint text in `messages.yml`
  - Examples: Unscramble shows the first letter, Fill-out reveals one more letter, Word Association reveals the next clue.
- **Countdown on the action bar** *(new)*, configurable per game. It works on every version: the Spigot API on 1.12+, and XSeries' `ActionBar` on 1.8–1.11. There is no boss bar, because 1.8 doesn't have one.

**Races (7)**
- Hunt (kill mobs), Mine, Place, Fish, Eat, Craft, Smelt.
- Configured per item or mob, with per-amount rewards in `races.yml`.
- Options: count or ignore spawner mobs, and allow or deny creative mode.
- Legacy and modern material and mob names are both accepted.

**Global games** *(new)*
- In a global game *everyone* who solves the round within N seconds is rewarded, instead of just the first N players.
  - Everyone who succeeds gets the participation reward.
  - The first three also get place rewards.
- Global games reuse the normal games' generators, so supporting another one is just a config entry. The list in `global_games.yml`:
  - **Global Reaction** (already in the recode), **Global Unscramble**, **Global Math**, **Global Sequence**, **Global Trivia**, **Global Fill-out**, **Global Word Association**
  - **Global Race**: everyone who mines, kills, catches etc. N of something in the time window. This reuses the race listeners.
- A global game starts by chance after a regular game (`chance_trigger_percent`), on its own interval, by command, or as a step in an event.
- Each global game has its own messages and rewards.

**Game events** *(new)*
- Defined in `events.yml`. An event is a named, ordered list of steps. A step is any chat game, race or global game, with optional overrides (time limit, difficulty, winners), plus a pause between steps.
- Starting an event:
  - by command (`/chatgames event start <name>`) or from the admin menu
  - optionally on a schedule: day-of-week + time, e.g. "Saturday 20:00"
  - optionally only when a minimum number of players is online
- While an event runs, the normal game rotation is paused. It resumes when the event ends or is stopped.
- Scoring:
  - Each step awards event points per place, configurable (e.g. 1st = 5, 2nd = 3, 3rd = 1). Taking part in a global step also earns points.
  - Wins still count towards global stats, points, challenges and streaks as usual.
- When the event ends:
  - The event leaderboard is broadcast, and every participant is told their own place and points.
  - Rewards go to the top places and to all participants.
  - The results go to Discord if the webhook is on.
- Event stats are stored separately, so each event has its own history: runs played, total event wins, best place. `/chatgames event top <name>` shows the all-time event leaderboard.

**Scheduling**
- A game every X minutes, or a random delay between min and max (seconds or minutes). Each game re-rolls the delay. *(fixed: in 1.9.9 it was rolled once)*
- **Weighted rotation, no repeats** *(new, my suggestion)*: each game has a weight, and the same game never runs twice in a row.
- Minimum players online, with options to exclude vanished players or players without the play permission.
- **Presets** *(new)*: admins create named sets of enabled games and load them by command or menu.
  - Ships with: weekend set, minimal set, full rotation.
  - `/chatgames preset save <name>` saves the current enabled games as a new preset.
- Disabled worlds, an optional permission to play, and an option to allow or deny starting games from disabled worlds.

**Winners and rewards**
- Multiple places per game, with per-place rewards. The fastest time is recorded for each winner.
- Reward actions:
  - `[consolecmd]`, `[playercmd]`, `[playermsg]`, `[broadcast]`
  - `[player_as_op]` *(kept from 1.9.9 if it still works on 26.x; otherwise removed)*
  - direct item give; if the inventory is full, the item drops
- Chance-based rewards (`50%~ command`) and permission-gated rewards (`has:perm`).
- Rewards have a minimum-players requirement.
- **Anti-macro** *(new, my suggestion)*: an answer typed faster than a per-game minimum time (default 400 ms for typing games) is rejected and logged. This stops chat bots from winning every Reaction.

**Progression**
- Points: +1 per win (configurable per game), plus admin add/remove/set.
- **Milestones** *(new)*: commands that run at point thresholds (`milestones.yml`).
- **Daily challenges** *(new)*: a set of challenges for everyone, reset every day, defined in `challenges.yml`.
  - Challenge types:
    - win N games, or win N games of a specific type
    - win N races
    - take part in N global games, or win N global games
    - answer in under N seconds
    - earn N points
    - take part in an event
  - Each challenge has its own reward. *(my call: you pick how many are active each day, either a fixed list or N drawn at random from a pool)*
- **Weekly challenges** *(new)*: the same system with a 7-day reset. *(my call: the reset day is configurable, Monday by default)*
- **Claiming**: rewards are claimed from the Challenges menu once a challenge shows "Ready to claim", or with `/chatgames claim`. Unclaimed rewards expire when the challenge resets.
- **Streaks** *(new)*: three streak types, each with a 7-day reward ladder. You configure 7 rewards, and day 8 onward repeats day 7. *(my call; it can be set to loop back to day 1 instead)*
  - **Challenge streak**: days in a row on which you completed *all* the daily challenges.
  - **Daily win streak**: days in a row on which you won at least one game.
  - **Global streak**: days in a row on which you took part in at least one global game.
  - Each streak tracks its current and longest value. Missing a day resets the current streak.
  - Streak rewards are claimed in the Streaks menu.

**Leaderboards**
- Top points.
- Fastest typers overall.
- **Fastest per game** *(new)*. Your overall best time is still kept.
- **Per-event leaderboards** *(new)*.
- Your own position and best time are shown under each list.

**Menus** *(new)*, built with your InventoryManager. Every item, title and slot is configurable in `menus.yml`.
- **Main menu**, `/chatgames menu`:
  - Profile, Challenges, Streaks and Leaderboards
  - your game and sound toggles
- **Profile**, `/chatgames profile [player]`:
  - games won, by type and in total; races won
  - fastest time overall and your best per game
  - global games played and won
  - events played, event wins and best event place
  - current and longest daily, challenge and global streaks
  - points, milestones reached, and challenges completed
- **Challenges**: daily and weekly tabs. Each challenge shows its progress bar and status (in progress, ready to claim, claimed), and you click to claim.
- **Streaks**: three sections (challenge, daily, global). Each shows the 7-day ladder with today highlighted and claimable days clickable.
- **Admin menu**, `/chatgames admin`, permission `chatgames.admin`:
  - Start a game: opens a page with every game and race.
  - Global games: start one.
  - Events: start or stop events.
  - Toggle: your own setting. ToggleAll: everyone's.
  - Presets: load a preset.
  - Logs: pick how many recent entries (10/25/50/100) and they are printed in chat.
  - Reload.
  - Stop the current game.
- **Debug menu**, `/chatgames debug`, permission `chatgames.debug`:
  - Pick any game to run as a private test. Only you see it; there are no rewards and nothing is saved to stats.
  - The menu shows the generated question and the answer.
  - Buttons:
    - answer correctly now
    - answer correctly after X ms (100/500/1000/5000)
    - wrong answer
    - wrong case
    - let it time out
  - Each run reports:
    - generation time
    - whether the mock answer was accepted
    - the measured answer time compared with the injected delay
    - which messages and sounds fired
  - A "run all" button tests every enabled game in turn and prints a pass/fail summary.

**Messages and presentation**
- Everything is in `messages.yml`: `<center>` text, hex colours on 1.16+, and PlaceholderAPI placeholders inside messages.
- **Message code rewrite** *(new)*: the current `messages` classes (one class per game per message type, built from long string-replace chains) are replaced by one small builder: `Msg.of(key).with("%player%", name).send(target)`, plus a typed `MessageKey` enum. Each game declares only the placeholders it adds. This also makes the per-player sending that Folia needs simple.
- Titles/subtitles per game.
- Sounds for start, win, time-expired, wrong-case and hint, with volume and pitch. The correct sound is picked for every version.
- Block, item and mob names are translated into the server language (`lang`):
  - **Translation files are updated to the latest version (26.x)** *(new)*. A script regenerates `nsider4/mine-translator` from Mojang's official language assets and keeps only the block/item/entity/enchantment/effect keys, so the files are about 10x smaller.
  - The plugin re-downloads a translation file when it is from an older version (1.9.9 only downloaded missing files).

**Commands** (`/chatgames`, alias `/chatgame`)
- Players:
  - `help`, `menu`, `profile [player]`, `challenges`, `streaks`, `claim`
  - `top`, `fastest [game]`, `points [player]`
  - `toggle`, `togglesound`
  - `event top <name>`
- Admin:
  - Starting games:
    - `<game> [args]` / `any`: start a specific or random game
    - `<race> [material/mob] [amount]`: start a custom race
    - `global <game>`: start a global game
  - `stop`: end the current game with no winner
  - `event start|stop|list <name>`
  - `preset load|save|list <name>`
  - `admin`, `debug`
  - `reload`, `toggleAll`, `logs [amount]`, `remove <player>`
  - `+points`, `-points`, `setpoints <player> <amount>`
  - `challenges reset <player>`, `streak set <player> <type> <days>`
- `shoppinglist` still works as an alias for `memorize`.
- All subcommands tab-complete. Every permission is declared in `plugin.yml`.

**PlaceholderAPI** (`%chatgames_…%`)
- From 1.9.9: `points`, `toggle`, `top_<n>`, `points_top_<n>`, `personal_best`, `fastest_<n>`, `fastest_time_<n>`, `show_current_pos_top`, `show_current_pos_fastest`.
- New:
  - `sound_toggle`, `wins`, `wins_<game>`
  - `fastest_<game>` (your best time), `fastest_<game>_<n>` and `fastest_<game>_time_<n>`
  - `daily_completed`, `daily_total`, `weekly_completed`, `weekly_total`
  - `streak_<daily|challenge|global>` and `streak_<…>_longest`
  - `global_played`, `global_won`
  - `event_running`, `event_name`, `event_points`, `event_place`
  - `current_game`, `time_left`
- All of them are served from memory, so a scoreboard never waits on the database.

**Integrations and admin tools**
- Discord webhook for winners and event results: embed, avatar, fields.
- Game logs to the console and/or a file, viewable in-game with `/chatgames logs` or the admin menu.
- Update check against Spigot, with a notice for OPs on join.
- `/chatgames reload` reloads every file without a restart.

**Developer API**
- `ChatGamesStartEvent` *(now actually cancellable; in 1.9.9 cancelling did nothing)*.
- `ChatGamesEndEvent`, which carries the game, answer, and winners with places and times.
- `ChatGamesEventStartEvent` / `ChatGamesEventEndEvent` *(new)*, for game events.

### 0.1 Your checklist (2026-10-20) → where it lives

| Request | Status before | Where in 2.0.0 |
|---|---|---|
| Easier-to-read message classes | not in plan | Message code rewrite (step 5) |
| General memorization game | Memorize existed, shopping-list only | Memorize content pools (step 5) |
| Daily challenges + claim menu | data model only | Challenges + Challenges menu (step 9) |
| Streaks: challenge / daily / global, 7-day setup | the recode counted wins, not days | Streaks + Streaks menu (step 9) |
| Weekly challenges | data model only | Challenges (step 9) |
| Milestones | done in config, not wired | step 5 |
| Fastest time per game + overall | DB table only | steps 4–5 |
| Sequence-completion math game | recode had missing-value equations | new Sequence; old one becomes a Math mode (step 5) |
| Global games | Reaction skeleton | 8 global games (step 9) |
| Admin menu | empty `Menu` enum | Admin menu (step 9b) |
| `/chatgames profile` menu | not present | Profile menu + new stats columns (steps 4, 9b) |
| Preset loader | config only | Presets incl. `preset save` (step 9) |
| Debug menu | not present | Debug menu (step 9b) |
| Update language files | files are from March 2023 | translation script + version check (step 9c) |
| Word Association | new | step 9a |
| Game events | new | step 9a |
| More global games | new | step 9 |
| Suggestions | | configurable hints, action-bar countdown, weighted no-repeat rotation, anti-macro (steps 5, 9) |

**Other suggestions, not in scope unless you want them:**
- monthly "seasons", where leaderboards reset with rewards for the top 3
- per-game opt-out, so a player can hide one game they dislike
- BungeeCord/Velocity sync, so one game runs across a network


## Progress (branch `recode-2.0`)

- [x] **Step 1: builds again** (2026-10-20). `mvn package` passes, with 5 tests.
  - The bundled `XMaterial` was replaced by XSeries 14.0.0. FoliaLib 0.5.1 was added. Everything shaded is relocated to `org.nsider.chatgames.lib.*`. Bytecode targets Java 8 via `release`.
  - The 36 API mismatches are fixed.
  - Pulled forward from step 2:
    - `GameType.configKey`/`isRace`/`isMaterialRace`, which fixes bugs 7 and 8 (`GUESS_NUMBER_*` was renamed to `GUESS_THE_NUMBER_*`).
    - The new `VersionUtils` parser, which handles `1.8.8-R0.1-SNAPSHOT`, `26.1.2-R0.1-SNAPSHOT` (Spigot) and `26.3.build.207-stable` (Paper), with unit tests.
  - `VersionUtils.correctConfigs` was removed; it rewrote users' configs. Races now resolve materials and entities through XSeries, so legacy and renamed names work, e.g. `RAW_FISH`/`COD` and `MUSHROOM_COW`/`MOOSHROOM`.
  - Found while porting:
    - The recode's `isMaterialRace` had dropped EAT and FISH, so `LogListener` would NPE on those races. This is fixed.
    - `[player_as_op]` was ported as-is from 1.9.9: a fake sender that claims OP. It's an anonymous `CommandSender` compiled against the 1.8 API, so on modern servers any newer interface method a command calls on it fails with `AbstractMethodError`. Vanilla commands may also reject a non-player sender. Test it on 26.x, or drop it in step 6. *(Step 6 replaced the fake sender with op-for-one-dispatch.)*
  - Still stubbed until its step: PlaceholderAPI is only detected, not hooked (step 7). The full `onEnable` order comes in step 3.
- [x] **Step 2: foundations** (2026-10-20). `mvn package` passes, with 34 tests.
  - **Config:** `ConfigPaths` resolves its file through the new `ConfigFile` enum when read, so values survive `/chatgames reload` (they used to keep the old file, or were null before init). `ConfigManager.initialize` is now called first in `onEnable`, and `Messages` is built after `messages.yml`. `reload()` reloads every file, adds `global_games.yml`, and rebuilds the messages.
  - **Database:**
    - `QueryUtils.query(conn, sql, handler, params)` reads rows while the statement is open. The old helper returned a closed `ResultSet`. Tested against a real SQLite database.
    - SQLite now uses one pooled connection.
    - MySQL falls back to the legacy `com.mysql.jdbc.Driver` on servers older than 1.16.
    - The leaderboard no longer queries the database on the main thread, and no longer casts the console to `Player`.
  - **Scheduler:** `ChatGamesScheduler` (FoliaLib behind it) replaces `TaskUtils` and every `Bukkit.getScheduler()`/`BukkitRunnable`. There are no direct Bukkit scheduler calls left. The game rotation reschedules itself each round, which also fixes the random delay being rolled once.
  - **Thread-safe game state:** `Game` now owns its clock, winners and end state. `GameController`'s static map is gone.
    - `claimPlace` is atomic. A 50-thread test, repeated 20 times, always gives exactly places 1..N.
    - `finish()` makes sure the last winner and the time limit can't both end a game.
    - Times are measured from game start with `nanoTime`.
  - **Also fixed:**
    - Rewards are given by the place passed in, so global-game participants actually get rewards. Before, they looked up a result that never existed.
    - `ChatGamesEndEvent` carries its own answer and winners, so listeners no longer read a game that may already be cleared.
    - `GameType.fromName` replaces the `valueOf` calls that threw on unknown names. It accepts `shoppinglist`.
  - Not yet checked on a live server: the plugin can't start until step 3 registers its listeners and commands. Steps 3 and 12 cover that.
- [x] **Step 3: bootstrap** (2026-10-20). `mvn package` passes, with 38 tests. **Boot-tested on real servers:** Spigot 1.8.8, Spigot 1.13 and Spigot 1.20.1, Paper 1.21.1 and Paper 26.3, and Folia 26.1.2. Each test covers enable, DB connect + table creation, `/chatgames reload`, unknown sub-command, and clean disable (`scripts/boot-test.py`).
  - **`onEnable` order:**
    1. messages util → configs
    2. scheduler + platform log
    3. database (the plugin disables itself with a clear message if it can't connect)
    4. player cache (+ players already online)
    5. game systems
    6. listeners
    7. `/chatgames`
    8. PAPI detection
    9. rotation, 5-min autosave, update check
  - **`onDisable`:** stop the rotation and global game, end the running game without rewards, cancel tasks, then save every player *synchronously* (async tasks don't run while disabling), then close the pool.
  - **`reloadAll()`:** reloads every file and messages, reloads the language if `lang` changed, reloads the enabled games, restarts the rotation. DB settings still need a restart.
  - **Command skeleton:** `ChatGamesCommand` routes `SubCommand`s and handles permission, player-only and tab completion. `reload` works; step 6 adds the rest.
  - **Join/quit:** these now load/save the player cache off the main thread. The join handler used to query the DB on the main thread.
  - `LanguageManager` downloads off-thread through a temp file and falls back to English names until the download arrives. `RaceUtils` no longer NPEs when a key is missing.
  - **Found by the real-server tests and fixed:**
    - `MessageUtils` was never initialised, so `onEnable` crashed.
    - The 1.8–1.12 bundled SQLite driver lacks `Connection.isValid` (`AbstractMethodError` in HikariCP). Fixed with a test query.
    - The update checker called 1.9.9 an "update" over 2.0.0. It now compares versions numerically, with tests.
    - SLF4J's stderr warning, which Paper logs as an ERROR and a nag, is silenced by bundling `slf4j-nop`.
    - Below 1.16, colours were applied by replacing every `&`. They now use `translateAlternateColorCodes`.
  - `folia-supported: true` was added early so the scheduler could be tested on Folia; the remaining Folia hardening stays in step 11.
- [x] **Step 4: player cache** (2026-10-20). 48 tests. Tested with a scripted player (`scripts/bot.js`, mineflayer) on Paper 1.21.1 and Spigot 1.8.8: first join creates the row, quit saves, a quick quit-and-rejoin loads the saved data, and a non-op is refused `reload`.
  - **`ChatGamesPlayer`** holds everything about one online player: points, toggles, stat counters, per-game best times, and the daily-challenge v1 fields. Every method is synchronized, so concurrent wins are never lost; a 1000-increment test confirms it. Changes are tracked as dirty and only those are written.
  - **`PlayerRepository`** holds all the per-player SQL, with one code path for SQLite and MySQL (`REPLACE INTO`).
    - A save is one transaction.
    - New players' rows are created inside `load`. This fixes a bug: the old code inserted asynchronously and read straight back, so **new players were never cached at all**.
  - **`ChatGamesPlayerManager`** runs all player database work on one dedicated thread, in order, so the quit's save always lands before the rejoin's load.
    - `modify(uuid, change)` works for offline players too: it loads, changes and saves. This is ready for the admin point commands.
    - `delete(uuid)` clears every table.
    - `shutdown()` blocks until every save is written.
    - A failed save keeps the changes marked dirty, so the next save retries them.
  - **New table `<table_name>_stats`** stores (player, stat, value) counters. A new profile statistic (events, streaks, challenges completed…) just needs a new name in `Stat`, with no migration. It starts with `wins`, `wins.<game>`, `race_wins`, `global_played` and `global_won`. The daily table is now `<table_name>_daily`.
  - **Game code no longer reads the database:** toggles, sound and points all come from the cache. `UpdateQueries`, `ManagementQueries`, `FastestTimeQueries`, `ChallengeQueries` and `FastestData` are gone.
  - **Leaderboards:** they use the configured fastest-times table; 1.9.9's hard-coded `game_fastest_times` never matched it.
  - **Milestones:** these now load on enable and reload (they were never loaded), and run their commands on the main/global thread.
  - **Left for later steps:**
    - Wins don't yet add points or stats; that's step 5.
    - A milestone crossed while the player is offline is skipped. *(Fixed in step 6: it is paid when they next join.)*
    - 1.9.9 databases have a `fastest NOT NULL` column that the new insert doesn't fill, so the step 10 migration must run before this.
    - MySQL is tested in step 12 (no MySQL server here).
- [x] **Step 5: core game loop** (2026-10-20). 79 tests. **Played on real servers** by a scripted player that reads and answers questions (`scripts/bot.js` `autoplay`), on Paper 1.21.1, Spigot 1.8.8 and Folia 26.1.2. All three: wins, Clickable by clicking, Memorize, Math missing-value, Sequence, the wrong-case message, anti-macro, hints, the action-bar countdown, "time's up" and a milestone command. Points, per-game wins and per-game fastest times were checked in the database afterwards.
  - **New structure:** each game has a `RoundGenerator` that builds a `Round` (answers, what to show, placeholders, hints, clickable/hoverable text) with no side effects. `GameBuilder` then generates the round, fires `ChatGamesStartEvent`, announces it and schedules the time limit, hints and countdown. `AnswerHandler` checks answers and pays out; `RoundAnnouncer` sends every game message through the new `Msg` builder. The six old message classes, `GameAlgorithms`, the handler factory and `TextMatchUtils` are gone, as are ~90 per-game `ConfigPaths` entries: `GameSettings` reads each game's games.yml section, with defaults for keys a 1.9.9 file lacks.
  - **Games:**
    - **Sequence** (new): add, multiply, squares, alternating and Fibonacci patterns; difficulty 1–3 picks patterns and number size.
    - **Math** gained `modes: [solve, missing-value]` and `operations`. Divisions are always exact (the 1.9.9 game could show `7 / 2` and expect 3).
    - **Memorize** draws from pools: shopping_list (1.9.9's list), words, colors, numbers, blocks and mobs (both shown in the server language), or any list you add. `order-matters` can be turned off. The list shows, chat clears, then answers open.
    - Admin arguments work for every game (a word, an expression, a list length…), ready for `/chatgames start` in step 6.
  - **New per-game settings** (all optional, documented at the top of games.yml): `weight`, `points`, `min-answer-ms` (anti-macro, default 400), `hints.enable/after-percent/max`, `countdown.enable/last-seconds`. The shared `hint_message` and `countdown_actionbar` are in messages.yml, and a game can override either.
  - **Rotation:** weighted, never the same game twice in a row, skips a round while a game is running, and keeps going if a game fails to start (one exception used to stop it until restart).
  - **Wins** add points (`points`, default 1), `wins` and `wins.<game>`, and the fastest time, and run milestone commands.
  - **Bugs fixed:** 9, 10, 11, 12, 13, 14, 16 (rest), 18, 19, 20, 21, 24, 25. Also:
    - The winner message stripped any text between two `%` signs.
    - Placeholders were replaced as regexes, so a `$` in a value broke messages.
    - Fill-out could hide the same letter twice, which hid fewer letters than its difficulty set.
    - A hint could reveal a whole answer ending in `_` (such as "Jeb_").
    - Sounds now go through XSeries, so `minecraft:entity.player.levelup` also works on 1.8.
    - Titles now go through XSeries, which handles every version.
    - The empty Discord webhook no longer logs a warning on every win.
  - **Until step 8:** races have no data loaded yet, so the default `enable-races: true` makes the rotation log an error whenever it picks one (it then carries on). The gameplay tests ran with races off. Races still use the old `GameMessageManager`.
  - **For step 11:** reward commands (`[playercmd]`) and broadcasts run on the global thread; Folia wants player commands on the player's thread. The Folia run had no errors with the default rewards, but this is still to do.
- [x] **Step 6: commands** (2026-10-20). 71 tests (step 5's "79" was a miscount; it was 68). Tested on real servers with two scripted players (an op and a non-op) plus the console, on Paper 1.21.1, Spigot 1.8.8 and Folia 26.1.2: every command below, its error cases, and tab completion.
  - **Structure:** one class per sub-command under `commands/sub`, routed by `ChatGamesCommand` (names and aliases ignore case; plain `/chatgames` shows the help). `CommandSupport` holds the shared bits: messages, number parsing, and an offline-safe player lookup that asks Mojang off the main thread. Commands that touch the running game hop to the global thread, because Folia runs a player's commands on that player's region thread.
  - **Ported from 1.9.9:** `help`, `top`, `fastest`, `points`, `toggle`, `togglesound`, `toggleAll`, `reload`, `logs [amount]`, `remove <player>`, `+points`/`-points`/`setpoints <player> <amount>`, `<game> [values]` and `any`. `shoppinglist` still works as an alias for `memorize`.
  - **New:**
    - `stop` ends the running game with no winner.
    - `fastest <game>` shows one game's leaderboard.
    - `points <player>` and `toggle|togglesound <player>`, with new `.others` permissions.
    - `preset list | load <name|none> | save <name>`. Presets moved from config.yml to a new `presets.yml`, because saving config.yml from code wipes its comments on older servers.
  - **Permissions:** `PermissionKey` lists every permission the code checks, and plugin.yml declares the same set. There is now one `chatgames.start.<game>` per game, all granted by `chatgames.start.*`. `PermissionKeyTest` fails the build if the two lists drift apart.
  - **Fixed along the way:**
    - `top` deadlocked the SQLite pool for 20 s: it held one connection and asked for a second.
    - Leaderboards could miss the last 5 minutes of points. They now save online players first and read after that save.
    - The points top read the wrong messages.yml section (`POINTS.*` instead of `top.*`), so it would have crashed.
    - Leaderboards showed "5.0 points" and crashed on a player with no known name. An empty leaderboard now shows its `no_data_message`.
    - `toggle <player>` let anyone toggle anyone else. It now needs `chatgames.toggle.others`.
    - `+points abc` threw an exception. Amounts are now validated.
    - Starting a game while one runs used to cancel the old game's timer and leave it stuck. It now says a game is already running.
    - `stop` fires `ChatGamesEndEvent`, so listeners and the game log see every game end.
    - `CooldownManager` kept every Player object forever and wasn't thread-safe.
  - **Milestones for offline players:** points given to an offline player now store any milestone crossed as pending, and the commands run when that player next joins (tested live). They used to be lost.
  - **`[player_as_op]`:** the 1.9.9 fake op sender was replaced. The player is made op only for that one synchronous dispatch, inside a `finally`. Modern servers reject the fake sender for vanilla commands.
  - **Moved to later steps:**
    - `challenges` and `claim` go to step 9. The v1 daily challenge has no rewards or config to claim yet, so a command now would have nothing to give.
    - Custom race values (`/chatgames mine <material> <amount>`) go to step 8 with the race rebuild. Until then, a race starts with random values.
    - `profile`, `menu`, `admin`, `debug`, `global` and `event` arrive with their features in step 9.
  - **Test tooling:** `scripts/bot.js` gained `tab:<text>` (prints the server's completions) and `play:on|off` (autoplay in the background). `boot-test.py` strips colour codes, so coloured console replies are shown.

---

## 1. Where you left off

The last commit is *"Added fastest top for all separate games, added memorize and sequence games…"*.
After that, uncommitted edits in 7 files were partway through **wiring races into the new structure**:

- `RaceMessageSender` now extends `GameMessageManager` and gained `sendTimeExpiredToWorlds()`. It reads race times through `ConfigPaths.RACE_<GAME>_TIME_LIMIT`.
- `RaceBuilder.endRace()` was added. `race` and `raceMessageSender` are now exposed.
- `RaceStarter` now stores the race on `RaceBuilder` instead of a local field.
- `ExpirationTask` was moved to `ConfigPaths`, `GameController` and `GameUtility`.
- `ChatGames` now creates `ExpirationTask`.
- `chatRaces.yml` was renamed to `races.yml`, and `getChatRaces()` became `getRaces()`.

The next logical step was finishing the race flow: the event listeners, scoring and the win path. After that comes the bootstrap in `onEnable`.

**Build state:** the project does not compile.
- `XMaterial` imports `org.nsider.chatgames.annotations.*`, which doesn't exist.
- With stub annotations in place, 36 more compile errors remain (section 2A).

---

## 2. Pending items in the recode

### A. Compile errors (36). Each is a renamed or removed API that callers weren't updated for.

| Where | Problem | Fix |
|---|---|---|
| `XMaterial` | `org.nsider.chatgames.annotations` package missing | Replace `XMaterial` with the XSeries dependency (see 4). Or point it at `inventorymanager.extras` |
| `ChatGames` | `PlaceHolderHook` class doesn't exist | Port it from 1.9.9 (section 3) |
| `ChatGames` | `VersionUtils.correctConfigs` is an instance method called statically. `getChatRaces()` was renamed | Rework (see 4) |
| `ChatGames` | `checkForUpdate()`, `mainTask`, `isUpdateNeeded()` missing | Add them back. `UpdateChecker` exists, resource id is `107781` |
| `GameBuilder`, `RaceStarter`, `MemorizationMessage` | Call `expirationTask.timeExpiredTask()`, but the method is now `scheduleExpirationTask()` | Rename the calls |
| `GameBuilder` | `GameManager.setCurrentGame()` was removed | Delete the call. `game` already holds the state |
| `ExpirationTask` | `GameManager.sendTimeExpiredToWorlds()` was removed | Add a `TimeExpiredMessage` next to the other `messages/*` classes |
| `ExpirationTask`, `ChatListener` | `GameController.getResults()` is now a `Map<UUID,PlayerResult>`, but `ChatGamesEndEvent` and `LogListener` expect a `List` | Pass `new ArrayList<>(results.values())` sorted by place. Or change the event to a `List` copy |
| `WinnerMessage` | For-each over a `Map` | Iterate `.values()` |
| `GameUtils`, `RewardManager` | `plugin.getRewardManager()` / `getCommandUtils()` don't exist | Add the fields. Keep only one command executor (see 6) |
| `RaceUtils` | `plugin.getLanguageManager()` doesn't exist | Add the field and construct it with `ConfigPaths.LANGUAGE` |
| `VariableMessage` | `MessageUtils.getCenteredMessage()` doesn't exist | Use `CenteredMessageUtils` |
| `GlobalReactionGameListener` | `plugin.isGlobalGameActive()` doesn't exist | Move this state into `GlobalGameController` |
| `ChatListener` | `ConfigPaths.CANCEL_MESSAGE_WHEN_WINNING` doesn't exist (the constant is `CANCEL_MESSAGE_ON_WIN`) | Rename |
| `ChatListener` | `TopManager.isFasterThan` / `setFastestTime` now take a `GameType` | Pass `game.getGameType()`, and also update overall-fastest |
| `LogListener` | `Race` isn't imported. `isMaterialRace` moved to `GameUtility` | Fix the import and the calls |
| `LoginListener` | `isUpdateNeeded()` | See `ChatGames` row |
| `MainTask` | `getNextGameDelay`, `checkPlayersOnline`, `isRace` and `getConfigManager` moved | Use `GameConfigManager`, `GamePlayerManager.checkMinimumPlayersOnline` and `GameUtility.isRace`. `ConfigManager` is static |

### B. Never wired up. These classes compile but nothing uses them.

- **`onEnable` bootstrap is missing**:
  - `ConfigManager.initialize()` is never called, so every `ConfigPaths` entry holds `null` → NPE on the first config read.
  - Also never called: `MessageUtils.initialize`, `VersionUtils.initialize`, `CommandUtils`, `QueryInitializer` and `ChallengeQueries.initialize` (it is missing from `QueryInitializer`).
  - Also never called: `ChatGamesPlayerManager.initialize`, `ChallengeManager.initialize`, `MilestoneManager.loadMilestones`, `InventoryManagement.initialize`, `MainTask`, `LanguageManager` and `RewardManager`.
- **No listeners are registered.** `ChatListener`, `LoginListener`, `DiscordListener` and `LogListener` are never passed to `registerEvents`.
- **No commands.** The `commands` package doesn't exist: no `Command`/`TabCompletion`, and `/chatgames` does nothing. Clicking the clickable-game button (`/chatgames <key>`) has no handler either.
- **No race listeners.** 1.9.9's `ChatRaces` handled these events:
  - Hunt: `EntityDeathEvent`, plus `CreatureSpawnEvent` to flag spawner mobs.
  - Mine: `BlockBreakEvent`. Place: `BlockPlaceEvent`. Fish: `PlayerFishEvent`. Eat: `PlayerItemConsumeEvent`.
  - Craft: `CraftItemEvent` with shift-click math. Furnace: `InventoryClickEvent`.

  None of these exist in the recode, so `RaceHandler.addScore`/`playerWon` are never called.
- **Race data is never loaded.** `RaceDataLoader.loadRace()` has no callers. 1.9.9 had `loadRaces()` in the main class. Without it the data maps stay empty, and `startEntityTypeRace` throws an NPE at `amountList.get(...)`.
- **No winner path for races.** Nothing awards race winners: no rewards, messages, end event or `endRace()`.
- **Points are never awarded.** 1.9.9 called `addPoint(p, 1)` in `ChatListener`. In the recode, `TopManager.addPoint` has no callers.
- **Fastest times are never persisted.** `FastestTimeQueries.updateFastestTime` has no callers. `ChatGamesPlayerManager.save()` doesn't write `FastestData`.
- **Player cache lifecycle is missing.** `ChatGamesPlayerManager.load/unload/saveAll` are never called: no join load, no `PlayerQuitEvent` unload, nothing in `onDisable`.
- **Challenges are not hooked up.**
  - `ChallengeManager.incrementWinsIfApplicable` / `incrementGlobalStreak` have no callers.
  - There is no claim command and no reward config for the daily, weekly or global challenges.
  - There is no weekly reset.
  - `dailyStreak` is incremented on every win, which doesn't match its name.
- **Global games**:
  - `global_games.yml` isn't loaded by `ConfigManager`.
  - The listener is registered per game but the "active" flag doesn't exist.
  - Messages are hard-coded with `TODO`s, and rewards use a fake `"global_word"` key.
  - Nothing schedules a global game (`chance_trigger_percent` is unused).
- **Presets are half done.** `MainTask` reads `active-preset`/`presets` directly (TODO). There is no command or GUI to switch presets.
- **Inventory GUI is a stub.** `Menu` is an empty enum (`{;//TODO REMOVE ;`), `getMenu()` returns null, and there's no menu YAML. The commit said "added inventory utility and base code", but no actual menu exists.
- **`onDisable` is empty.** It should save all players, close Hikari and cancel tasks.
- **`MessageKey`/`Messages` are only partly used.** Most code still reads raw strings from `messages.yml`.

### C. Logic bugs in code that does compile

**Critical**
1. **`QueryUtils.executeQuery` returns a `ResultSet` from a `PreparedStatement` it already closed** (try-with-resources). That breaks every read query: points, toggles, fastest data and tops. Rewrite it as a callback or mapper so statement and result set are closed after reading.
2. **`ConfigPaths` captures `FileConfiguration` objects when the enum class loads.**
   - If the enum loads before `ConfigManager.initialize()`, every value is permanently null.
   - `plugin.reloadConfig()` *replaces* the config object, so `/reload` leaves config.yml paths pointing at stale data.
   - Store a `Supplier<FileConfiguration>` (or a file id) instead.
3. **`Messages` is constructed before `MessagesConfig` exists** in `ConfigManager.initialize()`, so every message falls back to its default.
4. **Upgrading from 1.9.9 breaks the database.**
   - The 1.9.9 `chatgames` table has a `fastest DOUBLE NOT NULL` column. `CREATE TABLE IF NOT EXISTS` won't change it.
   - The recode's 4-column `INSERT` then fails for every new player on existing installs.
   - The fix is a migration: copy `fastest` into the new fastest-times table, then drop the column or give it a default.
5. **`TopPlayerQueries` hard-codes the table `game_fastest_times`**, but the config default is `chatgames_fastest_times`. Fastest tops would always be empty or error.
6. **Main-thread DB calls.**
   - `GameManager.gamesIsToggled()` and `GameSoundManager` hit the DB synchronously for every player on every broadcast.
   - `getPlayerToggle` throws a `RuntimeException` on SQL error.
   - Read from the `ChatGamesPlayer` cache instead.

**Game-specific**
7. `Game.getGameConfigName()` has no `SEQUENCE` case and returns `""`. The sequence game's winners, time and messages all break.
8. `guess_the_number` maps to `ConfigPaths.GUESS_THE_NUMBER_*`, but the enum is named `GUESS_NUMBER_*`, so `valueOf` throws. Rename the enum entries or add a `ConfigPaths` lookup on `GameType`.
9. **Memorize:** `MemorizationMessage` schedules `initGame()` *once per online player* inside the loop. The game is started N times, and never if nobody is eligible.
10. `messages.yml` has **no `memorize:` or `sequence:` sections**, so both games announce nothing. `games.yml` and `messages.yml` still carry the old `shoppinglist` section.
11. `MathQuestionGenerator.generateMissingValueEquation`:
    - `operators.get(rand.nextInt(operators.size() - 1))` never picks the last operator and crashes with a single operator.
    - The "target" is computed with x=0, so x is always 0 unless precedence changes it. The puzzle needs a real design.
12. `BuilderUtils.getRandomValue`: if *any* word in the list is blacklisted, the filter drops *every* word and returns `""`. It also shares one `Random` per call inside `synchronized`, which does nothing.
13. Code still reads moved keys from `config.yml`:
    - `TextMatchUtils` (`<game>.case-sensitive`) and `WinnerMessage` (`<game>.winners`) need `games.yml`.
    - `SymbolEquationCreator` (`variable.toGet`) also needs `games.yml`.
    - `LogUtils` (`log-file`) needs `add-ons.yml`.
    - All of these silently read defaults.

**Lifecycle & events**
14. `ChatGamesStartEvent` is `Cancellable`, but it's fired one tick *after* the game started, so cancelling does nothing.
15. `ExpirationTask` schedules an async task that only hops back to sync. Use `runTaskLater`. `MainTask` runs async and hops to sync too. Both are fine as sync tasks.
16. `MainTask.enable()` computes the random delay once, then repeats at a fixed period. The random-delay setting effectively doesn't work: it needs re-scheduling after each game. `selectAndStartGame` also doesn't skip when a game is already running.
17. `TimeUtils` and `GameController` are global static state. That's OK for one game at a time, but it collides with global games and races.

**Smaller**
18. `WinnerMessage` plays the win sound for players in disabled worlds.
19. `CommandUtils.broadcastMessage` checks the *winner's* world, not each receiver's.
20. `CommandUtils.initialize` is an instance method that sets a static field.
21. `RandomReward` shares one `total` between two maps, so resetting one corrupts the other. This was carried over from 1.9.9.
22. `TopPlayerQueries.getPersonalValue` casts the sender to `Player`. That crashes from the console and NPEs if the player isn't cached.
23. `RaceHandler.addScore` stores `1` on the first hit instead of `amount`. Its `results` map is never cleared.
24. `GameUtils.giveItemReward` uses `Material.getMaterial()`, which breaks across legacy and modern names. Use XMaterial. `Integer.parseInt` there is unguarded.
25. `HikariDatabase.initialize()` falls back to `"pa55w0rd"`. `getProperties()` applies MySQL-only properties to SQLite too.

---

## 3. What changed vs ChatGames-Premium 1.9.9

### Added in the recode (new compared to 1.9.9)

- **New games:** `MEMORIZE` (replaces/renames `SHOPPINGLIST`) and `SEQUENCE` (missing-value equation). The `GameType` enum changed, so a stored game name `shoppinglist` no longer maps.
- **Config split:**
  - `config.yml` is now: `config.yml` + `games.yml` (per-game settings) + `add-ons.yml` (Discord, logs) + `races.yml` (was `chatRaces.yml`) + `milestones.yml` + `global_games.yml`.
  - Every 1.9.9 key still exists somewhere, but there's **no migration** for existing servers' `config.yml` and `chatRaces.yml`.
- **New config keys:**
  - `GameSounds.pitch`, `fastest_times_table`, `active-preset` + `presets.*`.
  - `global_word.*` (enable, chance, duration, broadcast prompt) and `milestones.<points>`.
  - `memorize.*` and `sequence.*`.
- **Typed config layer:** `ConfigPaths` (168 entries, all resolve against the YAML files), `ConfigValue`/`ListConfigValue`, `MissingConfigDetector`.
- **Player data model:** a `ChatGamesPlayer` cache with points, toggles, daily wins, challenge/daily/global streaks, weekly wins and per-game fastest times.
- **New DB tables:**
  - `chatgames_daily` for challenges.
  - `chatgames_fastest_times`, which holds fastest time per game. 1.9.9 had one `fastest` column.
- **Per-game fastest tops:** `TopType.GameTypersTop` plus new `messages.yml` `GameTypersTop.*` section.
- **Milestones:** point thresholds that run commands.
- **Daily/weekly/streak challenges:** data model only so far (2B).
- **Global reaction game:** everyone who types the word in time is rewarded. Skeleton only.
- **Game presets:** enable a named set of games instead of per-game `enable` flags.
- **Inventory GUI base:** uses your `InventoryManager` library 1.1.2, shaded. Skeleton only.
- **Restructure:**
  - The game builder split into `GameHandler` strategies and an `algorithms/` package. `GameManager` split into message, sound, player and config managers.
  - `Database.java` (1,457 lines) split into `queries/*`. `ChatRaces` and `RaceBuilder` split into `setup/` and `validator/`.
  - Package renamed `chatgamespremium` → `chatgames`.
- **Permissions:** now declared in `plugin.yml`, and `PermissionKey` was added. 1.9.9 declared none.
- **rewards.yml:** new `memorize` and `sequence` reward sections.

### Missing from the recode but present in 1.9.9 (must be ported)

- **`/chatgames` command and tab completion.** Subcommands:
  - `help`, `top`, `fastest`, `points [player]`, `any`, `<game> [args]` for all 19 games.
  - `toggle`, `togglesound`, `reload`, `toggleAll`, `logs`, `remove <player>`.
  - `+points`, `-points`, `setpoints`, and the clickable-game key handler.
  - Note: 1.9.9 checks `chatgames.points` and `chatgames.manage.points`, which are **not** in the new `plugin.yml`/`PermissionKey`.
- **PlaceholderAPI expansion** (`%chatgames_...%`):
  - `points`, `toggle`, `top_<n>`, `points_top_<n>`, `personal_best`.
  - `fastest_<n>`, `fastest_time_<n>`, `show_current_pos_top`, `show_current_pos_fastest`.
  - New ones are needed for per-game fastest times, streaks and challenges.
- **Race event listener** (`ChatRaces`), **`loadRaces()`**, **`reloadAll()`** and the **update checker** wiring.

---

## 4. Supporting Minecraft 1.8 → 26.x

Mojang dropped `1.x` numbering after **1.21.11**. Releases are now `26.1`, `26.2`, … (year.drop.hotfix), so "1.26" doesn't exist. **26.1+ requires Java 25.** The recode currently breaks on these versions:

1. **`VersionUtils.getMCVersion()`** takes the *second* number of `Bukkit.getBukkitVersion()`.
   - On 26.x that gives `1`, so the plugin thinks it's on 1.1. Hex colors turn off (they require ≥16) and legacy paths trigger.
   - Replace it with a parsed `(major, minor, patch)` and a helper `isAtLeast(1, 16)` that treats `major >= 26` as newer than every 1.x.
   - Check the real version string on a 26.x server before finalising the parser.
2. **Replace the bundled `XMaterial` 11.4.1 with XSeries** (`com.github.cryptomorin:XSeries`, latest), shaded and relocated.
   - The old copy doesn't know 1.21+/26.x materials, and it is what breaks the build.
   - Use `XMaterial` for race materials and reward items, `XSound` for game sounds and `XEntityType` for hunt mobs.
   - Entity names were renamed in 1.20.5 (`MUSHROOM_COW` → `MOOSHROOM`, `SNOWMAN` → `SNOW_GOLEM`). `SALMON`/`COD` don't exist on 1.8.
3. **Sounds:** config uses `minecraft:entity.player.levelup` keys, which work on 1.9+ only.
   - The 1.8 fallback in `correctConfigs` writes enum names (`LEVEL_UP`), but `playSound(String)` on 1.8 expects `random.levelup`-style names. Neither path really works.
   - Resolve through `XSound.matchXSound(...)` instead, and drop the config rewriting. Rewriting a user's config on startup is bad practice anyway.
4. **Chat:** `AsyncPlayerChatEvent` still fires on Spigot and Paper 26.x, though Paper deprecates it.
   - Keep it for 1.8 compatibility.
   - Check that cancelling the winning message behaves on Paper, where chat signing is involved.
5. **Titles and components:** `Player.sendTitle(String,String)` and the Bungee `HoverEvent(Action, BaseComponent[])` constructor are deprecated but present from 1.8 to the latest. They're fine. Wrap them in one small `Compat` class so the deprecated calls live in one place.
6. **JDBC drivers:**
   - 1.8 to about 1.16 Spigot ships the old MySQL driver `com.mysql.jdbc.Driver`; newer servers ship `com.mysql.cj.jdbc.Driver`.
   - `DatabaseDriver.MySQL` hard-codes `cj`.
   - Try `cj` first, then fall back to the legacy driver. SQLite's `org.sqlite.JDBC` is bundled on all versions.
7. **Build:**
   - Keep compiling against `spigot-api 1.8.8` with Java 8 bytecode (`<maven.compiler.release>8</maven.compiler.release>`). Java 8 bytecode runs on Java 25.
   - Bump Lombok to the current release so it works on new JDKs.
   - Add `<relocations>` to the shade plugin for HikariCP, InventoryManager and XSeries. Right now they're shaded unrelocated and will clash with other plugins.
   - Exclude slf4j, or use `minimizeJar`.
8. **`plugin.yml`:** keep `api-version: '1.13'`; 1.8–1.12 ignore it.
9. **Folia:** supported in the same jar. See section 5.
10. **Test matrix** for each release: 1.8.8, 1.12.2, 1.16.5, 1.20.4, 1.21.11 and 26.x (Spigot and Paper), **plus Folia 26.x**.
    - Smoke checklist: start each game, win it, let it expire.
    - Also: one race of each type, `/chatgames top|fastest`, the placeholders, MySQL and SQLite, and `/chatgames reload`.

---

## 5. Folia support (one jar: Spigot / Paper 1.8 → 26.x **and** Folia)

**Goal:** one ChatGames jar that keeps working on everything 1.9.9 supported (Spigot, Paper and forks, 1.8.8 → latest) and also runs on Folia. Folia is an optional code path chosen at runtime. It is not a separate build.

### What Folia is, in short
- Folia is a Paper fork with **no main thread**. The world is split into *regions*, and each region ticks on its own thread, in parallel.
- A special **global region** handles world-wide things such as weather, time and **console commands**.
- What this means for plugins:
  - **`Bukkit.getScheduler()` can't be used.** The `runTask*` methods and `BukkitRunnable` are unsupported on Folia.
  - There are four replacement schedulers:
    - **GlobalRegionScheduler:** for work not tied to a place, such as game timers, starting and ending games, and console commands.
    - **RegionScheduler:** for work at a *location*, such as dropping an item at a player's feet.
    - **EntityScheduler** (`player.getScheduler()`): for work on a *player or entity*, such as giving items, titles, sounds or player commands. The task follows the entity between regions.
    - **AsyncScheduler:** for work off the tick entirely, such as the database, the update checker and Discord webhooks.
  - **Events and commands run in parallel** on whichever region owns the player or entity. Two players racing in different areas fire `BlockBreakEvent` *at the same time on different threads*. Console commands run on the global region.
  - **Code on one region's thread must not touch another region's data.** Any state shared across regions has to be thread-safe.
  - A plugin must declare `folia-supported: true` in `plugin.yml` or Folia refuses to load it. Folia's docs stress that the flag alone is "not nearly enough".
  - APIs listed as broken on Folia: all scoreboard API, `Entity#teleport` (use `teleportAsync`), world load/unload, and parts of the respawn, portal and login APIs. ChatGames uses none of these today; keep it that way.
- **Folia builds only exist for modern Minecraft**: the current stable is 26.1.2, with 26.3 experimental, and Java 25 is required. Folia support therefore never collides with 1.8 support. On old servers the Folia path simply never activates.

### Design
1. **One scheduler abstraction, chosen at startup.**
   - Use **FoliaLib** (`com.tcoded:FoliaLib`, MIT), shaded and **relocated** into `org.nsider.chatgames.lib.folialib`, and excluded from `minimizeJar`.
   - It detects Folia, Paper or Spigot at runtime. On Spigot/Paper it maps every call to the normal Bukkit scheduler, so 1.8 behaviour is unchanged. It states support for 1.8.8+.
   - Wrap it behind our own small interface (`ChatGamesScheduler`) with these methods:
     - `global(Runnable)` and `globalLater(Runnable, long)`
     - `globalTimer(...)`
     - `atPlayer(Player, Runnable)` and `atLocation(Location, Runnable)`
     - `async(Runnable)` and `asyncLater(...)`
     - `cancelAll()`
   - Why wrap it: FoliaLib warns its API may change, so this keeps that churn in one file.
   - Alternative if FoliaLib ever causes trouble: write the two implementations by hand. A `BukkitScheduler` one would be compiled against spigot-api 1.8.8. A Folia one would live in a small extra Maven module compiled against `folia-api` and loaded only when Folia is detected. Detect Folia via a `Class.forName("io.papermc.paper.threadedregions.RegionizedServer")` check; this class name is common practice but isn't in the docs I read, so confirm it.
2. **Replace every scheduler call.** There are 42 call sites in 16 files today:
   - `TaskUtils` (10)
   - DB queries (13: `ManagementQueries` 7, `UpdateQueries` 3, `TopPlayerQueries` 2, `FastestTimeQueries` 1)
   - `GlobalGameController` (4), `DiscordListener` (3)
   - `MainTask`, `ExpirationTask` and `ChatGamesPlayerManager` (2 each)
   - one each in `ChatListener`, `GameBuilder`, `GameUtils`, `MemorizationMessage`, `RaceStarter` and `UpdateChecker`

   Make `TaskUtils` *become* the abstraction and ban direct `Bukkit.getScheduler()` usage. A grep check in the build, or a code-review rule, is enough.
3. **Assign each kind of work to the right scheduler:**

   | Work | Spigot/Paper | Folia |
   |---|---|---|
   | Game loop: `MainTask` interval, expiration timer, start/end game, winner selection | main thread | **global region** |
   | Broadcasting start, end and winner messages to every eligible player | main thread | global region decides; **per-player `atPlayer`** for title, sound and the world/toggle check |
   | Rewards: give item, `[playercmd]`, `[playermsg]` | main thread | **`atPlayer(winner)`** |
   | Rewards: `[consolecmd]`, milestone commands | main thread | **global region** (console commands must run there) |
   | Inventory full → drop item at player | main thread | **`atLocation(player location)`** |
   | Opening GUIs | main thread | `atPlayer` |
   | DB, update checker, Discord webhook, language download | async | async |
   | Race events (break, place, kill, fish, eat, craft, smelt) | main thread | already on the owning region; just update shared state safely |

4. **Make shared game state thread-safe.** This is the real work and it also fixes existing bugs: chat is already async on Spigot and Paper.
   - **Winner claiming must be atomic.** Today `GameController.addResult(..., results.size() + 1)` and the `size() < maxWinners` check race each other. Two players answering in the same moment can get the same place, or more than `maxWinners` can win.
     - Replace this with one `synchronized` (or lock-based) `tryClaimPlace(uuid)` that returns the place or `-1`.
     - Use the same pattern for races: `RaceHandler` has a plain `HashMap` and no win guard, while scores now arrive from several region threads at once.
   - Use `ConcurrentHashMap`/atomics for `RaceHandler`, `CooldownManager` (currently a `HashMap<Player, Long>`, which also leaks `Player` objects; key by UUID) and `InventoryManagement.playerPage`.
   - **The current game state needs to be safely visible across threads.** That covers `TimeUtils` statics, `ChatGames.inGame` and `GameManager.game`. Turn them into one immutable `ActiveGame` snapshot held in a `volatile`/`AtomicReference`, created and cleared only on the global region.
   - **Player data cache:** it's already a `ConcurrentHashMap`. Mutating fields on `ChatGamesPlayer` from several threads also needs atomics or `synchronized` setters, because points can change from chat, races and commands at the same time.
   - **`GameController`, `MilestoneManager` and the presets list** must not be mutated in place while another thread reads them. Swap in a new collection instead, for example on reload.
5. **Don't read other regions' data from the global region.** Reading `player.getWorld()` or `getLocation()` for every online player from the global thread crosses regions. On Folia, do the disabled-world, toggle and permission check *inside* the per-player `atPlayer` task. Toggle and permission data come from the cache, which is thread-safe.
6. **Hunt race spawner flag:** `setMetadata` on spawn and `hasMetadata` on death both run on the region that owns that entity, so they stay valid. Optionally switch to PersistentDataContainer on 1.14+, which is more robust than metadata, and keep metadata as the 1.8–1.13 fallback.
7. **PlaceholderAPI:** other plugins (scoreboards, tab) request placeholders from their own threads. Serve every placeholder from the in-memory cache, with **no DB call and no `.join()`**. 1.9.9 blocked on DB futures inside placeholders.
8. **InventoryManager (your library):** it must also stop using the Bukkit scheduler, and its inventory handling has to run on the player's region. Update it to accept the `ChatGamesScheduler` abstraction, or give it its own FoliaLib usage, and release it as 1.2.0 before ChatGames ships Folia support.
9. **`plugin.yml`:** add `folia-supported: true`. Spigot, Paper and old versions ignore unknown keys. Confirm that once on 1.8.8 during testing.
10. **`onDisable`:** `scheduler.cancelAll()`, then save players synchronously, then close Hikari. On Folia there's no main thread to "run later" on during shutdown.

### Folia testing
- Use Folia 26.x on Java 25 with at least 2 test accounts standing **far apart** (different regions). Check:
  - Both answer a chat game in the same tick: exactly one winner per place, and never more than `winners`.
  - Both race in different regions at the same time (mine and hunt): correct scores and exactly one race winner.
  - Rewards: items given, console and player commands run, and "inventory full" drops work.
  - Titles and sounds play, the GUI opens, and placeholders render in another plugin's scoreboard or tab.
  - `/chatgames reload` during a running game, and server stop during a running game: no errors, data saved.
- Watch the console for Folia thread-check errors ("not owned by current region" and similar). Each one is a call routed to the wrong scheduler.
- Re-run the normal Spigot/Paper matrix from 4.10 afterwards to confirm nothing changed there.

---

## 6. Best-practice cleanup (do alongside, not before)

- **Pick one style for shared services.** The code currently mixes static singletons (`ConfigManager`, `TopManager`, `GameController`, `RaceHandler`, query classes) with instances passed around via `plugin.getX()`. Prefer instances created in `onEnable`, passed through constructors, with no static mutable state. That also makes reload and unit tests possible.
- **Remove duplicates:**
  - Toggle and world checks: `GameManager.gamesIsToggled/worldEnabled` vs `GamePlayerManager.canPlay/isWorldEnabled`.
  - Handler dispatch: `GameHelper` vs `BuilderUtils.handleGameType`.
  - Command executors: `CommandUtils` vs `RewardManager.executeCommand` vs `MilestoneUtils`. `[player_as_op]` exists only in `RewardManager`. Granting OP temporarily is risky; consider dropping it.
- **Put the config key on `GameType`.** Give it a `configKey` field instead of the `getGameConfigName()` switch and string-built `ConfigPaths.valueOf(game.toUpperCase()+"_WINNERS")` calls. That removes bugs 7 and 8 entirely.
- **Database:** all DB I/O async, all reads from the cache, and a `schema_version` table with simple migrations (bug 4).
- **Copy-paste leftovers:** Javadocs mention "ElytraTweaks" and "ChatSystem" (`Messages`, `MessageKey`, `YamlConfig`, `InventoryManagement`, `MessageUtils`). `PermissionKey` is unused.
- **Tests:** add JUnit tests for the pure algorithm classes (math evaluator, sequence generator, scramble, letter hider, random length, `TextMatchUtils`). They need no server.
- **Logging:** use the plugin logger, not `e.printStackTrace()` or `Bukkit.getLogger()`.

---

## 7. Plan (suggested order)

1. **Get it compiling.**
   - Add XSeries and FoliaLib (both relocated) and delete the bundled `XMaterial`.
   - Fix the 36 API mismatches in 2A. Commit.
2. **Fix the foundations.**
   - `ConfigPaths` lazy suppliers, `ConfigManager` init order, `Messages` init order, the `QueryUtils.executeQuery` rewrite.
   - The `GameType.configKey` refactor and the version parser.
   - **The `ChatGamesScheduler` abstraction (5.1).** Move all 42 scheduler calls onto it now, so every later step is written Folia-safe from the start instead of retrofitted.
   - **The thread-safe game state (5.4):** atomic winner claiming and the `ActiveGame` snapshot.
3. **Bootstrap.**
   - Write `onEnable`/`onDisable` in a clear order: configs → version → DB + migrations → caches → managers → listeners → commands → PAPI → tasks.
   - Add `reloadAll()`.
4. **Player cache lifecycle.** Load on join (also for players already online at enable), save on quit, `saveAll` on disable plus periodically. Remove direct DB reads from game code. Add the stats the profile needs: wins per game, races won, global played/won, events, streak current/longest, challenges completed.
5. **Core game loop end to end.**
   - Fix bugs 7–16, award points and fastest times, wire milestones, add the time-expired message.
   - Replace the message classes with the `Msg` builder.
   - Generalise Memorize (content pools), add the new Sequence, and turn the old one into Math's missing-value mode.
   - Add configurable hints, the action-bar countdown, weighted no-repeat rotation and anti-macro.
6. **Commands + tab completion.** Port every 1.9.9 subcommand, then add `preset`, `challenge`/`claim` and per-game `fastest <game>`. Sync permissions in `plugin.yml`/`PermissionKey`.
7. **PlaceholderAPI expansion.** Port the 1.9.9 placeholders and add new ones. Serve them from the cache, with no `.join()` on DB futures; 1.9.9 blocked the main thread there.
8. **Races.**
   - Port the `ChatRaces` listener using XSeries types, then `loadRaces()`.
   - Build the win flow: rewards, messages, end event, `endRace`.
   - Keep the spawner-mob and creative-mode rules.
   - Custom values from the start command: `/chatgames <race> <material/mob> <amount>`, with tab completion (moved from step 6).
9. **New features.**
   - Challenges: types, daily/weekly reset, claims, and the `challenges`/`claim` commands (moved from step 6).
   - Streaks: three types, 7-day ladders.
   - Global games: a generic global mode over the existing generators, plus Global Race.
   - Presets, including `preset save`.
   - 9a. **Word Association** game, and **game events**: `events.yml`, the runner that pauses the rotation, event scoring and stats tables, the end leaderboard, schedules.
   - 9b. **Menus**: main, profile, challenges, streaks, admin, debug (mock answers + timing report).
   - 9c. **Translations**: a script that rebuilds `mine-translator` from Mojang's latest language assets; a version check so outdated files re-download. Pushing to the GitHub repo is your call.
10. **1.9.9 → 2.0.0 migration.**
    - DB: `fastest` column → fastest-times table.
    - Config: split the old `config.yml` into `games.yml`/`add-ons.yml`, rename `chatRaces.yml` → `races.yml`, and `shoppinglist` → `memorize`.
    - Back up the old files first.
11. **Folia hardening.**
    - InventoryManager 1.2.0 with scheduler support (5.8).
    - Per-player routing for broadcasts and rewards (5.3, 5.5).
    - `folia-supported: true`.
    - The Folia test pass (5, "Folia testing").
12. **Test matrix** (section 4.10, including Folia), then release.

Steps 1–5 give you a working 1.9.9-equivalent core. Steps 6–8 bring it to feature parity. Steps 9–11 are what makes 2.0.0 worth releasing.
