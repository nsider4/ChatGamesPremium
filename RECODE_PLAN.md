# ChatGames-Premium 2.0.0 Recode: Status & Plan

Analysis date: 2026-10-20. The baseline is `Downloads/ChatGames-Premium-1.9.9.jar`, decompiled. The recode is at commit `9cc611b` (2025-08-07) plus uncommitted work dated about 2025-08-13.

---

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
    - `[player_as_op]` was ported as-is from 1.9.9: a fake sender that claims OP. It's an anonymous `CommandSender` compiled against the 1.8 API, so on modern servers any newer interface method a command calls on it fails with `AbstractMethodError`. Vanilla commands may also reject a non-player sender. Test it on 26.x, or drop it in step 6.
  - Still stubbed until its step: PlaceholderAPI is only detected, not hooked (step 7). The full `onEnable` order comes in step 3.
- [ ] Step 2: foundations

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
4. **Player cache lifecycle.** Load on join (also for players already online at enable), save on quit, `saveAll` on disable plus periodically. Remove direct DB reads from game code.
5. **Core game loop end to end.** Fix bugs 7–16, award points and fastest times, add the time-expired message, and add the missing `memorize`/`sequence` messages.
6. **Commands + tab completion.** Port every 1.9.9 subcommand, then add `preset`, `challenge`/`claim` and per-game `fastest <game>`. Sync permissions in `plugin.yml`/`PermissionKey`.
7. **PlaceholderAPI expansion.** Port the 1.9.9 placeholders and add new ones. Serve them from the cache, with no `.join()` on DB futures; 1.9.9 blocked the main thread there.
8. **Races.**
   - Port the `ChatRaces` listener using XSeries types, then `loadRaces()`.
   - Build the win flow: rewards, messages, end event, `endRace`.
   - Keep the spawner-mob and creative-mode rules.
9. **New features.** Challenges (rules, rewards, reset), global games (scheduling, messages, rewards) and presets. Then decide on the GUI: build a small main menu or cut it from 2.0.0.
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
