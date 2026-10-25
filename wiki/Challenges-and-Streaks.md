# Challenges and Streaks

## Challenges

Daily and weekly goals, the same for everyone, in `challenges.yml`. Progress counts as players
play. A finished challenge shows **Ready to claim** in `/chatgames challenges` (and the menu),
and `/chatgames claim` pays it. Rewards not claimed before the challenges reset are lost.

```yaml
timezone: ''          # e.g. Europe/Madrid or America/New_York; empty = the server's
daily:
  enabled: true
  mode: random        # random: 'amount' challenges drawn each day; fixed: all of them, every day
  amount: 3
  challenges:
    win_3:
      type: WIN_GAMES
      amount: 3
      name: '&eWarm Up'
      description: '&7Win %amount% chat games'
      icon: GOLD_NUGGET
      rewards:
        - '[consolecmd] give %player% iron_ingot 8'
weekly:
  enabled: true
  reset-day: MONDAY
  mode: fixed
  challenges:
    ...
```

### Challenge types

| Type | Counts | Extra setting |
|---|---|---|
| `WIN_GAMES` | chat game or race wins, in any place | |
| `WIN_GAME` | wins of one game or race | `game: unscramble` |
| `WIN_RACES` | race wins | |
| `PLAY_GLOBAL` | global games solved in time | |
| `WIN_GLOBAL` | global games finished in a rewarded place | |
| `FAST_WIN` | chat game wins in `seconds` or less (races don't count) | `seconds: 5` |
| `EARN_POINTS` | points earned from games | |
| `PLAY_EVENT` | game events taken part in | |

`amount` is how many are needed. In `name` and `description`, `%amount%`, `%game%` and
`%seconds%` are filled in; in rewards, `%player%` and `%challenge%`. Rewards are
[reward lines](Rewards-and-Milestones).

The ids (`win_3`, `fast_1`...) are saved with each player's progress: keep them to a-z, 0-9,
`-` and `_`. Renaming one restarts that challenge's progress. Other plugins can add challenge
types through the API.

`/chatgames challenges reset <player>` clears a player's progress.

## Streaks

Days in a row on which a player did something, in `streaks.yml`. Missing a day starts the
streak again from 1, and rewards not claimed by then are lost.

| Streak | Grows on a day the player... |
|---|---|
| `challenge` | completed every daily challenge |
| `daily` | won at least one game (or finished a global game in a rewarded place) |
| `global` | solved at least one global game in time |

Each day the streak grows pays that day's reward, claimed with `/chatgames claim`:

```yaml
after-day-7: repeat   # repeat: day 7's reward every day after; loop: back to day 1
daily:
  enabled: true
  name: '&6Daily win streak'
  rewards:
    1:
      - '[consolecmd] give %player% iron_ingot 2'
    ...
    7:
      - '[consolecmd] give %player% diamond 2'
```

`%player%`, `%streak%` (the name) and `%day%` are filled in. Days start in the time zone set in
challenges.yml.

`/chatgames streaks` shows a player's streaks, and
`/chatgames streak set <player> <challenge|daily|global> <days>` sets one.
