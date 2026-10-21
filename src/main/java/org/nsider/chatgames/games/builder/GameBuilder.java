package org.nsider.chatgames.games.builder;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.events.ChatGamesStartEvent;
import org.nsider.chatgames.games.Game;
import org.nsider.chatgames.games.manager.GameConfigManager;
import org.nsider.chatgames.games.manager.GameManager;
import org.nsider.chatgames.games.manager.RoundAnnouncer;
import org.nsider.chatgames.games.round.GameSettings;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.games.round.RoundContext;
import org.nsider.chatgames.games.round.RoundGenerator;
import org.nsider.chatgames.games.round.RoundGenerators;
import org.nsider.chatgames.globalgames.GlobalGameConfig;
import org.nsider.chatgames.games.StartOptions;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;
import java.util.logging.Level;

/**
 * Starts chat games:
 * <ol>
 *   <li>generate the round (nothing is shown yet, so a broken word list costs nothing)</li>
 *   <li>reserve the game slot and fire {@link ChatGamesStartEvent}; a cancelled event stops here</li>
 *   <li>announce it; for Memorize, wait while the list is on screen</li>
 *   <li>open it for answers and schedule the time limit, hints and countdown</li>
 * </ol>
 * Runs on the main thread (the global region on Folia).
 */
public class GameBuilder {

    private final ChatGames plugin;

    public GameBuilder(ChatGames instance) {
        this.plugin = instance;
    }

    /**
     * @param args optional values for the round, e.g. the word to use
     * @return true if the game started
     */
    public boolean start(@Nonnull GameType type, @Nonnull String... args) {
        return start(type, StartOptions.NORMAL, args);
    }

    /**
     * Starts a global game: everyone who answers within the global time limit places.
     *
     * @return true if the game started
     */
    public boolean startGlobal(@Nonnull GameType type, @Nonnull GlobalGameConfig.Entry global, @Nonnull String... args) {
        return start(type, StartOptions.global(global), args);
    }

    /**
     * Starts a game with some of its settings replaced, and/or as a global game.
     *
     * @return true if the game started
     */
    public boolean start(@Nonnull GameType type, @Nonnull StartOptions options, @Nonnull String... args) {
        // Overridden settings (an event's difficulty, say) also reach the round generator
        ConfigurationSection section = options.isPlain() ? GameConfigManager.section(ConfigManager.getGames(), type)
                : options.apply(GameConfigManager.section(ConfigManager.getGames(), type));
        Round round = generate(type, section, args);
        if (round == null) return false;
        GameSettings settings = options.isPlain() ? plugin.getGameConfigManager().settings(type)
                : new GameSettings(type, section);
        boolean global = options.isGlobal();

        GameManager manager = plugin.getGameManager();
        if (!manager.tryBegin()) return false;

        ChatGamesStartEvent event = new ChatGamesStartEvent(type, round.getMainAnswer());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            manager.endGame();
            return false;
        }

        RoundAnnouncer announcer = manager.getAnnouncer();
        announcer.announceStart(round, settings, global);
        if (round.getMemorizeSeconds() > 0) {
            // Answers open once the list is gone; endGame() cancels this if the game is stopped meanwhile
            manager.addTask(plugin.getScheduler().globalLater(() -> {
                announcer.announceMemorizeOver(round, settings);
                open(round, settings, global);
            }, round.getMemorizeSeconds() * 20L));
        } else {
            open(round, settings, global);
        }
        return true;
    }

    /**
     * Builds a round without starting it, e.g. for previews and tests.
     *
     * @return null (and a console warning) if the game's settings or word lists can't make one
     */
    @Nullable
    public Round generate(@Nonnull GameType type, @Nonnull String... args) {
        return generate(type, GameConfigManager.section(ConfigManager.getGames(), type), args);
    }

    @Nullable
    private Round generate(GameType type, @Nullable ConfigurationSection settings, String[] args) {
        RoundGenerator generator = RoundGenerators.get(type);
        if (generator == null) return null;
        RoundContext context = new RoundContext(
                settings,
                ConfigManager.getWords(),
                GameConfigManager.section(ConfigManager.getMessagesConfig(), type),
                plugin.getRandom(),
                key -> plugin.getLanguageManager() != null ? plugin.getLanguageManager().translate(key) : null);
        try {
            return generator.generate(context, args);
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Could not start " + type.getConfigKey() + ": " + e.getMessage());
            return null;
        }
    }

    private void open(Round round, GameSettings settings, boolean global) {
        GameManager manager = plugin.getGameManager();
        Game game = new Game(round, settings, global);
        manager.activate(game);

        manager.addTask(plugin.getScheduler().globalLater(() -> manager.conclude(game), settings.getTimeSeconds() * 20L));

        if (settings.isHintsEnabled()) {
            long[] delays = settings.hintDelaysTicks(round.getHints().size());
            for (int i = 0; i < delays.length; i++) {
                String hint = round.getHints().get(i);
                manager.addTask(plugin.getScheduler().globalLater(() -> {
                    if (!game.isFinished()) manager.getAnnouncer().announceHint(game, round, settings, hint);
                }, delays[i]));
            }
        }

        if (settings.isCountdownEnabled()) {
            int window = settings.getCountdownLastSeconds();
            manager.addTask(plugin.getScheduler().globalTimer(() -> {
                if (game.isFinished()) return;
                int left = (int) Math.ceil(settings.getTimeSeconds() - game.getElapsedSeconds());
                if (left > 0 && (window == 0 || left <= window)) {
                    manager.getAnnouncer().showCountdown(game, round, left);
                }
            }, 1L, 20L));
        }
    }
}
