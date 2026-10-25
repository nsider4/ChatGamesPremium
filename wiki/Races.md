# Races

A race asks players to do something in the world first: kill 5 zombies, mine 16 stone. The first
to finish wins. Races are in `races.yml`; turn them all off with `enable-races: false`.

## The races

| Race | Players have to... | Targets are |
|---|---|---|
| `hunt` | kill mobs | mob types (and MythicMobs mobs) |
| `mine` | break blocks | blocks |
| `place` | place blocks | blocks |
| `fish` | catch items by fishing | items |
| `eat` | eat food | food items |
| `craft` | craft items | items |
| `furnace` | take smelted items out of a furnace | items |
| `tame` | tame animals (wolves, cats, horses, parrots...) | tameable mobs |
| `shear` | shear sheep or mooshrooms | shearable mobs |

## Setting up a race

```yaml
hunt:
  enable: true
  timeToComplete_seconds: 180
  points: 1              # points the winner gets
  weight: 1              # how often the rotation picks it (0 = never)
  titleMessage: '&bHUNT'
  subtitleMessage: '&aKill %amount% %value%!'
  count-spawner-mobs: true   # do mobs from spawners and spawn eggs count?
  count-mythic-mobs: true    # does a MythicMobs zombie count as a ZOMBIE?
  data:
    ZOMBIE:
      3:                 # an amount: kill 3 zombies
        rewards:
          - reward_2
      5:
        rewards:
          - reward_1
```

Each round picks one target from `data` and one of its amounts. The winner gets the rewards
listed for exactly that target and amount. The reward names (`reward_1`...) are defined in
`rewards.yml` under `races.rewards`; see [Rewards](Rewards-and-Milestones).

- **Names:** modern or legacy names both work (`MOOSHROOM` or `MUSHROOM_COW`, `COD` or
  `RAW_FISH`). Names your server version doesn't have (a 1.21 block on 1.8) are skipped, with a
  console note.
- **MythicMobs:** in a Hunt race, `mythic:<MobType>` targets one of its mobs.
- **Creative mode:** `creative-enabled: false` stops players in creative mode from taking part.
- Players in disabled worlds and players who turned games off don't take part.

## Starting a race by command

`/chatgames <race> <target> [amount]`, e.g. `/chatgames mine stone 16`. Without an amount, one
of the amounts listed for that target is used. Any valid target works, even one not listed; it
just has no rewards (points are still given). Tab completion lists the targets.

## Global races

Every race can also be played as a [global game](Global-Games): the whole server mines 500 stone
together, and everyone who helped is rewarded.
