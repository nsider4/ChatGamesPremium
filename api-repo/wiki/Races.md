# Races

In a race, ChatGames announces a **target** and an **amount** ("Kill 3 Withers!") and the first
player to reach the amount wins. ChatGames runs the race: it picks the target and amount, announces
it, shows the countdown, keeps everyone's progress, decides the winner and gives the points and
rewards. **Your plugin only reports progress**: when a player does the thing, you call
`addRaceProgress`.

That means a race can count *anything* your plugin can detect: kills, votes, custom items, jobs,
quests, distance, money earned...

```java
GameType bossHunt = ChatGamesAPI.get().registerRace(this, RaceGame.builder("boss_hunt")
        .displayName("Boss Hunt")
        .icon("wither_skeleton_skull")
        .target("wither", 1, 2)          // target, then the amounts it can ask for
        .target("ender_dragon", 1)
        .build());

// In your listener, whenever a player kills a boss:
ChatGamesAPI.get().addRaceProgress(player, bossHunt, "wither", 1);
```

Keep the `GameType` that `registerRace` returns; you pass it to `addRaceProgress`.

## Targets and amounts

Every race has one or more **targets**, and each target has the **amounts** a race can ask for.
When the race starts, ChatGames picks a target and one of its amounts from races.yml, like
`WITHER: 2`. Targets are stored upper-case, and compared without case everywhere (`"wither"`,
`"WITHER"` and `"Wither"` are the same target).

The targets you give in the builder are **defaults**: they are written to races.yml the first time,
and from then on the owner decides which targets and amounts exist, and which rewards each pays.

### Races with a single target

If your race has no natural targets ("vote the most", "earn coins"), give it one target and report
progress with `null` as the target, which counts for whatever target is running:

```java
GameType voteRace = api.registerRace(this, RaceGame.builder("vote_race")
        .displayName("Vote Race")
        .target("votes", 1, 2, 3)
        .targetName(target -> "Votes")
        .build());

api.addRaceProgress(player, voteRace, null, 1);
```

## RaceGame.builder(id)

| Method | Default | What it does |
|---|---|---|
| `displayName(String)` | the id in title case | Name in menus, messages (`%game%`) and logs. |
| `icon(String material)` | `compass` | The menu icon (any material name, 1.8 to 26.x). |
| `target(String target, int... amounts)` | **at least one required** | A default target and the amounts it can ask for. Amounts below 1 are ignored. |
| `targetName(Function<String, String>)` | the target in title case (`ENDER_DRAGON` → `Ender Dragon`) | How a target is shown to players, as `%value%`. Gets the target upper-case. Return `null` to keep the default. Use it for nicer names or for your plugin's translated names. |
| `targetFilter(Predicate<String>)` | every name is valid | Which targets exist. Gets the target upper-case. Used for targets the owner writes in races.yml and for `/chatgames <race> <target>`; invalid ones are skipped with a console note. |
| `setting(String path, Object value)` | | A default for the race's races.yml section, e.g. `setting("timeToComplete_seconds", 300)` or `setting("points", 3)`. Your own settings work too. |
| `messages(start, win, expired)` | standard lines | The default messages.yml lines. `null` keeps the standard one. |
| `build()` | | Throws `IllegalStateException` if the id is malformed or there are no targets. |

### targetFilter and targetName together

Make the filter accept exactly what your plugin knows about, and the name show it nicely:

```java
RaceGame crateRace = RaceGame.builder("crate_race")
        .target("vote", 3, 5)
        .target("legendary", 1)
        .targetFilter(target -> crates.exists(target.toLowerCase()))       // your plugin's crate ids
        .targetName(target -> crates.displayName(target.toLowerCase()))   // "&6Legendary Crate"
        .build();
```

Then an owner can add any of your crates to races.yml, and `/chatgames crate_race epic 2` works for
every crate your plugin has.

## addRaceProgress

```java
void addRaceProgress(Player player, GameType race, String target, int amount)
```

- Does nothing unless **that race is running** right now. You can call it on every event without
  checking first; when no race of yours is running it costs almost nothing.
- `target` must match the running race's target (without case), or be `null` for any target.
- Does nothing for players who can't play: in creative mode (unless `creative-enabled: true` in
  races.yml), in a world where ChatGames is disabled, or who turned games off.
- `amount` below 1 does nothing.
- **Safe from any thread**, including async events and Folia region threads.
- The player who reaches the amount first wins; the race then ends and the winner gets the points
  and the rewards for that target and amount.
- When the owner runs your race as a **global race** (global_games.yml), the same calls add to
  the whole server's shared goal instead. You don't need to do anything different.

## Recipes

Each recipe is a complete listener. Register it with
`getServer().getPluginManager().registerEvents(listener, plugin)`.

### Kill anything: custom mobs, bosses, players

```java
public final class BossListener implements Listener {
    private final GameType bossHunt;

    public BossListener(GameType bossHunt) {
        this.bossHunt = bossHunt;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        String type = event.getEntity().getType().name();          // WITHER, ENDER_DRAGON...
        ChatGamesAPI.get().addRaceProgress(killer, bossHunt, type, 1);
    }
}
```

For your own custom mobs, pass your plugin's mob id as the target instead of the entity type.
(ChatGames' built-in **Hunt** race already counts vanilla mobs and MythicMobs mobs; make your own race
for anything else.)

### Votes (Votifier / NuVotifier)

```java
@EventHandler
public void onVote(VotifierEvent event) {
    Player player = Bukkit.getPlayerExact(event.getVote().getUsername());
    if (player != null) ChatGamesAPI.get().addRaceProgress(player, voteRace, null, 1);
}
```

### Something with an amount: money, XP, distance

Report the amount itself, not 1 per event:

```java
@EventHandler(ignoreCancelled = true)
public void onExp(PlayerExpChangeEvent event) {
    if (event.getAmount() > 0) {
        ChatGamesAPI.get().addRaceProgress(event.getPlayer(), xpRace, null, event.getAmount());
    }
}
```

For distance, count whole blocks (see the `travel` race in the
[example plugin](https://github.com/nsider4/ChatGames-API/tree/main/example)):

```java
@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
public void onMove(PlayerMoveEvent event) {
    Location from = event.getFrom(), to = event.getTo();
    if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ())) return;
    Player player = event.getPlayer();
    String mode = player.isSneaking() ? "sneak" : player.isSprinting() ? "sprint" : "walk";
    ChatGamesAPI.get().addRaceProgress(player, travel, mode, 1);
}
```

### Your plugin's own events

If your plugin fires its own events (a quest completed, a job level, a crate opened), the listener is
the same shape: find the player, choose the target, report the amount.

```java
@EventHandler
public void onCrateOpen(CrateOpenEvent event) {          // your plugin's event
    ChatGamesAPI.get().addRaceProgress(event.getPlayer(), crateRace, event.getCrateId(), 1);
}
```

## Built-in races

ChatGames ships these races; you don't need the API for them. Use them as models, or to see what's
already covered before you make your own:

| Race | Counts | Targets |
|---|---|---|
| `hunt` | mobs killed | entity types, and `mythic:<MobType>` with MythicMobs |
| `mine` | blocks broken | materials |
| `place` | blocks placed | materials |
| `fish` | items fished | materials |
| `eat` | food eaten | materials |
| `craft` | items crafted (shift-click counts every item) | materials |
| `furnace` | items taken out of a furnace | materials |
| `tame` | mobs tamed | entity types that can be tamed on the server's version |
| `shear` | mobs sheared | entity types |

## Message placeholders

In the start, win and expired messages of a race:

| Placeholder | Value |
|---|---|
| `%value%` | The target's name (from `targetName`). |
| `%amount%` | The amount to reach. |
| `%timeToComplete%` | The time limit in seconds. |
| `%winner%` | The winner's name (win message). |
| `%time%` | The winner's time in seconds. |
| `%game%` | The race's display name. |

The race's `titleMessage` and `subtitleMessage` in races.yml can use `%value%`, `%amount%` and
`%timeToComplete%`.

## Rewards for races

Race rewards are chosen per target **and** amount in races.yml, and point to named rewards in
rewards.yml (`races.rewards.<name>`). Your defaults give every target and amount `reward_1`:

```yaml
boss_hunt:
  enable: true
  timeToComplete_seconds: 180
  data:
    WITHER:
      '1':
        rewards:
        - reward_1
      '2':
        rewards:
        - reward_1
```

The owner can then pay more for a harder target, or add [your own reward actions](Reward-Actions) to
those rewards.
