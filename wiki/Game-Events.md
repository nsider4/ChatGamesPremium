# Game Events

An event is a series of games played back to back, like a tournament. Each game hands out
**event points** by place; when the last one ends, the players with the most event points win
the event's rewards. While an event runs, the normal rotation and global games wait.

Events are in `events.yml`.

## An example

```yaml
events:
  weekend_cup:
    name: '&6&lWeekend Cup'
    schedule:
      - 'SATURDAY 20:00'
    minimum-players: 3
    pause-seconds: 15
    points: [5, 3, 1]            # event points for 1st, 2nd, 3rd in each game
    participation-points: 1      # for anyone else who makes it
    steps:
      - game: unscramble
        winners: 3
      - game: math
        winners: 3
        time: 25
      - game: word_association
        global: true
        settings:
          difficulty: hard
      - game: mine
        global: true
        time: 120
        args: 'stone 100'
    rewards:
      1:
        - '[consolecmd] give %player% diamond 5'
      2:
        - '[consolecmd] give %player% diamond 3'
      participation:
        - '[consolecmd] give %player% iron_ingot 3'
```

## Settings

| Setting | Meaning |
|---|---|
| `name` | shown to players (colour codes work) |
| `schedule` | when it starts by itself: `'SATURDAY 20:00'`, `'DAILY 18:30'`; leave it out for command only. The time zone is the one in challenges.yml |
| `minimum-players` | players needed for the schedule to start it (the command ignores this) |
| `pause-seconds` | the break between games |
| `points` | event points for 1st, 2nd, 3rd... in each game |
| `participation-points` | event points for anyone else who scores |
| `rewards` | reward lines by final place, and `participation` for everyone else who scored |

Each step:

| Setting | Meaning |
|---|---|
| `game` | the game or race, as in games.yml / races.yml |
| `global` | `true` plays it as a [global game](Global-Games). Count is always global |
| `time` | seconds to answer or finish, instead of the game's own |
| `winners` | how many can win a chat game, instead of the game's own |
| `args` | the values, as after `/chatgames <game>`, e.g. `'stone 16'` |
| `settings` | any other games.yml values for this step, e.g. `{difficulty: hard}` |

In a global step, if the server reaches the goal, the biggest contributors get `points` and the
other contributors get `participation-points`. If the goal isn't reached, nobody scores that step.

## Commands

| Command | Permission |
|---|---|
| `/chatgames event list` | `chatgames.event` |
| `/chatgames event top <event>`: who has scored the most in it over time | `chatgames.event` |
| `/chatgames event start <event>` | `chatgames.event.manage` |
| `/chatgames event stop` | `chatgames.event.manage` |

Events can also be started from the admin menu, and posted to [Discord](Discord).
