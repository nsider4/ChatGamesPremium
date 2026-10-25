package org.nsider.chatgames.globalgames;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.games.Game;
import org.nsider.chatgames.games.StartOptions;
import org.nsider.chatgames.games.manager.GameConfigManager;
import org.nsider.chatgames.games.manager.GameSoundManager;
import org.nsider.chatgames.games.round.AnswerChecker;
import org.nsider.chatgames.games.round.GameSettings;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.api.PlayerResult;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.chatgames.utils.messages.Msg;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

/**
 * A chat game played globally: a stream of questions. Each player can answer each question
 * once; it moves on after {@code answers-per-question} correct answers, once everyone who
 * can play has answered, or after {@code question-seconds}, so slower players still get to
 * contribute and a small server isn't left waiting out every question. Every correct
 * answer is +1 for the server. Answers are always hidden, so they can't be copied.
 *
 * <p>Each question is a {@link Game} whose winners are its correct answers, which keeps
 * the one-answer-each and the limit atomic while answers arrive on several threads.</p>
 */
final class QuestionStream extends GlobalSession {

    /** Ticks between a question filling up and the next one. */
    private static final long NEXT_QUESTION_TICKS = 30L;
    /** Tries to generate a question before the game gives up. */
    private static final int GENERATE_TRIES = 3;

    private final ConfigurationSection section;
    private final GameSettings settings;
    private final String[] args;
    @Nullable private volatile Game question;
    @Nullable private Round first;
    private int number;

    private QuestionStream(ChatGames plugin, GlobalGameManager manager, GlobalGameConfig.Entry entry,
                           ConfigurationSection section, Round first, String[] args) {
        super(plugin, manager, entry, entry.getGoal());
        this.section = section;
        this.settings = new GameSettings(entry.getType(), section);
        this.first = first;
        this.args = args;
    }

    /**
     * @return the stream, or null if its first question couldn't be generated (the console
     *         says why)
     */
    @Nullable
    static QuestionStream create(@Nonnull ChatGames plugin, @Nonnull GlobalGameManager manager,
                                 @Nonnull GlobalGameConfig.Entry entry, @Nonnull StartOptions options, @Nonnull String[] args) {
        GameType type = entry.getType();
        // The game's own settings (and an event's overrides); the global ones decide each question's limits
        ConfigurationSection section = options.apply(GameConfigManager.section(ConfigManager.getGames(), type));
        section.set(StartOptions.GAME_TIME, entry.getQuestionSeconds());
        section.set("winners", entry.getAnswersPerQuestion());
        Round first = plugin.getGameBuilder().generate(type, section, args);
        return first == null ? null : new QuestionStream(plugin, manager, entry, section, first, args);
    }

    @Nonnull
    @Override
    Msg startMessage() {
        return fill(Msg.of(GlobalGameManager.messageList("start")))
                .with("%answers%", entry.getAnswersPerQuestion())
                .with("%question_time%", entry.getQuestionSeconds());
    }

    @Override
    void begin() {
        nextQuestion();
    }

    /** Main/global thread. */
    private void nextQuestion() {
        if (progress.isOver()) return;
        Round round = first;
        first = null;
        for (int i = 0; round == null && i < GENERATE_TRIES; i++) {
            // Values given to the start are for the first question only, or they would all be the same
            round = plugin.getGameBuilder().generate(getType(), section, new String[0]);
        }
        if (round == null) {
            plugin.getLogger().warning("The global " + getType().getConfigKey() + " game stopped: no new question could be generated.");
            manager.fail(this);
            return;
        }
        Game game = new Game(round, settings);
        question = game;
        number++;
        Msg header = fill(Msg.of(GlobalGameManager.messageList("question"))).with("%question%", number);
        plugin.getGameManager().getAnnouncer().announceQuestion(round, settings, header);
        schedule(plugin.getScheduler().globalLater(() -> timeUp(game), entry.getQuestionSeconds() * 20L));
    }

    /** Nobody filled the question in time: show its answer and move on. */
    private void timeUp(Game game) {
        if (question != game || !game.finish()) return;
        Msg over = fill(Msg.of(GlobalGameManager.message("question_over"))).with("%answer%", game.getAnswer());
        for (Player player : manager.players()) over.send(player);
        nextQuestion();
    }

    private void filled(Game game) {
        if (question != game || !game.finish()) return;
        schedule(plugin.getScheduler().globalLater(this::nextQuestion, NEXT_QUESTION_TICKS));
    }

    @Override
    boolean onChat(@Nonnull Player player, @Nonnull String message) {
        Game game = question;
        if (game == null || game.getRound() == null) return false;
        // A clickable key is only reachable by clicking, never by typing
        if (game.getRound().isClickAnswer(game.getRound().getMainAnswer())) return false;
        AnswerChecker.Result result = AnswerChecker.check(game.getRound(), message, settings.isCaseSensitive());
        if (game.hasResult(player.getUniqueId()) || game.isFinished()) {
            // Saying a correct answer again would hand it to everyone still answering
            return result == AnswerChecker.Result.CORRECT;
        }
        if (result == AnswerChecker.Result.WRONG_CASE) {
            MessageUtils.sendMessage(player, ConfigManager.getMessages().get(MessageKey.WRONG_CASE));
            GameSoundManager.playGameSound(player, "wrong-case");
            return false;
        }
        if (result != AnswerChecker.Result.CORRECT) return false;
        answered(player, game);
        return true;
    }

    @Override
    boolean onClick(@Nonnull Player player, @Nonnull String key) {
        Game game = question;
        Round round = game == null ? null : game.getRound();
        if (round == null || !round.isClickAnswer(key)) return false;
        if (!game.hasResult(player.getUniqueId()) && !game.isFinished()) answered(player, game);
        return true;
    }

    private void answered(Player player, Game game) {
        long millis = game.getElapsedMillis();
        if (millis < settings.getMinAnswerMillis()) {
            // Faster than a person can read and type: most likely a macro or chat bot
            plugin.getLogger().warning(player.getName() + " answered global " + getType().getConfigKey() + " in " + millis
                    + " ms (minimum " + settings.getMinAnswerMillis() + " ms); the answer was ignored.");
            return;
        }
        PlayerResult placed = game.claimPlace(player.getUniqueId());
        if (placed == null) return; // the question filled up a moment earlier
        CoopProgress.Step step = manager.contribute(this, player, 1);
        if (step != null && !step.isCompleted()) {
            fill(Msg.of(GlobalGameManager.message("answered"))).with("%contribution%", step.getContribution())
                    .with("%progress%", step.getAfter()).send(player);
            GameSoundManager.playGameSound(player, "win");
        }
        plugin.getScheduler().global(() -> {
            if (game.isFull() || everyoneAnswered(game)) filled(game);
        });
    }

    /** Main/global thread. */
    private boolean everyoneAnswered(Game game) {
        for (Player player : manager.players()) {
            if (!game.hasResult(player.getUniqueId())) return false;
        }
        return true;
    }

    @Override
    void end() {
        Game game = question;
        if (game != null) game.finish();
        question = null;
    }

    @Nonnull
    @Override
    public String getGoalText() {
        return progress.getGoal() + " " + GlobalGameManager.word("answers");
    }

    /** @return the question being answered, or null between questions */
    @Nullable
    Game getQuestion() {
        return question;
    }
}
