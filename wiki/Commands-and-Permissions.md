# Commands and Permissions

The command is `/chatgames` (alias `/chatgame`). `/chatgames help` lists everything the sender
can use. Every command tab-completes.

## Players

| Command | Permission | Default |
|---|---|---|
| `/chatgames menu [menu]` | `chatgames.menu` | everyone |
| `/chatgames profile [player]` | `chatgames.profile` (others: `chatgames.profile.others`) | everyone |
| `/chatgames top` | `chatgames.top` | everyone |
| `/chatgames fastest [game]` | `chatgames.fastesttop` | everyone |
| `/chatgames points [player]` | `chatgames.points` (others: `chatgames.points.others`, op) | everyone |
| `/chatgames challenges` | `chatgames.challenges` | everyone |
| `/chatgames streaks` | `chatgames.streaks` | everyone |
| `/chatgames claim` | `chatgames.claim` | everyone |
| `/chatgames event [list \| top <event>]` | `chatgames.event` | everyone |
| `/chatgames toggle [player]` | `chatgames.toggle` (others: `chatgames.toggle.others`, op) | everyone |
| `/chatgames togglesound [player]` | `chatgames.togglesound` (others: `chatgames.togglesound.others`, op) | everyone |
| `/chatgames help` | `chatgames.help` | everyone |

- `toggle` stops a player from seeing and playing games; `togglesound` mutes the game sounds.
- Playing needs `chatgames.play` only when `use-permission-toplay: true` in config.yml.

## Admins

| Command | What it does | Permission |
|---|---|---|
| `/chatgames <game> [values]` | Starts that game now (see [custom values](Chat-Games#starting-a-game-with-your-own-values)) | `chatgames.start.<game>` or `chatgames.start.*` |
| `/chatgames any` | Starts the game the rotation would pick next | `chatgames.start.*` |
| `/chatgames global <game> [values]` | Starts a [global game](Global-Games) | `chatgames.global` |
| `/chatgames stop` | Ends the running game with no winner | `chatgames.stop` |
| `/chatgames toggleAll` | Turns the automatic rotation on or off | `chatgames.toggleAll` |
| `/chatgames preset <list \| load \| save> [name]` | [Presets](Rotation-and-Presets#presets) | `chatgames.preset` |
| `/chatgames event start <event>` / `event stop` | Starts or stops a [game event](Game-Events) | `chatgames.event.manage` |
| `/chatgames +points <player> <amount>` | Adds points | `chatgames.manage.points` |
| `/chatgames -points <player> <amount>` | Removes points | `chatgames.manage.points` |
| `/chatgames setpoints <player> <amount>` | Sets points | `chatgames.manage.points` |
| `/chatgames remove <player>` | Deletes all of a player's data | `chatgames.remove` |
| `/chatgames challenges reset <player>` | Clears a player's challenge progress | `chatgames.challenges.reset` |
| `/chatgames streak set <player> <challenge \| daily \| global> <days>` | Sets a streak | `chatgames.streaks.set` |
| `/chatgames logs [amount]` | Shows the last game logs | `chatgames.logs` |
| `/chatgames discord test <message \| all>` | Posts a Discord message with sample values | `chatgames.discord` |
| `/chatgames discord leaderboard` | Posts the leaderboard to Discord now | `chatgames.discord` |
| `/chatgames admin` | Opens the admin menu | `chatgames.admin` |
| `/chatgames debug` | Opens the debug menu: plays any game only for you, with no rewards | `chatgames.debug` |
| `/chatgames reload` | Reloads every file | `chatgames.reload` |

All admin permissions default to op. Point changes work on offline players too: milestones they
cross are paid when they next join.

## Permission notes

- `chatgames.start.*` covers every game and race, and also lets the holder start games in a
  disabled world even when `allow_starting_from_disabled: false`.
- `chatgames.admin` only opens the admin menu; each button still checks the permission of the
  command it runs.
- A `has:<permission>` at the end of a reward line gives that line only to players with the
  permission (see [Rewards](Rewards-and-Milestones)).
