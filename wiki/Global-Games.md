# Global Games

In a global game the whole server plays as **one team**. Every correct answer (or block mined,
mob killed, number counted) adds to one shared score shown on a progress bar. Nobody has to be
the fastest: if the server reaches the goal in time, **everyone who contributed is rewarded**.
If time runs out first, the game fails.

Everything is in `global_games.yml`.

## Kinds

- **Chat games** run as a stream of questions. Each player can answer each question once, and
  the next question comes after `answers-per-question` correct answers or `question-seconds`.
  Answers are always hidden, so they can't be copied. Memorize and Guess the Number can't be
  played globally.
- **Races** add everyone's progress together, e.g. 500 stone mined by the whole server.
- **Count** (global only): count together in chat, one number per message, in order. A mistake
  starts the count over (`on-mistake: reset`, up to `max-mistakes`) or ends the game
  (`on-mistake: fail`). Modes such as `up`, `down`, `evens` and `fives` set the start, target and
  step.

## When they start

```yaml
triggers:
  chance-after-game: 10      # % chance one starts after a regular game or race ends
  delay-seconds: 5           # wait after that game
  interval-minutes: 0        # also start one every N minutes (0 = off)
  minimum-players: 2         # players needed for one to start by itself
```

Or start one yourself: `/chatgames global <game> [values]`, e.g. `/chatgames global mine stone 500`
or `/chatgames global count evens`. A [game event](Game-Events) step can also be global.

## The goal

`defaults` applies to every game; each game under `games:` can change any of it:

| Setting | Meaning |
|---|---|
| `goal` | correct answers (or items/mobs) the server needs |
| `duration-seconds` | how long the server has |
| `min-contribution` | the least a player must contribute to be rewarded |
| `points` | points every rewarded player gets |
| `points-per-contribution` | extra points per contribution |
| `answers-per-question` | chat games: how many players can answer one question |
| `question-seconds` | chat games: the longest a question stays up |

```yaml
games:
  math:
    enabled: true
    goal: 15
    duration-seconds: 150
  mine:
    enabled: true
    goal: 200
    duration-seconds: 240
```

## Progress bar

A boss bar on 1.9+, the action bar on 1.8. Set its `color` and `style` under `progress-bar`.
`announce-at: [25, 50, 75]` also announces those percentages in chat.

## Rewards

```yaml
rewards:
  completed:          # everyone who contributed enough, when the goal is reached
    reward_1:
      data:
        - '[consolecmd] give %player% iron_ingot 3'
  failed: {}          # the same players when time runs out (empty = nothing)
  top-contributors:   # extra for the biggest contributors, by place
    1:
      reward_1:
        data:
          - '[consolecmd] give %player% diamond 1'
```

These use the [rewards.yml format](Rewards-and-Milestones); `%contribution%` is what the player
put in. A game's own `rewards:` section replaces these for that game.

Every message players see (start, each question, milestones, completed, failed) is under
`messages:` in the same file.
