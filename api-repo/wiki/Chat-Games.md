# Chat Games

A chat game asks something in chat, and the first players to type the right answer win. You write
one thing: a **generator** that makes a `Question` (what to show and which answers win). ChatGames
does the rest: the announcement, the timer, hints, the countdown, checking answers, winners,
points, rewards, stats, leaderboards, challenges and streaks.

```java
ChatGamesAPI.get().registerGame(this, ChatGame.builder("capitals")
        .displayName("Capitals")
        .generator(context -> Question.builder("Paris")
                .display("France")
                .build())
        .build());
```

## How a round runs

1. The rotation (or `/chatgames capitals`, an event, a preset...) picks your game.
2. ChatGames calls your **generator** with a `QuestionContext`. You return a `Question`.
3. `ChatGamesStartEvent` fires. If no plugin cancels it, the start message from messages.yml is
   announced, with `%selection%` replaced by the question's `display` text.
4. Players type in chat. A message wins if it matches **one of the question's answers**:
   - capitalisation is ignored, unless the owner sets `case-sensitive: true` (players who are right
     except for capitalisation are then told so);
   - spaces are ignored, so `ender pearl` matches `enderpearl`;
   - one full stop at the end is allowed;
   - accents are **not** ignored: add `Brasília` *and* `Brasilia` if both should win.
5. The first `winners` players (games.yml, 1 by default) win, in order. Each gets points, the
   rewards from rewards.yml and a `ChatGamesWinEvent`.
6. When the places are taken or the time runs out, the win or expired message is shown and
   `ChatGamesEndEvent` fires.

Your game can also be played as a **global game** (the whole server answering a stream of
questions together), if the owner adds it to global_games.yml. Your generator is then called once
per question, so make sure it gives different questions.

## ChatGame.builder(id)

| Method | Default | What it does |
|---|---|---|
| `displayName(String)` | the id in title case (`boss_quiz` → `Boss Quiz`) | The name in menus, messages (`%game%`) and logs. |
| `icon(String material)` | `paper` | The menu icon. Any material name from 1.8 to 26.x; modern and legacy names both work. |
| `setting(String path, Object value)` | | A default for the game's games.yml section. Use it to change a standard setting (`timeToGuess_seconds`, `winners`, `points`, `hints.max`...) or to add **your own** settings, which your generator reads back (see [Your own settings](#your-own-settings)). |
| `messages(start, win, expired)` | standard lines | The default messages.yml lines, as lists. Pass `null` for any list to keep the standard one. |
| `rewards(String... lines)` | a "You won" message | The default reward lines for first place, e.g. `"[consolecmd] give %player% diamond 1"`. See [Reward Actions](Reward-Actions) for the line format. |
| `generator(QuestionGenerator)` | **required** | Makes each round's question. |
| `build()` | | Throws `IllegalStateException` if the id is malformed or there is no generator. |

All of these are **defaults**: they are written into the owner's files the first time your game is
registered. After that, the files win, so the owner can change the time, the points, the messages
and the rewards without touching your code. Changing a default in your code later does not change
servers that already have the section. See [What Server Owners Configure](What-Server-Owners-Configure).

## Question.builder(answer)

| Method | What it does |
|---|---|
| `builder(String answer)` | The main answer. It's the one shown as `%correct_answer%` when the round ends. |
| `answer(String)` | Another accepted answer (a synonym, another spelling, a plural). Blank and repeated answers are ignored. |
| `answers(Iterable<String>)` | Several more answers at once. |
| `display(String)` | The text for `%selection%`: the question, the scrambled word, the puzzle. Colour codes (`&a`) work. |
| `placeholder(String key, Object value)` | An extra placeholder for this round's messages, e.g. `placeholder("%category%", "Fruit")`. Use it to show more than one piece of information. |
| `hints(List<String>)` | Your own hints, shown one by one while the round runs. At most 3 are used. |
| `revealHints()` | Let ChatGames make the hints by revealing the main answer letter by letter. |
| `component(placeholder, text, hoverText, clickCommand)` | Text that can be hovered and/or clicked. It replaces `placeholder` in the start message. See [Hover and click](#hover-and-click). |
| `build()` | Throws `IllegalStateException` if there is no answer. |

Hints only show if the owner has `hints.enable: true` for your game (the default for new games),
and only as many as `hints.max` (1 by default, up to 3). With neither `hints(...)` nor
`revealHints()`, your game simply has no hints.

## QuestionContext

The generator gets a `QuestionContext`:

| Method | Returns |
|---|---|
| `getGame()` | Your game's `GameType`. |
| `getSettings()` | Your game's section of games.yml, including your own settings. If a game event changes a value for this round, the change is already applied. |
| `getMessages()` | Your game's section of messages.yml, for extra texts you let the owner translate. |
| `getWords()` | All of words.yml (the word lists the built-in games use). |
| `getArgs()` | The words after `/chatgames <id>` when an admin starts the game by command; empty otherwise. |
| `getRandom()` | The random source to use. |
| `randomWord(List<String>)` | A random word from the list that isn't in words.yml's blacklist. Throws `IllegalStateException` when none is left. |

### Rules for generators

- It runs on the main thread (the global region on Folia). **Be quick and never block**: no
  database queries, web requests or file reads. Load your data in `onEnable` and keep it in memory.
- Throw `IllegalStateException` with a clear message if you can't make a question (an empty word
  list, a missing setting). The game then doesn't start, and the message is logged for the owner.
- Make plain answers. Players type them, so avoid colour codes and symbols they can't type.

## Recipes

### A fixed list of questions

```java
private static final String[][] CAPITALS = {
        {"France", "Paris"},
        {"Japan", "Tokyo"},
        {"Brazil", "Brasília", "Brasilia"},   // extra entries are more accepted answers
};

ChatGame capitals = ChatGame.builder("capitals")
        .displayName("Capitals")
        .icon("map")
        .messages(
                Arrays.asList("", "<center>&6&lCAPITALS", "<center>&fWhat is the capital of &e%selection%&f?", ""),
                Arrays.asList("<center>&a%winner_1% &fknew it: &e%correct_answer% &7(%time_1%s)"),
                Arrays.asList("<center>&fNobody knew. It was &e%correct_answer%&f."))
        .generator(context -> {
            String[] pick = CAPITALS[context.getRandom().nextInt(CAPITALS.length)];
            Question.Builder question = Question.builder(pick[1]).display(pick[0]).revealHints();
            for (int i = 2; i < pick.length; i++) question.answer(pick[i]);
            return question.build();
        })
        .build();
```

### Generated questions

Nothing has to be stored: the generator can invent every round.

```java
ChatGame times = ChatGame.builder("times_tables")
        .setting("timeToGuess_seconds", 15)
        .generator(context -> {
            int a = 2 + context.getRandom().nextInt(11);
            int b = 2 + context.getRandom().nextInt(11);
            return Question.builder(String.valueOf(a * b))
                    .display(a + " × " + b)
                    .build();
        })
        .build();
```

### Your own settings

Anything you pass to `setting(...)` is written to games.yml once, and your generator reads the
owner's value back. This is how you make your game configurable without your own config file.

```java
ChatGame times = ChatGame.builder("times_tables")
        .setting("max-factor", 12)                              // written to games.yml
        .setting("words", Arrays.asList("apple", "banana"))     // lists work too
        .generator(context -> {
            int max = Math.max(2, context.getSettings().getInt("max-factor", 12));
            int a = 2 + context.getRandom().nextInt(max - 1);
            int b = 2 + context.getRandom().nextInt(max - 1);
            return Question.builder(String.valueOf(a * b)).display(a + " × " + b).build();
        })
        .build();
```

Always give `getInt`, `getString`... a fallback, because the owner may delete the key.

### Word lists and more placeholders

```java
ChatGame category = ChatGame.builder("category_quiz")
        .setting("fruit", Arrays.asList("apple", "banana", "cherry"))
        .setting("animals", Arrays.asList("horse", "rabbit", "parrot"))
        .messages(Arrays.asList("<center>&fName the &e%category% &fthat starts with &e%selection%&f!"), null, null)
        .generator(context -> {
            String category = context.getRandom().nextBoolean() ? "fruit" : "animals";
            String word = context.randomWord(context.getSettings().getStringList(category));
            return Question.builder(word)
                    .display(word.substring(0, 2).toUpperCase() + "...")
                    .placeholder("%category%", category)
                    .revealHints()
                    .build();
        })
        .build();
```

`randomWord` skips words in words.yml's blacklist, so owners can ban a word for every game in one place.

### Your own hints

```java
.generator(context -> Question.builder("Notch")
        .display("Who created Minecraft?")
        .hints(Arrays.asList("His real name is Markus", "He sold Mojang in 2014", "N____"))
        .build())
```

### Hover and click

`component(placeholder, text, hoverText, clickCommand)` puts interactive text where `placeholder`
appears in the **start message**, so your start message must contain that placeholder.

**Hover to see the question** (answer by typing):

```java
ChatGame hidden = ChatGame.builder("hover_word")
        .messages(Arrays.asList("<center>&fHover over %word% &fand type what you see!"), null, null)
        .generator(context -> {
            String word = context.randomWord(Arrays.asList("diamond", "emerald", "redstone"));
            return Question.builder(word)
                    .component("%word%", "&b&n[hover me]", "&e" + word, null)   // null: not clickable
                    .build();
        })
        .build();
```

**Click first to win.** Use `"/chatgames " + answer` as the click command, exactly like the built-in
Clickable game. The click then counts as that answer, and typing the answer in chat doesn't count.
Make the answer a random key so nobody can type or guess it.

```java
ChatGame button = ChatGame.builder("quick_click")
        .messages(Arrays.asList("<center>&fFirst to click %button% &fwins!"), null, null)
        .generator(context -> {
            String key = "qc" + Long.toString(context.getRandom().nextLong() & Long.MAX_VALUE, 36);
            return Question.builder(key)
                    .component("%button%", "&a&l[CLICK]", "&eClick me!", "/chatgames " + key)
                    .build();
        })
        .build();
```

A click command that is anything else (`/warp shop`) simply runs that command, and doesn't answer.

### Letting admins choose the question

`getArgs()` holds what an admin typed after `/chatgames <id>`:

```java
.generator(context -> {
    String[] args = context.getArgs();
    if (args.length >= 2) {
        // /chatgames capitals Italy Rome
        return Question.builder(args[1]).display(args[0]).build();
    }
    return randomCapital(context);
})
```

### Questions from your own data

Load and cache your data in `onEnable` (or asynchronously, then hand it over). The generator only
reads from memory:

```java
private volatile List<String[]> questions = Collections.emptyList();

@Override
public void onEnable() {
    getServer().getScheduler().runTaskAsynchronously(this, () -> questions = loadQuestionsFromDatabase());
    ChatGamesAPI.get().registerGame(this, ChatGame.builder("server_quiz")
            .generator(context -> {
                List<String[]> list = questions;
                if (list.isEmpty()) throw new IllegalStateException("server_quiz: questions aren't loaded yet");
                String[] pick = list.get(context.getRandom().nextInt(list.size()));
                return Question.builder(pick[1]).display(pick[0]).build();
            })
            .build());
}
```

(On Folia, use `getServer().getAsyncScheduler()` instead of `getScheduler()`.)

## Message placeholders

In the start, win and expired messages:

| Placeholder | Value |
|---|---|
| `%selection%` | The question's `display` text. |
| `%correct_answer%` | The main answer (win and expired messages). |
| `%winner_1%`, `%winner_2%`... | The winners by place. |
| `%winner_displayname_1%`... | The winners' display names (nicknames). |
| `%time_1%`, `%time_2%`... | Their times in seconds. |
| `%points_1%`, `%points_2%`... | The winners' total points after the round. |
| `%hint%` | In the hint message only: the hint being shown. |
| `%timeToGuess%` | The round's time limit in seconds. |
| `%game%` | Your game's display name. |
| `%prefix%` | ChatGames' prefix from messages.yml. |
| your own | Anything you added with `Question.placeholder(...)`. |

`<center>` at the start of a line centres it, `&` colour codes work, and PlaceholderAPI
placeholders are filled in for each player who reads the message.

## Removing a game

`ChatGamesAPI.get().unregister(gameType)` removes a game you registered (it stops it first if it is
running). You rarely need it: games are removed when your plugin disables.
