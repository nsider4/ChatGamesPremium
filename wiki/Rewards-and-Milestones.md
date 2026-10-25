# Rewards and Milestones

## Reward lines

Every reward in ChatGames (games, races, global games, events, challenges, streaks) is a list
of lines like these:

| Action | What it does |
|---|---|
| `[consolecmd] <command>` | The console runs the command |
| `[playercmd] <command>` | The winner runs the command |
| `[player_as_op] <command>` | The winner runs the command as op (op is removed straight after) |
| `[playermsg] <message>` | Sends the winner a message |
| `[broadcast] <message>` | Sends a message to everyone in enabled worlds |

`%player%` is the winner's name. Other plugins can add their own actions through the API.

**Chance:** start a line with `<chance>%~ ` to run it only sometimes:

```yaml
- '10.0%~ [consolecmd] give %player% diamond_block 3'
- '30.0%~ [consolecmd] give %player% gold_block 3'
- '[consolecmd] give %player% iron_block 5'
```

Lines without a chance always run. Of the lines with one, at most one is drawn: here a 10%
chance of diamond blocks, 30% of gold blocks, and 60% of neither. If the chances add up to more
than 100, they're scaled to fit.

**Permission:** end a line with `has:<permission>` to give it only to players who have it:

```yaml
- '[consolecmd] give %player% diamond 5 has:rank.vip'
```

**Items:** a `[consolecmd] give <player> <item> [amount]` line gives the item directly, with any
item name from 1.8 to 26.x. Whatever doesn't fit in the inventory drops at the player's feet.

## Chat game rewards (rewards.yml)

```yaml
unscramble:
  rewards:
    1:                        # first place
      reward_1:
        minimum-players: 2    # only picked with 2+ players online
        chance: 50            # how likely this reward is picked
        data:
          - '[consolecmd] give %player% iron_block 5'
      reward_2:
        data:
          - '[consolecmd] give %player% iron_block 2'
    2:                        # second place (if winners is 2 or more)
      reward_1:
        data:
          - '[consolecmd] give %player% iron_block 1'
```

For each place:
1. Rewards whose `minimum-players` isn't met are skipped.
2. Every reward without a `chance` is given. Of those with one, a single reward is drawn
   (chances are percentages; below 100 in total, sometimes none is).
3. In each reward given, the `data` lines run as described above.

## Race rewards

Races name their rewards in races.yml (`rewards: [reward_2]`); the names are defined once in
`rewards.yml`:

```yaml
races:
  rewards:
    reward_1:
      chance: 50
      data:
        - '60.0%~ [consolecmd] give %player% diamond_block 5'
    reward_2:
      data:
        - '[consolecmd] give %player% iron_block 5'
        - '[playermsg] &aYou won!'
```

## Points

Each win gives the game's `points` (games.yml or races.yml). Points make up `/chatgames top`, and
admins change them with `+points`, `-points` and `setpoints`.

## Milestones

`milestones.yml` runs console commands when a player's points go from below a total to that
total or more:

```yaml
milestones:
  '100':
    - "say %player% reached 100 points!"
    - "give %player% diamond 1"
  '500':
    - "give %player% emerald 5"
```

Points given to an offline player by command still pay their milestones: they run when that
player next joins.
