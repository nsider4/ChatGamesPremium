# Threads and Folia

ChatGames runs on Spigot, Paper and Folia, and handles chat on the chat thread, so some of its work
happens off the main thread. This page says which API calls are safe where, and how to keep your
code safe on Folia.

## Calling the API

| Call | Where |
|---|---|
| `getCurrentGame` | any thread |
| `addRaceProgress`, `addChallengeProgress` | any thread |
| `getStats`, `addPoints` | any thread |
| `getTopPoints`, `getTopTimes` | any thread |
| `registerGame`, `registerRace`, `registerRewardAction`, `registerChallengeType`, `unregister` | main thread (normally in `onEnable`) |
| `startGame`, `stopGame`, `getGame`, `getGames` | main thread |

"Main thread" means the server thread on Spigot and Paper, and the **global region** on Folia.

The "any thread" calls are the ones you would call from listeners: async events (chat, Votifier,
your database callbacks) and, on Folia, region threads (every player and block event). Call them
directly; don't schedule them first.

## Code ChatGames calls

| Your code | Runs on |
|---|---|
| a chat game's `QuestionGenerator` | main thread / global region |
| a race's `targetName` and `targetFilter` | main thread / global region |
| a `RewardAction` | main thread / global region |
| event listeners | see the [Events](Events#overview) table; some are async |

## Folia in short

On Folia there is no single main thread. Each area of the world has its own thread, and the
**global region** handles things that don't belong to an area. The rule: **touch a player, an
entity or a block only on the thread that owns it.** Sending a player a message is fine from
anywhere; changing their inventory, health or location, or spawning an entity, is not.

Reward actions run on the global region, which owns no player. So a reward action that changes the
player must hand that work to the player's own thread.

### Running on the player's thread

If your plugin supports Folia, you build against the Paper API (1.20 or newer), which has
per-entity schedulers:

```java
private static final boolean FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");

private static boolean classExists(String name) {
    try {
        Class.forName(name);
        return true;
    } catch (ClassNotFoundException e) {
        return false;
    }
}

/** Runs the task where the player's data may be changed: their region on Folia, the main thread elsewhere. */
void runForPlayer(Player player, Runnable task) {
    if (FOLIA) {
        player.getScheduler().run(this, scheduled -> task.run(), null);
    } else if (Bukkit.isPrimaryThread()) {
        task.run();
    } else {
        Bukkit.getScheduler().runTask(this, task);
    }
}
```

Then, in a reward action:

```java
api.registerRewardAction(this, "token", (player, argument) ->
        runForPlayer(player, () -> player.getInventory().addItem(myTokenItem(1))));
```

A library like [FoliaLib](https://github.com/TechnicallyCoded/FoliaLib) gives you the same thing if
your plugin must also build against old Spigot versions.

### Async events

`ChatGamesAnswerEvent` and chat-game `ChatGamesWinEvent`s usually come from the chat thread, and
`ChatGamesPointsChangeEvent`, `ChatGamesChallengeCompleteEvent` and `ChatGamesStreakEvent` can come
from any thread. In those listeners:

- reading the event, changing it (`setPoints`, `setCancelled`) and sending messages is fine;
- for anything that touches the world or the player's state, use `runForPlayer` above (or
  `Bukkit.getScheduler().runTask` on Spigot/Paper);
- keep your own data thread-safe (`ConcurrentHashMap`, atomic counters).

## Declaring Folia support

If your plugin works on Folia, add `folia-supported: true` to plugin.yml. Without it, Folia won't load
your plugin, but ChatGames still works without your content.
