# ChatGames API

The ChatGames API lets your plugin add content to [ChatGames-Premium](https://www.spigotmc.org/resources/107781/)
(2.0.0 and newer) and react to what happens in it. Whatever you add plays like the built-in content:
server owners configure it in the same files, it shows in the same menus and leaderboards, and it
works from Spigot 1.8.8 to 26.x and on Folia.

## What you can make

| You want to... | Use | Page |
|---|---|---|
| Ask players something in chat (a quiz, a puzzle, a reaction test, a "click first" button) | a **chat game** | [Chat Games](Chat-Games) |
| Have players race to do something in the world (kill bosses, vote, catch custom fish, walk...) | a **race** | [Races](Races) |
| Give your plugin's rewards (crates, keys, coins, cosmetics) from any ChatGames reward list | a **reward action** | [Reward Actions](Reward-Actions) |
| Let daily and weekly challenges count something your plugin tracks | a **challenge type** | [Challenge Types](Challenge-Types) |
| React to games: anti-cheat, Discord messages, bonus points, statistics | **events** | [Events](Events) |
| Start or stop games, read stats, give points, show leaderboards | **control and data** | [Games and Player Data](Games-and-Player-Data) |

## Your first game in 2 minutes

1. Add the API to your build and `depend: [ChatGames-Premium]` to plugin.yml ([Getting Started](Getting-Started)).
2. Register a game in `onEnable`:

```java
@Override
public void onEnable() {
    ChatGamesAPI.get().registerGame(this, ChatGame.builder("capitals")
            .displayName("Capitals")
            .generator(context -> Question.builder("Paris")
                    .display("What is the capital of France?")
                    .build())
            .build());
}
```

3. Start the server and type `/chatgames capitals`. The game is also in the automatic rotation, the
   menus, `/chatgames top`, the placeholders and the challenges, with no extra code.

## All pages

- **Basics:** [Getting Started](Getting-Started) · [Threads and Folia](Threads-and-Folia)
- **Adding content:** [Chat Games](Chat-Games) · [Races](Races) · [Reward Actions](Reward-Actions) · [Challenge Types](Challenge-Types)
- **Reacting and reading:** [Events](Events) · [Games and Player Data](Games-and-Player-Data)
- **Reference:** [What Server Owners Configure](What-Server-Owners-Configure) · [Troubleshooting](Troubleshooting)
- **Full example:** the [`example/`](https://github.com/nsider4/ChatGames-API/tree/main/example) plugin in the repository uses every part of the API.
