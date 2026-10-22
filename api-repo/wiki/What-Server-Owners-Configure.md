# What Server Owners Configure

Your defaults are only a starting point. The first time your game or race is registered on a server,
ChatGames writes its sections into the owner's files, with a comment saying which plugin added them.
From then on, **the files win**: owners tune your content like the built-in games, and you don't
have to build any config of your own.

This page shows what ends up where, so you can describe it on your plugin page.

## Chat games

**games.yml** (from your `setting(...)` calls over these standard values):

```yaml
#Capitals, added by MyPlugin
capitals:
  enable: true              # in the automatic rotation
  winners: 1                # how many players can win each round
  weight: 1                 # how often the rotation picks it (0 = never)
  points: 1                 # points per win
  hints:
    enable: true
    after-percent: 50       # first hint after this much of the time
    max: 1                  # up to 3
  countdown:
    enable: true            # time left on the action bar
    last-seconds: 10
  case-sensitive: false
  timeToGuess_seconds: 20
  titleMessage: '&b%selection%'   # empty: no title
  subtitleMessage: '&aCapitals'
  # ...and your own settings
```

Owners can also add `min-answer-ms` (answers faster than this are ignored, 400 by default).

**messages.yml**: `gameStartAnnouncement`, `correct_message` and `time_expired_message` (from
`messages(...)`, or the standard lines). Owners may add `hint_message` and `countdown_actionbar` to
your section to override the shared ones.

**rewards.yml**: the first-place rewards (from `rewards(...)`):

```yaml
capitals:
  rewards:
    1:                      # place; add 2:, 3:... for more winners
      reward_1:
        data:
          - '[playermsg] &aYou won a round of Capitals!'
```

## Races

**races.yml**: `enable`, `weight`, `points`, `timeToComplete_seconds`, `titleMessage`,
`subtitleMessage`, your settings, and `data` with every target and amount (see [Races](Races#rewards-for-races)).
Races also follow the global `enable-races` and `creative-enabled` switches at the top of races.yml.

**messages.yml**: `gameStartAnnouncement`, `correct_message` and `time_expired_message`.

## Everywhere else, by id

Owners can use your game's id anywhere a game is named, the same way as a built-in game:

| File / feature | Example |
|---|---|
| a preset (presets.yml) | `games: [unscramble, capitals]` |
| a game event step (events.yml) | `- game: capitals` |
| a global game (global_games.yml, under `games:`) | `capitals: { enabled: true, goal: 10 }` |
| a challenge (challenges.yml) | `type: WIN_GAME` with `game: capitals` |
| the command and its permission | `/chatgames capitals`, `chatgames.start.capitals` |
| PlaceholderAPI | `%chatgames_wins_capitals%` |

These aren't written for you: owners add them when they want them. It helps to put ready-to-paste
snippets on your plugin page.

Your game also appears in the menus (with your `displayName` and `icon`), `/chatgames top`, tab
completion and the logs, with no configuration.

## Resetting to your defaults

Your defaults are written only when the section is missing. To get them back (say, after you
improved them in an update), an owner deletes your section from the file and restarts the server.

## Things to tell owners

- Which ids your plugin registers, and what each one is.
- Your own settings in games.yml or races.yml, and what they do.
- Your [reward actions](Reward-Actions) and their argument format.
- Your [challenge types](Challenge-Types) with a challenges.yml example.
