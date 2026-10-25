# Placeholders

With [PlaceholderAPI](https://www.spigotmc.org/resources/6245/) installed, these work in any
plugin (scoreboards, holograms, tab lists) and in ChatGames' own menus. Every 1.9.9 placeholder
still works.

`<game>` is a game's name as in games.yml or races.yml (`unscramble`, `guess_the_number`, `mine`);
`<n>` is a place (1, 2, 3...).

## You

| Placeholder | Value |
|---|---|
| `%chatgames_points%` | your points |
| `%chatgames_wins%` | your wins, all games |
| `%chatgames_wins_<game>%` | your wins of one game |
| `%chatgames_race_wins%` | your race wins |
| `%chatgames_global_played%` / `%chatgames_global_won%` | global games you took part in / were rewarded in |
| `%chatgames_toggle%` / `%chatgames_sound_toggle%` | whether you have games / sounds on |
| `%chatgames_events_played%`, `%chatgames_event_wins%`, `%chatgames_event_best_place%` | game events (best place 0 = none yet) |
| `%chatgames_personal_best%` | your fastest time in any chat game |
| `%chatgames_fastest_<game>%` | your fastest time in one game (races too) |
| `%chatgames_show_current_pos_top%` | your place on the points leaderboard |
| `%chatgames_show_current_pos_fastest%` | your place on the fastest leaderboard |

## Leaderboards

| Placeholder | Value |
|---|---|
| `%chatgames_top_<n>%` / `%chatgames_points_top_<n>%` | name / points of the n-th player by points |
| `%chatgames_total_points%` | every player's points together |
| `%chatgames_fastest_<n>%` / `%chatgames_fastest_time_<n>%` | name / time of the n-th fastest (chat games) |
| `%chatgames_fastest_<game>_<n>%` / `%chatgames_fastest_<game>_time_<n>%` | the same for one game |

## Right now

| Placeholder | Value |
|---|---|
| `%chatgames_game_running%` | whether a game is running |
| `%chatgames_current_game%` | the game running |
| `%chatgames_next_game_in%` | seconds until the rotation's next game |
| `%chatgames_race_target%`, `%chatgames_race_amount%`, `%chatgames_race_progress%` | the running race, and your progress in it |

## Global games

| Placeholder | Value |
|---|---|
| `%chatgames_global_running%` | whether a global game is running |
| `%chatgames_global_progress%` / `%chatgames_global_goal%` | the server's progress / the goal |
| `%chatgames_global_percent%` | the percent of the goal reached |
| `%chatgames_global_contribution%` | what you have put into the running global game |
| `%chatgames_global_time_left%` | seconds left |
| `%chatgames_contributions%` | everything you have put into global games, all time |

## Challenges and streaks

| Placeholder | Value |
|---|---|
| `%chatgames_daily_completed%` / `%chatgames_daily_total%` | today's challenges done / in total |
| `%chatgames_weekly_completed%` / `%chatgames_weekly_total%` | this week's |
| `%chatgames_challenges_completed%` | challenges completed, all time |
| `%chatgames_streak_<type>%` / `%chatgames_streak_<type>_longest%` | current / longest streak; `<type>` is `challenge`, `daily` or `global` |

## Game events

| Placeholder | Value |
|---|---|
| `%chatgames_event_running%`, `%chatgames_event_name%` | whether an event is running, and its name |
| `%chatgames_event_step%` / `%chatgames_event_steps%` | the game it's on / how many it has |
| `%chatgames_event_points%`, `%chatgames_event_place%` | your points and place in the running event |

## Settings

```yaml
# config.yml
placeholders:
  refresh-seconds: 60     # how often the leaderboard placeholders are rebuilt
  empty-value: ''         # shown for an empty leaderboard place, no best time, no game running
```

Leaderboards are also rebuilt after every game with a winner. Your own points, wins and times
are always live.
