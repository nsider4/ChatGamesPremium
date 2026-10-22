# Events

ChatGames fires Bukkit events for everything that happens in a game. Listen to them like any other
event; they are in the package `org.nsider.chatgames.api.event`.

```java
public final class MyListener implements Listener {

    @EventHandler
    public void onWin(ChatGamesWinEvent event) {
        event.getPlayer().sendMessage("Well played!");
    }
}
```

They fire for **every** game: built-in ones and the ones plugins add. Use `event.getType()` (a
`GameType`) to tell games apart: `event.getType() == GameType.MATH`, `event.getType().isRace()`,
`event.getType().getId().equals("capitals")`.

## Overview

| Event | When | Can change | Thread |
|---|---|---|---|
| [`ChatGamesStartEvent`](#chatgamesstartevent) | a chat game, race or global game is about to be announced | cancel | main |
| [`ChatGamesAnswerEvent`](#chatgamesanswerevent) | a player typed a correct answer, before they get a place | cancel | usually async |
| [`ChatGamesWinEvent`](#chatgameswinevent) | a player won a place, before points and rewards | points, rewards | async (chat games) / main (races) |
| [`ChatGamesEndEvent`](#chatgamesendevent) | a game ended: won, timed out or stopped | | main |
| [`ChatGamesGlobalEndEvent`](#chatgamesglobalendevent) | a global game ended, after `ChatGamesEndEvent` | | main |
| [`ChatGamesEventStartEvent`](#chatgameseventstartevent) | a game event (events.yml) is about to start | cancel | main |
| [`ChatGamesEventEndEvent`](#chatgameseventendevent) | a game event ended | | main |
| [`ChatGamesChallengeCompleteEvent`](#chatgameschallengecompleteevent) | a player completed a challenge | | any |
| [`ChatGamesStreakEvent`](#chatgamesstreakevent) | a player's streak grew by a day | | any |
| [`ChatGamesPointsChangeEvent`](#chatgamespointschangeevent) | a player's points changed | | any |

"Main" means the main thread, or the global region on Folia. Events marked async or any return true
from `isAsynchronous()` when they are fired off the main thread. In those listeners, don't touch the
world, entities or inventories directly; schedule that work (see [Threads and Folia](Threads-and-Folia)).
Reading the event, sending messages and updating your own thread-safe data is fine.

## Events in the order of a chat game

```
ChatGamesStartEvent        (cancellable)
  ChatGamesAnswerEvent     (once per correct answer, cancellable)
  ChatGamesWinEvent        (once per winner)
  ChatGamesPointsChangeEvent, ChatGamesChallengeCompleteEvent, ChatGamesStreakEvent  (as they happen)
ChatGamesEndEvent
ChatGamesGlobalEndEvent    (global games only)
```

---

## ChatGamesStartEvent

Fired just before a game is announced. Nothing has been shown to players yet. Cancelling it stops
the game from starting.

| Method | Returns |
|---|---|
| `getType()` | the game |
| `hasAnswer()` / `getAnswer()` | the round's answer; `null` for races; for global games, a description of the goal |
| `setCancelled(boolean)` | `true` to keep the game from starting |

```java
// No chat games while a boss fight is on
@EventHandler
public void onStart(ChatGamesStartEvent event) {
    if (bossFightRunning && !event.getType().isRace()) event.setCancelled(true);
}
```

## ChatGamesAnswerEvent

Fired when a player types a **correct** answer, before they get a place. Cancelling it ignores the
answer, as if it were wrong; the player can still answer again.

| Method | Returns |
|---|---|
| `getPlayer()` | who answered |
| `getType()` | the game |
| `getAnswer()` | what they typed |
| `getMillis()` | milliseconds from the round opening to the answer |
| `setCancelled(boolean)` | `true` to ignore the answer |

Usually fired from the chat thread (async).

```java
// Anti-cheat: ignore answers from players your plugin has flagged
@EventHandler
public void onAnswer(ChatGamesAnswerEvent event) {
    if (flagged.contains(event.getPlayer().getUniqueId())) event.setCancelled(true);
}
```

ChatGames already ignores answers faster than the game's `min-answer-ms` (400 ms by default), so you
don't need to check reading speed yourself.

## ChatGamesWinEvent

Fired for each winner of a chat game or race, **before** they get points and rewards. You can change
the points and skip the rewards. The win itself always counts (stats, leaderboards, challenges).

| Method | Returns |
|---|---|
| `getPlayer()` | the winner |
| `getType()` | the game |
| `getPlace()` | 1 for the first winner; races have one winner |
| `getSeconds()` | seconds from the start to the win |
| `getPoints()` / `setPoints(int)` | the points the win pays; 0 for none |
| `isGiveRewards()` / `setGiveRewards(boolean)` | `false` to skip the rewards from rewards.yml or races.yml |

```java
// Double points for players with a booster
@EventHandler
public void onWin(ChatGamesWinEvent event) {
    if (event.getPlayer().hasPermission("myplugin.booster")) {
        event.setPoints(event.getPoints() * 2);
    }
}
```

## ChatGamesEndEvent

Fired when a chat game, race or global game ends: with winners, when time runs out, or when an
admin stops it. Rewards have been given by then.

| Method | Returns |
|---|---|
| `getType()` | the game |
| `hasAnswer()` / `getAnswer()` | the round's answer; `null` for races |
| `hasWinner()` | whether anyone won |
| `getWinners()` | `List<PlayerResult>` by place; for global games, every contributor |
| `getResult(UUID)` | that player's `PlayerResult`, or `null` if they didn't place |
| `getTime(Player)` | that player's time in seconds, or `null` if they didn't place |

`PlayerResult` has `getUuid()`, `getPlace()`, `getTime()` (seconds) and `getPlayer()` (an `OfflinePlayer`).

```java
// Post results to your Discord bot
@EventHandler
public void onEnd(ChatGamesEndEvent event) {
    if (!event.hasWinner()) return;
    PlayerResult first = event.getWinners().get(0);
    discord.send(first.getPlayer().getName() + " won " + event.getType().getId()
            + " in " + first.getTime() + "s");
}
```

## ChatGamesGlobalEndEvent

Fired when a global game ends, after `ChatGamesEndEvent`.

| Method | Returns |
|---|---|
| `getType()` | the game |
| `getOutcome()` | `COMPLETED` (goal reached), `FAILED` (time ran out, or a count broke too often) or `STOPPED` (an admin stopped it) |
| `isCompleted()` | `getOutcome() == COMPLETED` |
| `getProgress()` / `getGoal()` | how far the server got, and the goal |
| `getMinContribution()` | the least a player had to contribute to be rewarded |
| `getContributors()` | `List<Contributor>`, most first; each has `getUuid()`, `getAmount()`, `getPlace()` |

## ChatGamesEventStartEvent

Fired when a game event from events.yml is about to start, by command or by its schedule. Cancelling
it keeps the event from starting.

| Method | Returns |
|---|---|
| `getEventId()` | the event's id in events.yml |
| `getEventName()` | its display name |
| `isScheduled()` | `true` if its schedule started it, `false` for a command |

## ChatGamesEventEndEvent

Fired when a game event ends.

| Method | Returns |
|---|---|
| `getEventId()` / `getEventName()` | which event |
| `wasStopped()` | `true` if an admin stopped it before its last game (no standings, no rewards) |
| `getStandings()` | `List<Standing>` best first; each has `getUuid()`, `getPoints()` (event points), `getPlace()` |

## ChatGamesChallengeCompleteEvent

Fired when a player completes a daily or weekly challenge. The reward is claimed later with
`/chatgames claim`.

| Method | Returns |
|---|---|
| `getPlayer()` | the player |
| `getChallengeId()` | its id in challenges.yml |
| `getChallengeName()` | its name |
| `getPeriod()` | `"daily"` or `"weekly"` |

## ChatGamesStreakEvent

Fired when one of a player's streaks (streaks.yml) grows by a day.

| Method | Returns |
|---|---|
| `getPlayer()` | the player |
| `getStreak()` | the streak's type in streaks.yml, e.g. `"daily_win"` |
| `getDays()` | how many days in a row it is now |

## ChatGamesPointsChangeEvent

Fired **after** a player's points change: from a game, an admin command or a plugin. The player may
be offline (their data was loaded to change it), so it gives a UUID.

| Method | Returns |
|---|---|
| `getUuid()` | the player |
| `getBefore()` / `getAfter()` | the points before and after |

```java
// Keep a scoreboard up to date
@EventHandler
public void onPoints(ChatGamesPointsChangeEvent event) {
    Player player = Bukkit.getPlayer(event.getUuid());
    if (player != null) scoreboard.setPoints(player, event.getAfter());
}
```

(On Folia, update the scoreboard on the player's thread.)
