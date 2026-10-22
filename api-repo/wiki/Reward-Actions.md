# Reward Actions

Every reward in ChatGames is a list of **reward lines**, and each line starts with an **action** in
square brackets. A reward action you register can be used in every one of those lists: chat-game
and race rewards (rewards.yml), challenge and streak rewards, global games and events.
(Milestones in milestones.yml are plain console commands, not reward lines.)
So one registration makes your plugin's rewards available everywhere.

```java
ChatGamesAPI.get().registerRewardAction(this, "coins", (player, argument) -> {
    int amount = Integer.parseInt(argument.trim());
    economy.deposit(player, amount);
});
```

A server owner then writes, for example in rewards.yml:

```yaml
  - '[coins] 250'
```

## The reward line format

```
[<chance>%~ ]<[action]> <argument>[ has:<permission>]
```

| Part | Example | Meaning |
|---|---|---|
| `[action]` | `[coins]` | Which action runs. Your action names work exactly like the built-in ones. |
| argument | `250` | The rest of the line, given to your action as a string. |
| `%player%` | `[coins] 250 %player%` | Replaced by the player's name before your action is called. |
| chance | `25.0%~ [coins] 1000` | Optional, at the very start. Lines without a chance always run; of the lines **with** one, a single line is drawn by those chances (if they add up to less than 100, sometimes none). |
| permission | `[coins] 500 has:vip.bonus` | Optional, at the very end: the line only runs if the player has the permission. |

ChatGames handles the chance and the permission check, and strips them, so your action only receives
the argument. Some reward lists also pick between whole rewards (`chance:`, `minimum-players:`);
that also happens before your action is called.

### Built-in actions

These names are taken, and show what owners already know:

| Action | Does |
|---|---|
| `[consolecmd] <command>` | Runs a command from the console. |
| `[playercmd] <command>` | Runs a command as the player. |
| `[player_as_op] <command>` | Runs a command as the player with op. |
| `[playermsg] <message>` | Sends the player a message (colour codes work). |
| `[broadcast] <message>` | Sends a message to everyone in the worlds where ChatGames is enabled. |

## registerRewardAction(owner, name, action)

- **name**: 1 to 32 lower-case letters, digits, `_` or `-`, without the brackets, e.g.
  `"coins"` or `"myplugin_crate"`. Owners may write it in any case: `[Coins]` works too.
  A name that is a built-in action or already registered throws `IllegalArgumentException`.
  Prefixing it with your plugin's name avoids clashes with other plugins.
- **action**: a `RewardAction`, `(Player player, String argument) -> { ... }`.
  - `argument` is the rest of the line after `[name]`, trimmed, with `%player%` filled in. It can
    be empty.
  - It runs on the main thread (the global region on Folia), even when the win happened on the chat
    thread. On Folia, use the player's scheduler for anything that touches the player or the world;
    see [Threads and Folia](Threads-and-Folia).
  - If it throws, ChatGames logs the error with your plugin's name and the line, and the other
    reward lines still run.

## Reading the argument

The argument is free text, so you decide its format. Keep it simple for owners and fail clearly:

```java
api.registerRewardAction(this, "crate", (player, argument) -> {
    // [crate] <crate id> [amount]
    String[] parts = argument.split(" ");
    if (parts.length == 0 || parts[0].isEmpty()) {
        getLogger().warning("[crate] needs a crate id, e.g. '[crate] vote 1'");
        return;
    }
    String crate = parts[0];
    int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
    crates.give(player, crate, amount);
});
```

Owners then write `- '[crate] legendary 2'`. Document your action's format for owners on your own
plugin page.

## Recipes

**Give a custom item:**

```java
api.registerRewardAction(this, "token", (player, argument) -> {
    int amount = argument.isEmpty() ? 1 : Integer.parseInt(argument);
    player.getInventory().addItem(myTokenItem(amount));
});
```

On Folia, the player's inventory must be changed on the player's own thread; wrap the body as shown
in [Threads and Folia](Threads-and-Folia#running-on-the-players-thread).

**Send a title** (anything else the player sees works the same way):

```java
api.registerRewardAction(this, "title", (player, text) ->
        player.sendTitle(ChatColor.translateAlternateColorCodes('&', text), ""));
```

(`sendTitle(String, String)` exists on 1.8.8; newer versions also have the overload with fade times.)

**Broadcast** (see the [example plugin](https://github.com/nsider4/ChatGames-API/tree/main/example)):

```java
api.registerRewardAction(this, "example_broadcast",
        (player, message) -> Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message)));
```

## Using your action as a default

Your [chat games](Chat-Games) can put your action in their default rewards:

```java
ChatGame.builder("capitals")
        .rewards("[coins] 100", "[playermsg] &aYou earned 100 coins!")
        // ...
```

## When your plugin is missing

When your plugin is disabled or removed, lines with your action stay in the owner's files. ChatGames
logs "Unknown reward action" for them when they run and skips them; nothing else breaks.
