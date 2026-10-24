# Games and Player Data

Besides adding content, the API lets you control games and read or change ChatGames' data.

## Finding games: GameType

Every game, built-in or added, is a `GameType`.

```java
GameType math = GameType.MATH;                       // built-in games are constants
GameType capitals = api.getGame("capitals");         // any game by id (null if there is none)
List<GameType> all = api.getGames();                 // every game, built-in first
```

| Method | Returns |
|---|---|
| `getId()` | the id, e.g. `"word_association"` or `"capitals"` |
| `name()` | the id in capitals, e.g. `"WORD_ASSOCIATION"` (as in logs) |
| `getConfigKey()` | its section name in the config files (the id) |
| `getKind()` | `CHAT`, `RACE` or `GLOBAL_ONLY` (Count) |
| `isRace()` / `isGlobalOnly()` | shortcuts for the kind |
| `isBuiltIn()` | `true` for the games ChatGames comes with |
| `getOwner()` | the plugin that added it (`"ChatGames"` for built-in ones) |

`GameType.fromName(String)` also finds a game by id, ignoring case. Compare game types with `==`.

Built-in constants: `UNSCRAMBLE`, `MATH`, `TRIVIA`, `REACTION`, `CLICKABLE`, `RANDOM`, `UNREVERSE`,
`VARIABLE`, `GUESS_THE_NUMBER`, `MEMORIZE`, `SEQUENCE`, `FILLOUT`, `HOVERABLE`, `WORD_ASSOCIATION`,
`COUNT`, and the races `HUNT`, `PLACE`, `MINE`, `FISH`, `EAT`, `CRAFT`, `FURNACE`, `TAME`, `SHEAR`.
`isMobRace()` is true for the built-in races that target a mob (`HUNT`, `TAME`, `SHEAR`), `isMaterialRace()` for the
other built-in ones.

## Running games

```java
ChatGamesAPI api = ChatGamesAPI.get();

GameType running = api.getCurrentGame();          // null when nothing runs. Any thread.
boolean started = api.startGame(GameType.TRIVIA); // as /chatgames trivia
api.startGame(GameType.MINE, "diamond_ore", "5"); // a race with a target and an amount
boolean stopped = api.stopGame();                 // as /chatgames stop: ends it with no winner
```

- `startGame(game, args...)` returns `false` if a game is already running, the game can't start
  (e.g. a race target that doesn't exist) or a plugin cancelled `ChatGamesStartEvent`. `args` are
  what an admin would type after `/chatgames <game>`: a race's target and amount, or, for
  [your chat games](Chat-Games#letting-admins-choose-the-question), whatever your generator reads
  with `getArgs()`.
- `stopGame()` returns `false` if nothing was running.
- Both run on the main thread (the global region on Folia).

## Player stats

```java
PlayerStats stats = api.getStats(player);    // online players only
if (stats != null) {
    int points = stats.getPoints();
    long wins = stats.getWins();                       // every chat game and race
    long mathWins = stats.getWins(GameType.MATH);
    double best = stats.getFastest(GameType.MATH);     // seconds; 0 if they never won it
    boolean playing = stats.isPlaying();               // false if they turned chat games off
}
```

`getStats` returns `null` while the player's data is still loading (just after they join). It is a
snapshot: read it again for newer numbers. Any thread.

## Points

```java
api.addPoints(player, 50);     // give
api.addPoints(player, -20);    // take away
```

- Works for **offline players** too (any `OfflinePlayer`): their data is loaded, changed and saved.
- Milestones (milestones.yml) run as they would for points won in games.
- Fires `ChatGamesPointsChangeEvent`.
- Any thread.

## Leaderboards

```java
List<TopEntry> top = api.getTopPoints(10);                      // most points
List<TopEntry> fastestMath = api.getTopTimes(GameType.MATH, 5); // fastest wins of one game
List<TopEntry> fastest = api.getTopTimes(null, 5);              // fastest wins of any game

for (TopEntry entry : top) {
    sender.sendMessage(entry.getRank() + ". " + entry.getName() + " - " + (int) entry.getValue());
}
```

`TopEntry` has `getRank()` (1 is the top), `getName()` (the last known name), `getUuid()` and
`getValue()` (points, or seconds on a time board). The lists come from ChatGames' cached
leaderboard, rebuilt every `refresh-seconds` (config.yml) and after games, so they are cheap to call
often, even from a scoreboard or hologram. They may hold fewer entries than you asked for. Any
thread.

## Placeholders

Your plugin doesn't need to do anything for PlaceholderAPI: ChatGames' placeholders work for added
games too, using their id. For example:

| Placeholder | Value |
|---|---|
| `%chatgames_points%` | the player's points |
| `%chatgames_wins_<id>%` | the player's wins of that game, e.g. `%chatgames_wins_capitals%` |
| `%chatgames_fastest_<id>%` | the player's best time in that game |
| `%chatgames_fastest_<id>_<n>%` / `%chatgames_fastest_<id>_time_<n>%` | the n-th fastest player of that game, and their time |

The full list is in ChatGames' config.yml.
