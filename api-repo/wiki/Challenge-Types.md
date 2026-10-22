# Challenge Types

ChatGames has **daily and weekly challenges** (challenges.yml): "Win 3 chat games", "Earn 10
points"... Players see them in `/chatgames challenges` and the menu, and claim the rewards with
`/chatgames claim`. A **challenge type** you register lets server owners write challenges that count
something your plugin tracks, like "Kill 5 bosses this week" or "Vote 3 times today".

You do two things:

```java
// 1. In onEnable: register the type
ChatGamesAPI.get().registerChallengeType(this, "boss_kills");

// 2. Whenever it happens: add progress
ChatGamesAPI.get().addChallengeProgress(player, "boss_kills", 1);
```

The server owner does the rest in challenges.yml:

```yaml
weekly:
  challenges:
    bosses_5:
      type: boss_kills          # your type
      amount: 5
      name: '&cBoss Slayer'
      description: '&7Kill %amount% bosses this week'
      icon: WITHER_SKELETON_SKULL
      rewards:
        - '[consolecmd] give %player% diamond 5'
```

## How it behaves

- Your type works in **daily** and **weekly** challenges, in fixed and random mode, just like the
  built-in types. Progress, "Ready to claim", resets, the menu and `/chatgames claim` all work.
- `addChallengeProgress(player, type, amount)` adds `amount` to **every active challenge of that
  type** for that player, daily and weekly. Each challenge stops at its own `amount`.
- Progress for a type no active challenge uses is simply ignored, so you can always call it.
- When a challenge completes, the player is told, and `ChatGamesChallengeCompleteEvent` fires.
- **Safe from any thread.** `amount` below 1 does nothing. The type is not case-sensitive.

## registerChallengeType(owner, id)

- **id**: 2 to 32 lower-case letters, digits or `_`, starting with a letter, e.g. `boss_kills`.
- It can't be one of the built-in types or a type another plugin registered; that throws
  `IllegalArgumentException`.
- Prefix it with your plugin's name if it's generic (`myjobs_levels` rather than `levels`).

Built-in types, for reference (owners write them in capitals):

| Type | Counts |
|---|---|
| `WIN_GAMES` | chat games or races won, in any place |
| `WIN_GAME` | wins of one game, set with `game:` (custom games work here too) |
| `WIN_RACES` | races won |
| `PLAY_GLOBAL` | global games solved in time |
| `WIN_GLOBAL` | global games finished in a rewarded place |
| `FAST_WIN` | chat games won in `seconds:` or less |
| `EARN_POINTS` | points earned from games |
| `PLAY_EVENT` | game events taken part in |

Your [custom games and races](Chat-Games) already count for `WIN_GAMES`, `WIN_GAME`, `FAST_WIN` and the
others without any work; a challenge type is for things that happen **outside** ChatGames.

## Startup order

ChatGames reads challenges.yml while it enables, before your plugin has registered its type. It
doesn't warn about types it doesn't know yet: once the server has finished starting, it reads the
file again and your type is there. If your plugin is missing, owners get a console warning that the
challenge was skipped, and the other challenges keep working.

## Recipes

**Count a Bukkit event:**

```java
@EventHandler(ignoreCancelled = true)
public void onEnchant(EnchantItemEvent event) {
    ChatGamesAPI.get().addChallengeProgress(event.getEnchanter(), "items_enchanted", 1);
}
```

**Count an amount** (money earned, blocks travelled, minutes played):

```java
public void onPayout(Player player, double money) {     // called by your plugin
    ChatGamesAPI.get().addChallengeProgress(player, "money_earned", (int) money);
}
```

**A race and a challenge from the same action:** both calls are cheap, so report to both:

```java
ChatGamesAPI api = ChatGamesAPI.get();
api.addRaceProgress(player, bossHunt, bossType, 1);
api.addChallengeProgress(player, "boss_kills", 1);
```

**Tell owners what to write:** list your types and what they count on your plugin's page, with a
ready-to-paste challenges.yml snippet like the one at the top of this page.
