# Rotation and Presets

The rotation starts a game by itself every so often. Games can also be started by command, by a
[game event](Game-Events) or as a [global game](Global-Games).

## How often

In `config.yml`:

```yaml
# A game every 25 minutes. -1 turns the rotation off completely (commands still work).
time_minutes: 25

# Or a random delay, rolled again before every game:
randomDelay:
  enable: true
  timeUnit: 'minutes'     # or 'seconds'
  minimum: 5
  limit: 25
```

With `randomDelay.enable: true`, `time_minutes` is ignored (unless it's -1). With
`timeUnit: seconds`, keep `minimum` above the games' answer times.

## Who has to be online

```yaml
min_players_online: 1
count-all-players: false
```

The rotation only starts a game when at least `min_players_online` players can play. Vanished
players never count. With `count-all-players: false`, only players with `chatgames.play` count;
with `true`, everyone else does.

When the time comes and a game is still running (one started by command, say) or an event is
on, that turn is skipped.

## Which game

Every game and race with `enable: true` takes part. The rotation draws one at random, weighted
by each game's `weight` (a game with `weight: 3` comes up three times as often as one with
`weight: 1`; `0` never). The same game never runs twice in a row, unless it's the only one.

`/chatgames toggleAll` turns the rotation off and on without changing any file.
`/chatgames any` starts, right now, the game the rotation would pick.

## Worlds

```yaml
disabled-worlds:
  - 'example_world'
allow_starting_from_disabled: true
```

Players in a disabled world don't see or play games. `allow_starting_from_disabled` decides
whether an admin standing in one can start a game; `chatgames.start.*` always can.

## Presets

A preset is a named set of games. While one is active, the rotation only picks its games and the
`enable` flags in games.yml / races.yml are ignored.

```yaml
# presets.yml
active: none
presets:
  weekend_set:
    games:
      - unscramble
      - unreverse
      - reaction
```

| Command | What it does |
|---|---|
| `/chatgames preset list` | Lists the presets and their games |
| `/chatgames preset load <name>` | Makes a preset active (`none` goes back to the enable flags) |
| `/chatgames preset save <name>` | Saves the games enabled right now as a preset |

A running game finishes normally when you switch. Races in a preset still only run while
`enable-races` is on in races.yml. The plugin rewrites presets.yml when you use these commands.
