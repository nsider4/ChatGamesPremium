# ChatGames API

The developer API of [ChatGames-Premium](https://www.spigotmc.org/resources/107781/) 2.0.0+.
It lets your plugin:

- add **chat games** that play exactly like the built-in ones: rotation, weights, presets, events,
  challenges, `/chatgames <id>`, menus, leaderboards and placeholders;
- add **races**, where ChatGames runs the race and your plugin reports progress;
- add **reward actions** (`[myaction] argument`) to every reward list;
- add **challenge types** for challenges.yml;
- **start and stop** games, and read or change points, wins and leaderboards;
- listen to **events** for everything that happens in a game.

It works on Spigot and Paper 1.8.8 to 26.x and on Folia.

**📖 The [wiki](https://github.com/nsider4/ChatGames-API/wiki) explains every part step by step, with recipes:**
[Getting Started](https://github.com/nsider4/ChatGames-API/wiki/Getting-Started) ·
[Chat Games](https://github.com/nsider4/ChatGames-API/wiki/Chat-Games) ·
[Races](https://github.com/nsider4/ChatGames-API/wiki/Races) ·
[Reward Actions](https://github.com/nsider4/ChatGames-API/wiki/Reward-Actions) ·
[Challenge Types](https://github.com/nsider4/ChatGames-API/wiki/Challenge-Types) ·
[Events](https://github.com/nsider4/ChatGames-API/wiki/Events)

## Adding it

The API is on [JitPack](https://jitpack.io/#nsider4/ChatGames-API). Use `provided` (Maven) or
`compileOnly` (Gradle), and don't shade it: ChatGames is already on the server.

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.nsider4</groupId>
    <artifactId>ChatGames-API</artifactId>
    <version>2.0.0</version>
    <scope>provided</scope>
</dependency>
```

```kotlin
repositories { maven("https://jitpack.io") }
dependencies { compileOnly("com.github.nsider4:ChatGames-API:2.0.0") }
```

In your plugin.yml, add `depend: [ChatGames-Premium]` (or `softdepend`), then get the API in `onEnable`:

```java
ChatGamesAPI api = ChatGamesAPI.get();
```

Everything you register is removed when your plugin disables.

## A chat game

You return a `Question` with its answers. ChatGames handles the announcement, the timer, hints,
the winners, points, rewards and stats.

```java
api.registerGame(this, ChatGame.builder("capitals")
        .displayName("Capitals")
        .icon("map")
        .setting("timeToGuess_seconds", 30)
        .generator(context -> Question.builder("Paris")
                .display("France")            // %selection% in the messages
                .revealHints()                 // "P____", "Pa___"...
                .build())
        .build());
```

The first time a game is registered, its sections are added to games.yml, messages.yml and
rewards.yml, using your defaults. After that, the server owner's edits win. `QuestionContext`
gives the generator the game's games.yml section, its messages, words.yml, the command arguments
and a random source.

`Question.builder` also takes more answers (`answer`), placeholders for your messages
(`placeholder`), your own hints (`hints`) and a hoverable or clickable text (`component`).

## A race

```java
GameType travel = api.registerRace(this, RaceGame.builder("travel")
        .target("walk", 50, 100)       // target and the amounts it can ask for
        .target("sprint", 100, 200)
        .build());

// In your own listener:
api.addRaceProgress(player, travel, "walk", 1);
```

`addRaceProgress` does nothing unless that race is running for that target and the player can
play. It is safe from any thread.

## Rewards and challenges

```java
// rewards.yml: - '[crate] vote'
api.registerRewardAction(this, "crate", (player, argument) -> giveCrate(player, argument));

// challenges.yml: type: boss_kills
api.registerChallengeType(this, "boss_kills");
api.addChallengeProgress(player, "boss_kills", 1);
```

## Events

All in `org.nsider.chatgames.api.event`:

| Event | When | Can change |
|---|---|---|
| `ChatGamesStartEvent` | a game or race is about to start | cancel |
| `ChatGamesAnswerEvent` | a player answers (often async) | cancel, to ignore the answer |
| `ChatGamesWinEvent` | a player wins a place, before points and rewards | points, give rewards |
| `ChatGamesEndEvent` | a game or race ended | |
| `ChatGamesGlobalEndEvent` | a global game ended | |
| `ChatGamesEventStartEvent` / `ChatGamesEventEndEvent` | a game event (events.yml) | |
| `ChatGamesChallengeCompleteEvent` | a player finished a challenge | |
| `ChatGamesStreakEvent` | a player's streak grew | |
| `ChatGamesPointsChangeEvent` | a player's points changed | |

## Threads

Unless the Javadoc says "Any thread", call the API on the main thread (the global region on
Folia). Reward actions run there too; on Folia, use the player's scheduler for anything that
touches the player or the world.

## Example plugin

[`example/`](example) is a complete plugin with a chat game, a race, a reward action, a challenge
type and an event listener. Build it with `mvn package` in that folder.
