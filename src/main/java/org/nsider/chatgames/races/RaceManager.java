package org.nsider.chatgames.races;

import com.cryptomorin.xseries.messages.Titles;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.events.ChatGamesEndEvent;
import org.nsider.chatgames.events.ChatGamesStartEvent;
import org.nsider.chatgames.games.manager.GameManager;
import org.nsider.chatgames.games.manager.GameSoundManager;
import org.nsider.chatgames.games.utils.PlayerResult;
import org.nsider.chatgames.globalgames.GlobalGameConfig;
import org.nsider.chatgames.managers.TopManager;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.chatgames.progression.Activity;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.chatgames.utils.management.TimeUtils;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.chatgames.utils.messages.Msg;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Runs races: start, the winner, time running out and stopping. Shares the game
 * slot with chat games ({@link GameManager#tryBegin()}), so only one of either runs.
 *
 * <p>{@link RaceListener} reports progress from whichever thread the action happened
 * on; the race itself is started and ended on the main thread (the global region on
 * Folia).</p>
 */
public final class RaceManager {

    private static final int DEFAULT_TIME_LIMIT = 180;
    private static final int DEFAULT_POINTS = 1;

    private final ChatGames plugin;
    private volatile Map<GameType, RaceData> data = Collections.emptyMap();
    @Nullable private volatile Race current;
    @Nullable private ChatGamesScheduler.Task expiry;
    /** Who finished the running global race, by place. */
    private final List<PlayerResult> finishers = Collections.synchronizedList(new ArrayList<PlayerResult>());

    public RaceManager(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
        load();
    }

    /** Reads every race's targets and amounts from races.yml. */
    public void load() {
        Map<GameType, RaceData> loaded = new EnumMap<>(GameType.class);
        for (GameType type : GameType.values()) {
            if (!type.isRace()) continue;
            RaceData races = RaceData.load(section(type).getConfigurationSection("data"),
                    problem -> plugin.getLogger().warning("races.yml: " + problem));
            for (String target : races.getTargets()) {
                if (RaceTarget.resolve(type, target) == null) {
                    plugin.getLogger().info("races.yml: " + type.getConfigKey() + " target " + target
                            + " doesn't exist on this server version and will be skipped.");
                }
            }
            loaded.put(type, races);
        }
        data = loaded;
    }

    @Nonnull
    public RaceData getData(@Nonnull GameType race) {
        RaceData races = data.get(race);
        return races == null ? RaceData.EMPTY : races;
    }

    /** @return the running race, or null */
    @Nullable
    public Race getCurrent() {
        return current;
    }

    /**
     * Starts a race. Main/global thread.
     *
     * @param args optional target and amount, e.g. {@code DIAMOND_ORE 5}; without an amount
     *             one configured for that target is used
     * @return false if something is running, the values are invalid, nothing in races.yml
     *         exists on this version, or a plugin cancelled it (a console warning says which)
     */
    public boolean start(@Nonnull GameType type, @Nonnull String... args) {
        return start(prepare(type, null, args));
    }

    /**
     * Starts a global race: everyone who gets there within the global time limit finishes.
     * Main/global thread.
     *
     * @return false for the same reasons as {@link #start(GameType, String...)}
     */
    public boolean startGlobal(@Nonnull GameType type, @Nonnull GlobalGameConfig.Entry global, @Nonnull String... args) {
        return start(prepare(type, global, args));
    }

    private boolean start(@Nullable Race race) {
        if (race == null) return false;
        GameType type = race.getType();

        GameManager manager = plugin.getGameManager();
        if (!manager.tryBegin()) return false;
        current = race;
        finishers.clear();
        ChatGamesStartEvent event = new ChatGamesStartEvent(type, null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            current = null;
            manager.endGame();
            return false;
        }

        announceStart(race);
        expiry = plugin.getScheduler().globalLater(() -> expire(race), race.getTimeLimitSeconds() * 20L);
        return true;
    }

    @Nullable
    private Race prepare(GameType type, @Nullable GlobalGameConfig.Entry global, String[] args) {
        RaceData races = getData(type);
        String target;
        int amount;
        if (args.length == 0) {
            RaceData.Pick pick = races.pick(plugin.getRandom(), name -> RaceTarget.resolve(type, name) != null);
            if (pick == null) {
                plugin.getLogger().warning("Can't start the " + type.getConfigKey()
                        + " race: none of its targets in races.yml exist on this server version.");
                return null;
            }
            target = pick.getTarget();
            amount = pick.getAmount();
        } else {
            target = args[0];
            List<Integer> configured = races.getAmounts(target);
            if (args.length > 1) {
                amount = parseAmount(args[1]);
            } else {
                amount = configured.isEmpty() ? 0 : configured.get(plugin.getRandom().nextInt(configured.size()));
            }
            if (amount <= 0) {
                plugin.getLogger().warning("Can't start the " + type.getConfigKey() + " race: give an amount above 0 for " + target + ".");
                return null;
            }
        }
        RaceTarget resolved = RaceTarget.resolve(type, target);
        if (resolved == null) {
            plugin.getLogger().warning("Can't start the " + type.getConfigKey() + " race: " + target
                    + " is not a valid " + (type == GameType.HUNT ? "mob" : "material") + " on this server version.");
            return null;
        }
        int time = global != null ? global.getDurationSeconds() : section(type).getInt("timeToComplete_seconds", DEFAULT_TIME_LIMIT);
        return new Race(type, resolved, resolved.displayName(plugin.getLanguageManager()), amount,
                races.getRewards(resolved.getName(), amount), time > 0 ? time : DEFAULT_TIME_LIMIT, global != null);
    }

    /**
     * Counts an action towards the running race. Any thread.
     *
     * @param race the race the action was checked against (a newer one may have started since)
     */
    public void progress(@Nonnull Race race, @Nonnull Player player, int amount) {
        int place = race.getProgress().addAndPlace(player.getUniqueId(), amount);
        if (place == 0) return;
        double seconds = race.getElapsedSeconds();
        if (race.isGlobal()) {
            plugin.getScheduler().global(() -> finish(race, player, seconds, place));
        } else {
            plugin.getScheduler().global(() -> win(race, player, seconds));
        }
    }

    /** A player made it in a global race, which goes on until time is up. */
    private void finish(Race race, Player player, double seconds, int place) {
        if (current != race) return;
        finishers.add(new PlayerResult(player.getUniqueId(), seconds, place));
        plugin.getGlobalGameManager().award(player, race.getType(), seconds, place);
    }

    private void win(Race race, Player player, double seconds) {
        if (current != race) return;
        ChatGamesScheduler.cancel(expiry);

        GameType type = race.getType();
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data != null) {
            data.incrementStat(Stat.WINS, 1);
            data.incrementStat(Stat.RACE_WINS, 1);
            data.incrementStat(Stat.wins(type), 1);
        }
        int points = section(type).getInt("points", DEFAULT_POINTS);
        if (points > 0) TopManager.addPoints(player, points);
        if (ConfigValue.getBoolean(ConfigPaths.FASTEST_TYPERS_TOP)) {
            TopManager.recordTime(player, type, seconds);
        }

        List<PlayerResult> winners = Collections.singletonList(new PlayerResult(player.getUniqueId(), seconds, 1));
        Bukkit.getPluginManager().callEvent(new ChatGamesEndEvent(type, null, winners));

        Msg message = message(race, "correct_message")
                .with("%winner%", player.getName())
                .with("%winner_displayname%", player.getDisplayName())
                .with("%time%", TimeUtils.roundTo2Decimals(seconds))
                .with("%points%", TopManager.getPoints(player));
        for (Player online : players()) {
            if (!plugin.getGameManager().worldEnabled(online)) continue;
            message.send(online);
        }
        GameSoundManager.playGameSound(player, "win");

        plugin.getGameUtils().giveRaceRewards(type.getConfigKey(), player, race.getRewards());
        plugin.getProgressionManager().record(player, Activity.win(type, seconds, Math.max(0, points)));
        plugin.getLeaderboardCache().refresh();
        end(race);
        plugin.getGlobalGameManager().afterRegularGame();
    }

    private void expire(Race race) {
        if (current != race || !race.getProgress().close()) return;
        if (race.isGlobal()) {
            List<PlayerResult> results;
            synchronized (finishers) {
                results = new ArrayList<>(finishers);
            }
            results.sort((a, b) -> Integer.compare(a.getPlace(), b.getPlace()));
            Bukkit.getPluginManager().callEvent(new ChatGamesEndEvent(race.getType(), null, results));
            plugin.getGlobalGameManager().announceEnd(race.getType(), race.getAmount() + " " + race.getDisplayName(), results);
            end(race);
            return;
        }
        Bukkit.getPluginManager().callEvent(new ChatGamesEndEvent(race.getType(), null, Collections.<PlayerResult>emptyList()));
        Msg message = message(race, "time_expired_message");
        for (Player player : players()) {
            if (!plugin.getGameManager().worldEnabled(player)) continue;
            message.send(player);
            if (plugin.getGameManager().gamesIsToggled(player)) GameSoundManager.playGameSound(player, "time-expired");
        }
        end(race);
        plugin.getGlobalGameManager().afterRegularGame();
    }

    /**
     * Ends the running race with no winner and no announcement. Main/global thread.
     *
     * @param notify fire {@link ChatGamesEndEvent} (not while the plugin shuts down)
     * @return false if no race was running
     */
    public boolean stop(boolean notify) {
        Race race = current;
        if (race == null || !race.getProgress().close()) return false;
        if (notify) {
            Bukkit.getPluginManager().callEvent(new ChatGamesEndEvent(race.getType(), null, Collections.<PlayerResult>emptyList()));
        }
        end(race);
        return true;
    }

    private void end(Race race) {
        if (current != race) return;
        ChatGamesScheduler.cancel(expiry);
        expiry = null;
        current = null;
        plugin.getGameManager().endGame();
    }

    private void announceStart(Race race) {
        Msg message = race.isGlobal()
                ? plugin.getGlobalGameManager().startMessage(race.getType(), race.getTimeLimitSeconds())
                        .with("%value%", race.getDisplayName()).with("%amount%", race.getAmount())
                : message(race, "gameStartAnnouncement");
        ConfigurationSection settings = section(race.getType());
        String title = fill(race, settings.getString("titleMessage", ""));
        String subtitle = fill(race, settings.getString("subtitleMessage", ""));
        for (Player player : players()) {
            if (!plugin.getGameManager().worldEnabled(player) || !plugin.getGameManager().gamesIsToggled(player)) continue;
            GameSoundManager.playGameSound(player, "game-start");
            message.send(player);
            if (!title.isEmpty() || !subtitle.isEmpty()) {
                Titles.sendTitle(player, 10, 60, 10, MessageUtils.color(title), MessageUtils.color(subtitle));
            }
        }
    }

    private static Msg message(Race race, String key) {
        ConfigurationSection messages = ConfigManager.getMessagesConfig().getConfigurationSection(race.getType().getConfigKey());
        return Msg.of(messages == null ? null : messages.getStringList(key))
                .with("%value%", race.getDisplayName())
                .with("%amount%", race.getAmount())
                .with("%timeToComplete%", race.getTimeLimitSeconds())
                .with("%game%", race.getType().getConfigKey());
    }

    private static String fill(Race race, String text) {
        return text.replace("%value%", race.getDisplayName())
                .replace("%amount%", String.valueOf(race.getAmount()))
                .replace("%timeToComplete%", String.valueOf(race.getTimeLimitSeconds()));
    }

    /** @return the race's section in races.yml (empty if missing) */
    @Nonnull
    public static ConfigurationSection section(@Nonnull GameType race) {
        ConfigurationSection section = ConfigManager.getRaces().getConfigurationSection(race.getConfigKey());
        return section != null ? section : new MemoryConfiguration();
    }

    private static int parseAmount(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static List<Player> players() {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }
}
