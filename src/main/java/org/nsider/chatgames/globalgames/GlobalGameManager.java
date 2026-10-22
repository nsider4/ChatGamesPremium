package org.nsider.chatgames.globalgames;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.api.event.ChatGamesEndEvent;
import org.nsider.chatgames.api.event.ChatGamesGlobalEndEvent;
import org.nsider.chatgames.api.event.ChatGamesStartEvent;
import org.nsider.chatgames.games.StartOptions;
import org.nsider.chatgames.games.manager.GameManager;
import org.nsider.chatgames.games.manager.GameSoundManager;
import org.nsider.chatgames.api.PlayerResult;
import org.nsider.chatgames.managers.TopManager;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.chatgames.progression.Activity;
import org.nsider.chatgames.races.Race;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.chatgames.utils.messages.Msg;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Global games: the whole server plays as one team. Every correct answer (or race unit,
 * or counted number) adds to one shared score; when it reaches the goal in time, everyone
 * who contributed enough is rewarded. If time runs out first, the game fails.
 *
 * <p>This class decides when global games start, runs the one in play
 * ({@link GlobalSession}), shows its progress bar, and pays out at the end. It holds the
 * same game slot as chat games and races ({@link GameManager#tryBegin()}), so nothing
 * else runs meanwhile.</p>
 *
 * <p>Starting, the timer and the end run on the main/global thread; chat and race
 * progress arrive on any thread.</p>
 */
public final class GlobalGameManager {

    /** Ticks between progress bar updates and time checks. */
    private static final long TICK = 10L;

    private final ChatGames plugin;
    private volatile GlobalGameConfig config = GlobalGameConfig.EMPTY;
    @Nullable private ChatGamesScheduler.Task interval;
    @Nullable private volatile GlobalSession current;
    @Nullable private ProgressBar bar;

    public GlobalGameManager(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
        load();
    }

    /** Reads global_games.yml and restarts the interval timer. Main/global thread. */
    public void load() {
        config = GlobalGameConfig.load(ConfigManager.getGlobalGames(),
                problem -> plugin.getLogger().warning("global_games.yml: " + problem));
        ChatGamesScheduler.cancel(interval);
        interval = null;
        int minutes = config.getIntervalMinutes();
        if (minutes > 0) {
            long ticks = minutes * 60L * 20L;
            interval = plugin.getScheduler().globalTimer(this::startRandom, ticks, ticks);
        }
    }

    /** Stops the interval timer and drops a running game without results, for shutdown. */
    public void shutdown() {
        ChatGamesScheduler.cancel(interval);
        interval = null;
        GlobalSession session = current;
        if (session != null && session.getProgress().close(CoopProgress.Outcome.STOPPED)) {
            current = null;
            session.cancelTasks();
            session.end();
            hideBar();
        }
    }

    @Nonnull
    public GlobalGameConfig getConfig() {
        return config;
    }

    /** @return the global game in play, or null */
    @Nullable
    public GlobalSession getCurrent() {
        return current;
    }

    public boolean isRunning() {
        return current != null;
    }

    /**
     * Starts a global game now, with its settings from global_games.yml. Main/global thread.
     *
     * @param args optional values, as for a normal start (Count: a mode, or start target [step])
     * @return false if it isn't enabled in global_games.yml, something is running, or it couldn't start
     */
    public boolean start(@Nonnull GameType type, @Nonnull String... args) {
        GlobalGameConfig.Entry entry = config.get(type);
        return entry != null && plugin.getGameManager().startGame(type, StartOptions.global(entry), args);
    }

    /**
     * Starts a global game with these options; {@link GameManager#startGame} sends every
     * global start here. Main/global thread.
     *
     * @return false if something is running, it couldn't be set up, or a plugin cancelled it
     */
    public boolean begin(@Nonnull GameType type, @Nonnull StartOptions options, @Nonnull String... args) {
        if (!GlobalGameConfig.supports(type)) return false;
        GlobalGameConfig.Entry entry = options.getGlobal() != null ? options.getGlobal() : config.find(type);
        GlobalSession session = create(type, entry, options, args);
        if (session == null) return false;

        GameManager games = plugin.getGameManager();
        if (!games.tryBegin()) return false;
        ChatGamesStartEvent event = new ChatGamesStartEvent(type, "global: " + session.getGoalText());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            games.endGame();
            return false;
        }
        current = session;
        bar = new ProgressBar(config.getBar(), plugin.getLogger());

        Msg start = session.startMessage();
        for (Player player : players()) {
            GameSoundManager.playGameSound(player, "game-start");
            start.send(player);
        }
        session.begin();
        session.schedule(plugin.getScheduler().globalTimer(() -> tick(session), 1L, TICK));
        return true;
    }

    @Nullable
    private GlobalSession create(GameType type, GlobalGameConfig.Entry entry, StartOptions options, String[] args) {
        if (type.isGlobalOnly()) return CountSession.create(plugin, this, entry, args);
        if (!type.isRace()) return QuestionStream.create(plugin, this, entry, options, args);
        Race race = plugin.getRaceManager().prepareGlobal(type, options, args);
        if (race == null) return null;
        // An amount given to the start (e.g. "STONE 500") is the goal
        if (args.length > 1) entry = entry.withGoal(race.getAmount());
        return new GlobalRace(plugin, this, entry, race);
    }

    /**
     * Called when a regular game or race ends: maybe starts a global game a few seconds later.
     */
    public void afterRegularGame() {
        GlobalGameConfig config = this.config;
        if (plugin.getEventManager().isRunning()
                || config.getChanceAfterGame() <= 0 || plugin.getRandom().nextDouble() * 100 >= config.getChanceAfterGame()) return;
        plugin.getScheduler().globalLater(this::startRandom, config.getDelaySeconds() * 20L);
    }

    private void startRandom() {
        GlobalGameConfig config = this.config;
        if (plugin.isInGame() || plugin.getEventManager().isRunning()
                || plugin.getGameManager().getGamePlayerManager().getOnlinePlayers() < config.getMinimumPlayers()) return;
        boolean races = ConfigValue.getBoolean(ConfigPaths.RACES_ENABLED);
        GameType type = config.pick(plugin.getRandom(), game -> races || !game.isRace());
        if (type != null) start(type);
    }

    /**
     * A chat message during a global game. Async chat thread.
     *
     * @return true to hide the message (an answer, or a counted number)
     */
    public boolean onChat(@Nonnull Player player, @Nonnull String message) {
        GlobalSession session = current;
        if (session == null || session.getProgress().isOver() || !canPlay(player)) return false;
        return session.onChat(player, message);
    }

    /**
     * {@code /chatgames <key>} from clicking a Clickable question.
     *
     * @return true if it was the current question's key
     */
    public boolean onClick(@Nonnull Player player, @Nonnull String key) {
        GlobalSession session = current;
        return session != null && !session.getProgress().isOver() && canPlay(player) && session.onClick(player, key);
    }

    /**
     * Progress in a global race, from the race listeners. Any thread.
     */
    public void raceProgress(@Nonnull Race race, @Nonnull Player player, int amount) {
        GlobalSession session = current;
        if (session instanceof GlobalRace && ((GlobalRace) session).getRace() == race) {
            ((GlobalRace) session).progress(player, amount);
        }
    }

    /**
     * Adds a player's contribution to the shared score; reaching the goal ends the game.
     * Any thread.
     *
     * @return what changed, or null if it no longer counts (the game ended)
     */
    @Nullable
    CoopProgress.Step contribute(@Nonnull GlobalSession session, @Nonnull Player player, int amount) {
        if (current != session) return null;
        CoopProgress.Step step = session.getProgress().add(player.getUniqueId(), amount);
        if (step == null) return null;
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data != null) data.incrementStat(Stat.CONTRIBUTIONS, amount);

        if (step.isCompleted()) {
            plugin.getScheduler().global(() -> conclude(session));
        } else if (session.announcesMilestones()) {
            List<Integer> crossed = CoopProgress.crossed(step.getBefore(), step.getAfter(),
                    session.getProgress().getGoal(), config.getAnnouncedPercents());
            if (!crossed.isEmpty()) {
                int percent = crossed.get(crossed.size() - 1);
                plugin.getScheduler().global(() -> {
                    if (current != session) return;
                    Msg milestone = session.fill(Msg.of(messageList("milestone"))).with("%reached%", percent)
                            .with("%progress%", step.getAfter());
                    for (Player online : players()) milestone.send(online);
                });
            }
        }
        return step;
    }

    /** Ends a game as failed (a broken count, no more questions). Any thread. */
    void fail(@Nonnull GlobalSession session) {
        if (session.getProgress().close(CoopProgress.Outcome.FAILED)) {
            plugin.getScheduler().global(() -> conclude(session));
        }
    }

    /**
     * Ends the global game in play with no results or rewards. Main/global thread.
     *
     * @return false if none was running
     */
    public boolean stop() {
        GlobalSession session = current;
        if (session == null || !session.getProgress().close(CoopProgress.Outcome.STOPPED)) return false;
        conclude(session);
        return true;
    }

    private void tick(GlobalSession session) {
        if (current != session) return;
        if (session.getSecondsLeft() <= 0) {
            fail(session);
            return;
        }
        ProgressBar shown = bar;
        if (shown == null) return;
        CoopProgress progress = session.getProgress();
        List<String> lines = session.fill(Msg.of(message("bar"))).render(null);
        String text = lines.isEmpty() ? "" : lines.get(0);
        shown.show(players(), text, progress.getProgress() / (double) progress.getGoal());
    }

    /** Announces the result, pays everyone out and frees the game slot. Main/global thread. */
    private void conclude(GlobalSession session) {
        if (current != session) return;
        current = null;
        session.cancelTasks();
        session.end();
        hideBar();

        CoopProgress progress = session.getProgress();
        CoopProgress.Outcome outcome = progress.getOutcome();
        if (outcome == null) outcome = CoopProgress.Outcome.STOPPED;
        List<CoopProgress.Contributor> ranking = progress.ranking();
        boolean completed = outcome == CoopProgress.Outcome.COMPLETED;
        if (outcome != CoopProgress.Outcome.STOPPED) {
            announceEnd(session, ranking, completed);
            payOut(session, ranking, completed);
        }

        List<PlayerResult> results = new ArrayList<>();
        if (completed) {
            double seconds = session.getElapsedSeconds();
            for (CoopProgress.Contributor contributor : ranking) {
                results.add(new PlayerResult(contributor.getUuid(), seconds, contributor.getPlace()));
            }
        }
        Bukkit.getPluginManager().callEvent(new ChatGamesEndEvent(session.getType(), "global: " + session.getGoalText(), results));
        List<ChatGamesGlobalEndEvent.Contributor> contributors = new ArrayList<>();
        for (CoopProgress.Contributor contributor : ranking) {
            contributors.add(new ChatGamesGlobalEndEvent.Contributor(contributor.getUuid(), contributor.getAmount(), contributor.getPlace()));
        }
        Bukkit.getPluginManager().callEvent(new ChatGamesGlobalEndEvent(session.getType(), ChatGamesGlobalEndEvent.Outcome.valueOf(outcome.name()),
                progress.getProgress(), progress.getGoal(), session.getEntry().getMinContribution(), contributors));
        plugin.getGameManager().endGame();
        plugin.getLeaderboardCache().refresh();
    }

    private void announceEnd(GlobalSession session, List<CoopProgress.Contributor> ranking, boolean completed) {
        String none = ConfigManager.getMessagesConfig().getString("no_winner_placeholder", "---");
        Msg end = session.fill(Msg.of(messageList(completed ? "completed" : "failed")))
                .with("%count%", rewardedCount(session, ranking))
                .with("%contributors%", ranking.size())
                .with("%seconds%", Math.round(session.getElapsedSeconds()));
        for (int place = 1; place <= 3; place++) {
            CoopProgress.Contributor top = place <= ranking.size() ? ranking.get(place - 1) : null;
            String name = top == null ? null : Bukkit.getOfflinePlayer(top.getUuid()).getName();
            end.with("%top_" + place + "%", name != null ? name : none)
                    .with("%top_" + place + "_amount%", top != null ? String.valueOf(top.getAmount()) : none);
        }
        for (Player player : players()) {
            end.send(player);
            if (session.contribution(player.getUniqueId()) > 0) {
                GameSoundManager.playGameSound(player, completed ? "win" : "time-expired");
            }
        }
    }

    private static int rewardedCount(GlobalSession session, List<CoopProgress.Contributor> ranking) {
        int count = 0;
        for (CoopProgress.Contributor contributor : ranking) {
            if (contributor.getAmount() >= session.getEntry().getMinContribution()) count++;
        }
        return count;
    }

    /**
     * Stats for everyone who took part; points, rewards, challenges and streaks for those
     * who contributed at least {@code min-contribution}. Players who left keep the stats.
     */
    private void payOut(GlobalSession session, List<CoopProgress.Contributor> ranking, boolean completed) {
        GlobalGameConfig.Entry entry = session.getEntry();
        GameType type = session.getType();
        for (CoopProgress.Contributor contributor : ranking) {
            int amount = contributor.getAmount();
            boolean rewarded = amount >= entry.getMinContribution();
            int points = completed ? entry.pointsFor(amount) : 0;
            ChatGamesPlayerManager.modify(contributor.getUuid(), data -> {
                data.incrementStat(Stat.GLOBAL_PLAYED, 1);
                if (completed && rewarded) data.incrementStat(Stat.GLOBAL_WON, 1);
            });
            Player player = Bukkit.getPlayer(contributor.getUuid());
            if (player == null) continue;
            if (!rewarded) {
                Msg.of(message("too_little")).with("%contribution%", amount).with("%min%", entry.getMinContribution()).send(player);
                // Taking part still counts for "play global games" challenges and the global streak
                plugin.getProgressionManager().record(player, Activity.global(type, 0, false));
                continue;
            }
            if (points > 0) TopManager.addPoints(player, points);
            List<String> sections = new ArrayList<>();
            sections.add(completed ? "completed" : "failed");
            if (completed) sections.add("top-contributors." + contributor.getPlace());
            plugin.getGameUtils().giveGlobalRewards(type, player, sections, amount);
            if (completed) {
                Msg.of(message("rewarded")).with("%contribution%", amount).with("%points%", points).send(player);
            }
            plugin.getProgressionManager().record(player, Activity.global(type, points, completed));
        }
    }

    private void hideBar() {
        ProgressBar shown = bar;
        bar = null;
        if (shown != null) shown.hide();
    }

    /** @return the players who can play: in an enabled world, with games toggled on */
    @Nonnull
    List<Player> players() {
        List<Player> players = new ArrayList<>();
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            if (canPlay(player)) players.add(player);
        }
        return players;
    }

    private boolean canPlay(Player player) {
        GameManager games = plugin.getGameManager();
        return games.worldEnabled(player) && games.gamesIsToggled(player);
    }

    /** A message from the messages section of global_games.yml. */
    @Nullable
    static String message(@Nonnull String key) {
        return ConfigManager.getGlobalGames().getString("messages." + key);
    }

    @Nonnull
    static List<String> messageList(@Nonnull String key) {
        ConfigurationSection file = ConfigManager.getGlobalGames();
        if (file.isList("messages." + key)) return file.getStringList("messages." + key);
        String line = file.getString("messages." + key);
        return line == null ? Collections.<String>emptyList() : Collections.singletonList(line);
    }

    /** A word from the {@code words} section of global_games.yml, e.g. "answers". */
    @Nonnull
    static String word(@Nonnull String key) {
        return ConfigManager.getGlobalGames().getString("words." + key, key);
    }

    /**
     * @param keys sections under {@code rewards}, e.g. {@code completed} and {@code top-contributors.1}
     * @return those reward sections from {@code games.<game>.rewards} in global_games.yml, or
     *         from the shared {@code rewards} if the game has none of its own
     */
    @Nonnull
    public static List<ConfigurationSection> rewardSections(@Nonnull GameType type, @Nonnull List<String> keys) {
        List<ConfigurationSection> sections = new ArrayList<>();
        ConfigurationSection rewards = ConfigManager.getGlobalGames().getConfigurationSection("games." + type.getConfigKey() + ".rewards");
        if (rewards == null) rewards = ConfigManager.getGlobalGames().getConfigurationSection("rewards");
        if (rewards == null) return sections;
        for (String key : keys) {
            ConfigurationSection section = rewards.getConfigurationSection(key);
            if (section != null) sections.add(section);
        }
        return sections;
    }

    /** @return a player's contribution to the game in play, 0 if none is */
    public int contribution(@Nonnull UUID player) {
        GlobalSession session = current;
        return session == null ? 0 : session.contribution(player);
    }
}
