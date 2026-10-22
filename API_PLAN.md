# Developer API, MythicMobs and custom sounds (2.0.0)

Approved 2026-10-21, with one change to the original plan: no games without code (games need their
own logic). Everything ships in 2.0.0, not 2.1.0. The plugin itself also supports MythicMobs.

## Decisions

- **GameType becomes a registry class.** It stays `org.nsider.chatgames.enums.GameType`, with the same
  constant names, so the 62 files that use it (and 1.9.9 listeners) keep compiling. It gets `values()`,
  `fromName()` and `register()`/`unregister()`. Built-in games are registered like custom ones would be.
  The two `switch` statements and the two `EnumMap`s on it become if-chains and HashMaps.
- **Where the API lives:** in this repo, in `org.nsider.chatgames.api`, `org.nsider.chatgames.events` and
  `GameType`. Those files may only import Bukkit and the JDK. `ApiBoundaryTest` enforces that.
  `scripts/publish-api.py` copies them into the public `nsider4/ChatGames-API` repo, which builds alone
  on JitPack, so the plugin repo stays private.
- **Custom games get what built-in ones get:** the rotation, weights, presets, `/chatgames <game>`,
  `chatgames.start.<id>`, menus, leaderboards, `%chatgames_wins_<id>%`, events, challenges, and the
  global question stream. Their default sections are added to games.yml, messages.yml, rewards.yml
  and menus.yml the first time, and owners can edit them after that.
- **MythicMobs is read by reflection.** This supports MythicMobs 5 (`io.lumine.mythic`) and 4
  (`io.lumine.xikage.mythicmobs`), with no compile dependency, and is a softdepend.

## Phases

### A. Custom sounds and MythicMobs (plugin)
- [x] GameSounds: a namespace other than `minecraft:` is played as written (resource packs).
- [x] Menus `[sound]`: InventoryManager 2.0.2 plays unknown or namespaced sounds as written.
- [x] `MythicHook`: is it installed, an entity's mob type, mob names, display names.
- [x] Hunt targets `mythic:<MobType>` in races.yml, commands, tab completion and global races.
- [x] `count-mythic-mobs` in hunt, so MythicMobs zombies can stop counting as ZOMBIE.
- [ ] races.yml example and comment, translated into the 9 languages.

### B. Registry (no visible change)
- [ ] GameType class: Kind {CHAT, RACE, GLOBAL_ONLY}, the owning plugin, and a display name.
- [ ] Remove the enum-only code (switches, EnumMap, `ordinal`, `valueOf`).
- [ ] RoundGenerators: look up the registered generator.
- [ ] Default config sections for a newly registered game; permissions registered at runtime.
- [ ] Unregister when the owning plugin disables: stop it if it's running, drop it from the rotation.

### C. API
- [ ] `ChatGamesAPI.get()`.
- [ ] Custom chat games: `ChatGame.builder(id)`, a `QuestionGenerator` returning a `Question`
      (answers, display, hints, placeholders, a clickable component).
- [ ] Custom races: `RaceGame.builder(id)` (target validation and display name), and
      `api.raceProgress(player, id, target, amount)`.
- [ ] Reward actions: `api.registerRewardAction("myaction", (player, argument) -> ...)`.
- [ ] Challenge types: `api.registerChallengeType(id)` and `api.challengeProgress(player, id, amount)`.
- [ ] Control: `start`, `stop`, `getCurrentGame`, `isRunning`.
- [ ] Data: points, wins and best times for online players; `addPoints`/`removePoints`; the top N.

### D. Events
- [ ] `ChatGamesAnswerEvent`: async and cancellable (anti-cheat).
- [ ] `ChatGamesWinEvent`: per winner, before rewards; points can be changed, rewards cancelled.
- [ ] `ChatGamesChallengeCompleteEvent`, `ChatGamesStreakEvent`, `ChatGamesPointsChangeEvent`.
- [ ] Fix `ChatGamesEndEvent.getTime` (it returned 0 for players who didn't place; now null).

### E. Example, docs, publishing
- [ ] An example plugin: one chat game, one race (MythicMobs-free), one reward action, one challenge type.
- [ ] A README for the API repo; release notes; the RECODE_PLAN note.
- [ ] Test on Paper 1.21.1, Spigot 1.8.8 and Folia with the example plugin loaded.
- [ ] Create `nsider4/ChatGames-API`, push it and check the JitPack build.

## Progress log
- 2026-10-21: plan written; phase A sounds done (InventoryManager 2.0.2 on JitPack).
- 2026-10-21: MythicMobs Hunt targets tested live on Paper 1.21.1 + MythicMobs 5.7.2 (both count-mythic-mobs values). Translations of the new comments are batched for the end.
