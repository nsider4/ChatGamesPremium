package org.nsider.chatgames.globalgames;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.manager.GameSoundManager;
import org.nsider.chatgames.games.utils.PlayerResult;
import org.nsider.chatgames.managers.TopManager;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.chatgames.progression.Activity;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.chatgames.utils.management.TimeUtils;
import org.nsider.chatgames.utils.messages.Msg;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Global games: a chat game or race where everyone who makes it in time is rewarded,
 * not just the first few. They run through the normal game and race code with
 * {@code global} set (see {@link org.nsider.chatgames.games.Game#isGlobal()} and
 * {@link org.nsider.chatgames.races.Race#isGlobal()}); this class decides when they
 * start, pays everyone who makes it, and announces the result.
 *
 * <p>{@link #award} runs on whichever thread the answer or race action arrived on;
 * starting and announcing happen on the main/global thread.</p>
 */
public final class GlobalGameManager {

    private final ChatGames plugin;
    private volatile GlobalGameConfig config = GlobalGameConfig.EMPTY;
    @Nullable private ChatGamesScheduler.Task interval;

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

    /** Stops the interval timer, for shutdown. */
    public void shutdown() {
        ChatGamesScheduler.cancel(interval);
        interval = null;
    }

    @Nonnull
    public GlobalGameConfig getConfig() {
        return config;
    }

    /**
     * Starts a global game now. Main/global thread.
     *
     * @param args optional values for the round or race, as for a normal start
     * @return false if it isn't enabled in global_games.yml, something is running, or it couldn't start
     */
    public boolean start(@Nonnull GameType type, @Nonnull String... args) {
        GlobalGameConfig.Entry entry = config.get(type);
        return entry != null && plugin.getGameManager().startGlobal(type, entry, args);
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
        if (plugin.isInGame() || plugin.getEventManager().isRunning() || plugin.getGameManager().getGamePlayerManager().getOnlinePlayers() < config.getMinimumPlayers()) return;
        boolean races = ConfigValue.getBoolean(ConfigPaths.RACES_ENABLED);
        GameType type = config.pick(plugin.getRandom(), game -> races || !game.isRace());
        if (type != null) start(type);
    }

    /**
     * Pays a player who made it: points, stats, rewards (taking part, plus their place's),
     * challenges and streaks. Any thread.
     *
     * @param place 1 for the first to make it
     */
    public void award(@Nonnull Player player, @Nonnull GameType type, double seconds, int place) {
        GlobalGameConfig config = this.config;
        int points = config.find(type).getPoints();
        boolean placed = place <= config.getRewardedPlaces();

        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data != null) {
            data.incrementStat(Stat.GLOBAL_PLAYED, 1);
            if (placed) data.incrementStat(Stat.GLOBAL_WON, 1);
        }
        if (points > 0) TopManager.addPoints(player, points);
        if (ConfigValue.getBoolean(ConfigPaths.FASTEST_TYPERS_TOP)) TopManager.recordTime(player, type, seconds);

        Msg.of(message("solved")).with("%time%", TimeUtils.roundTo2Decimals(seconds)).with("%place%", place)
                .with("%points%", points).send(player);
        GameSoundManager.playGameSound(player, "win");

        plugin.getGameUtils().giveGlobalRewards(type, player, place);
        plugin.getProgressionManager().record(player, Activity.global(type, seconds, points, placed));
    }

    /**
     * Announces the end of a global game or race. Main/global thread.
     *
     * @param answer  the chat game's answer, or the race's goal (e.g. "16 Stone")
     * @param results everyone who made it, by place
     */
    public void announceEnd(@Nonnull GameType type, @Nonnull String answer, @Nonnull List<PlayerResult> results) {
        String none = ConfigManager.getMessagesConfig().getString("no_winner_placeholder", "---");
        Msg message = Msg.of(messageList(results.isEmpty() ? "nobody" : "end"))
                .with("%game%", type.getConfigKey())
                .with("%correct_answer%", answer)
                .with("%count%", results.size());
        for (int place = 1; place <= 3; place++) {
            PlayerResult result = place <= results.size() ? results.get(place - 1) : null;
            String name = result == null ? null : result.getPlayer().getName();
            message.with("%winner_" + place + "%", name != null ? name : none)
                    .with("%time_" + place + "%", result != null ? String.valueOf(TimeUtils.roundTo2Decimals(result.getTime())) : none);
        }
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            if (plugin.getGameManager().worldEnabled(player)) message.send(player);
        }
        plugin.getLeaderboardCache().refresh();
    }

    /**
     * @return the global announcement for a game (above its question) or a race, with
     *         {@code %time%} filled in
     */
    @Nonnull
    public Msg startMessage(@Nonnull GameType type, int seconds) {
        return Msg.of(messageList(type.isRace() ? "race_start" : "start"))
                .with("%time%", seconds)
                .with("%game%", type.getConfigKey());
    }

    /** A message from the messages section of global_games.yml. */
    @Nullable
    private static String message(String key) {
        return ConfigManager.getGlobalGames().getString("messages." + key);
    }

    private static List<String> messageList(String key) {
        return ConfigManager.getGlobalGames().getStringList("messages." + key);
    }

    /**
     * @return the reward sections a player gets for making it: {@code participation}, then
     *         their place's, from {@code games.<game>.rewards} in global_games.yml, or the
     *         shared {@code rewards} if the game has none of its own
     */
    @Nonnull
    public static List<ConfigurationSection> rewardSections(@Nonnull GameType type, int place) {
        List<ConfigurationSection> sections = new ArrayList<>();
        ConfigurationSection rewards = ConfigManager.getGlobalGames().getConfigurationSection("games." + type.getConfigKey() + ".rewards");
        if (rewards == null) rewards = ConfigManager.getGlobalGames().getConfigurationSection("rewards");
        if (rewards == null) return sections;
        for (String key : new String[]{"participation", String.valueOf(place)}) {
            ConfigurationSection section = rewards.getConfigurationSection(key);
            if (section != null) sections.add(section);
        }
        return sections;
    }

}
