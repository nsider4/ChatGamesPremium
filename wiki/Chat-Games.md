# Chat Games

A chat game asks something in chat; the first player(s) to type the right answer win. The
winning message is hidden from chat (`cancel-message-when-winning`), so nobody can copy it.

## The games

| Game | Players have to... | Its own settings (games.yml) |
|---|---|---|
| `unscramble` | unscramble a word | |
| `unreverse` | write a backwards word the right way round | |
| `reaction` | type the word shown, fastest | |
| `random` | type a random string | length range, character type (letters, numbers, symbols, hex, custom) |
| `fillout` | fill in the hidden letters of a word | percent of letters hidden |
| `hoverable` | hover over the text and type the word it shows | |
| `clickable` | click the text first | |
| `math` | solve a sum, or find the missing number (`12 + ? x 3 = 33`) | difficulty 1-5, operators, how many, modes |
| `variable` | work out a symbol's value from two equations | symbols and their values, result range |
| `sequence` | type the next number (`3, 6, 12, 24, ?`) | difficulty 1-3, numbers shown |
| `memorize` | remember a list shown for a few seconds, then type it | list length, pools, order matters, memorise time |
| `guess_the_number` | guess a number in a range | lower and upper limit |
| `trivia` | answer a question | |
| `word_association` | find the link between three clues | difficulty: easy, medium, hard or mixed |
| `count` | count together, one number per message | global only, see [Global Games](Global-Games) |

The words, questions and lists are in `words.yml`: add your own under each game. Trivia
questions can have several accepted answers. `words.yml` also has a blacklist of words no game
will ever pick.

## Settings every game has

Each game has a section in `games.yml`. A missing setting uses the default.

```yaml
unscramble:
  enable: true
  weight: 1                 # how likely the rotation picks it (0 = never)
  winners: 1                # how many players can win each round
  points: 1                 # points per win
  case-sensitive: true      # right but wrong capitals -> 'wrong_case' message
  timeToGuess_seconds: 20
  min-answer-ms: 400        # faster correct answers are ignored and logged (0 = off)
  hints:
    enable: true
    after-percent: 50       # first hint once half the time has passed
    max: 1                  # up to 3
  countdown:
    enable: true            # time left on the action bar
    last-seconds: 10        # 0 = the whole round
  titleMessage: '&b%selection%'
  subtitleMessage: '&aUnscramble'   # leave both empty for no title
```

- **Several winners:** with `winners: 3`, the first three correct players win, in order. Give
  each place its own rewards in rewards.yml (`1:`, `2:`, `3:`).
- **Anti-macro:** `min-answer-ms` catches chat bots. A person can't read and type an answer in
  under 400 ms; those answers are ignored and the console names the player.
- **Hints** use `hint_message` in messages.yml. Most games reveal the answer letter by letter;
  Word Association reveals more clues.

## Messages

Each game's start, win and time-up messages are in its section of `messages.yml`. Some
placeholders:

| Placeholder | Value |
|---|---|
| `%selection%` | the question: the scrambled word, the sum, the trivia question |
| `%timeToGuess%` | seconds to answer |
| `%winner_<n>%`, `%time_<n>%`, `%points_<n>%` | the n-th winner, their time and points |
| `%display_name%` | the winner's display name, instead of `%player%` |
| `%prefix%` | config.yml's prefix |

Put `<center>` at the start of a line in a list message to centre it.

## Starting a game with your own values

`/chatgames <game> [values]` starts a game now. The values are optional:

| Game | Values | Example |
|---|---|---|
| unscramble, unreverse, reaction, fillout, hoverable | `<word>` | `/chatgames unscramble Diamond` |
| math | `<expression>` | `/chatgames math 12 + 7 x 3` |
| guess_the_number | `<number> <spread>` | `/chatgames guess_the_number 42 50` |
| random, memorize | `<length>` | `/chatgames memorize 5` |
| trivia | `<question id>` from words.yml | `/chatgames trivia 3` |
| word_association | `<association id>` from words.yml | `/chatgames word_association creeper` |

Only one game (or race) runs at a time; a command while one runs says so.

## Testing a game privately

`/chatgames debug` opens a menu where you can play any chat game only for yourself: nobody else
sees it and no rewards or points are given. It tells you the answer, how long the round took to
make, and whether what you type would win (or would get the wrong-capitals message). Go straight
to one game with `/chatgames debug <game> [values]`. Races are played in the world, so test them
with `/chatgames <race>` instead.

## Sounds

`GameSounds` in config.yml sets the sounds for a game starting, a win, time running out and a
wrong-case answer. Any sound name from 1.8 to 26.x works, and so do resource-pack sounds
(`myserver:custom.ding`). Leave one empty to turn it off. Players can mute them with
`/chatgames togglesound`.
