# ChatGames-Premium

ChatGames-Premium runs games in your server's chat: unscramble a word, solve a sum, answer a trivia
question, be the first to mine 16 stone. Winners get points, rewards and a place on the leaderboards.

One jar runs on Spigot, Paper and their forks from **1.8.8 to 26.x**, and on **Folia**.

## Start here

1. [Installation](Installation): requirements, first start, SQLite or MySQL, upgrading from 1.9.9.
2. [Commands and Permissions](Commands-and-Permissions): everything players and admins can type.
3. [Chat Games](Chat-Games) and [Races](Races): the games, and how to tune each one.
4. [Rewards and Milestones](Rewards-and-Milestones): what winners get.

## What's in it

| Feature | What it does | Page |
|---|---|---|
| **15 chat games** | Unscramble, Math, Trivia, Word Association, Memorize, Sequence... | [Chat Games](Chat-Games) |
| **9 races** | Hunt, Mine, Place, Fish, Eat, Craft, Furnace, Tame, Shear | [Races](Races) |
| **Rotation** | Starts a weighted random game every few minutes, with presets | [Rotation and Presets](Rotation-and-Presets) |
| **Global games** | The whole server works together towards one goal | [Global Games](Global-Games) |
| **Game events** | A series of games with event points and a final leaderboard | [Game Events](Game-Events) |
| **Challenges and streaks** | Daily and weekly challenges, 7-day streak ladders | [Challenges and Streaks](Challenges-and-Streaks) |
| **Menus** | Profile, leaderboards, challenges, admin and debug menus, all editable | [Menus](Menus) |
| **Placeholders** | Points, wins, times, leaderboards, streaks for PlaceholderAPI | [Placeholders](Placeholders) |
| **Discord** | Live game posts, results and leaderboards, by webhook or DiscordSRV | [Discord](Discord) |
| **Languages** | Item, block and mob names in 140+ languages; config files in 20 | [Languages](Languages) |

## The files

Everything lives in `plugins/ChatGames-Premium/`. After editing a file, run `/chatgames reload`.

| File | What's in it |
|---|---|
| `config.yml` | Storage, language, rotation timing, player counts, sounds, placeholder settings |
| `games.yml` | Each chat game's settings: on/off, weight, winners, points, time, hints, countdown |
| `words.yml` | The words, trivia questions, word associations and Memorize lists the games use |
| `races.yml` | Each race's targets, amounts and the rewards for each |
| `rewards.yml` | The rewards for every chat game, and the reward sets races use |
| `messages.yml` | Every message players see |
| `milestones.yml` | Commands run when a player reaches a points total |
| `presets.yml` | Named sets of games for the rotation |
| `global_games.yml` | Global games: triggers, goals, progress bar, rewards |
| `events.yml` | Game events: their steps, schedule, points and rewards |
| `challenges.yml` / `streaks.yml` | Daily/weekly challenges and streaks, plus the time zone |
| `menus.yml` | Every menu (more can go in the `menus/` folder) |
| `discord.yml` | Discord channels and every message posted |
| `add-ons.yml` | Game logs (console and file) |

## For developers

Plugins can add their own chat games, races, reward actions and challenge types, and listen to
game events. See the [ChatGames API wiki](https://github.com/nsider4/ChatGames-API/wiki).
