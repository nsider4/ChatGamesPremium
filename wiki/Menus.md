# Menus

ChatGames has inventory menus for players and admins, all defined in `menus.yml`. Change what
any button does, move or restyle it, add buttons, or add whole menus of your own.

| Menu | Open with | Shows |
|---|---|---|
| `main` | `/chatgames menu` (alias `gui`) | your profile, challenges, streaks, leaderboards, your game and sound toggles, and the admin menu for admins |
| `profile` | `/chatgames profile [player]` | points, wins, best times, streaks; works for offline players |
| `challenges` | `/chatgames menu challenges` | today's and this week's challenges, with a claim button |
| `streaks` | `/chatgames menu streaks` | the three streaks and their reward ladders |
| `leaderboards` | `/chatgames menu leaderboards` | points, fastest per game, event leaderboards |
| `admin` | `/chatgames admin` | start/stop games and globals, events, presets, rotation, logs |
| `debug` | `/chatgames debug` | private test rounds of any chat game |

`/chatgames menu <id>` opens any menu by its id, including your own.

## Items

```yaml
menus:
  main:
    title: '&8Chat Games'
    rows: 3
    items:
      profile:
        slot: 10
        material: player_head
        head: '%player%'
        name: '&e&lYour Profile'
        lore:
          - '&7Points: &e%points%'
          - '&7Wins: &e%wins%'
        actions: ['[open] profile']
```

| Key | Meaning |
|---|---|
| `slot` / `slots` | from 0 (top left) to rows×9−1; `slots: [0, 1, '9-17']` repeats the item |
| `material` | any item name from 1.8 to 26.x (`lime_dye`, `INK_SACK:10`, `351:10`), or `head:<name, texture or URL>` |
| `name`, `lore` | colour codes (`&a`, and `&#RRGGBB` on 1.16+) and PlaceholderAPI placeholders |
| `amount`, `glow`, `enchantments`, `flags` | `glow: true`, `enchantments: ['SHARPNESS:1']`, `flags: [HIDE_ATTRIBUTES]` |
| `head` | a player's head, e.g. `'%player%'` |
| `custom-model-data` / `item-model` / `color` | 1.14+ / 1.21.2+ / leather colour `'#FF0000'` |
| `actions` | what a click does; also `left-actions`, `right-actions`, `shift-left-actions`, `shift-right-actions`, `middle-actions`, `drop-actions` |
| `view-requirements` | the item only shows when every line passes |
| `click-requirements` + `deny-actions` | when a line fails, `deny-actions` run instead |
| `state` + `when-off` | when the state is false, `when-off`'s keys replace the item's own |

Leave an item out to hide it. `filler` fills every empty slot.

## Actions

| Action | What it does |
|---|---|
| `[command] <command>` / `[console] <command>` | you / the console run a command |
| `[message] <text>` / `[broadcast] <text>` | a message to you / to everyone |
| `[sound] <sound> [volume] [pitch]` | plays a sound (any name from 1.8 to 26.x, or a resource-pack sound) |
| `[open] <menu> [values]` | opens another menu |
| `[switch] <menu> [values]` | the same, but `[back]` skips it |
| `[back] [menu]` | the menu before (or the one named, or closes) |
| `[refresh]`, `[close]`, `[next-page]`, `[previous-page]`, `[wait] <ticks>` | |
| `[chatgames:no-permission]` | sends the no-permission message |

Menus with their own actions (claiming a challenge, starting a game) describe them in comments
above the menu in menus.yml.

## Requirements

One per line; every line must pass. Start a line with `!` to reverse it.

```yaml
view-requirements:
  - 'permission chatgames.admin'
  - '%chatgames_points% >= 100'       # ==, !=, >=, <=, >, <
```

A value that is `true`, `on`, `yes` or a number other than 0 also passes.

## Lists and pages

Menus that list things (challenges, games, events, leaderboards) put entries in `list-slots`, in
order, drawn with the menu's `templates`. With more entries than slots, items whose actions are
`[next-page]` / `[previous-page]` turn the pages, and hide when there's no page to turn to.
Every menu has `%player%`, `%page%`, `%pages%` and `%entries%`.

## Your own menus

Add a menu under `menus:`, or put it in its own file `menus/<id>.yml`, then open it with
`/chatgames menu <id>` or an `[open] <id>` action.
