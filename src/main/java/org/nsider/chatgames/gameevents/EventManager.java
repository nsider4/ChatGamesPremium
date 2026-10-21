package org.nsider.chatgames.gameevents;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.events.ChatGamesEndEvent;
import org.nsider.chatgames.events.ChatGamesEventEndEvent;
import org.nsider.chatgames.events.ChatGamesEventStartEvent;
import org.nsider.chatgames.games.utils.PlayerResult;
import org.nsider.chatgames.listeners.DiscordListener;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.chatgames.progression.Activity;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.chatgames.utils.messages.Msg;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Game events: a series of games played back to back, where players collect event
 * points by placing in each, and the best totals win the event's rewards.
 *
 * <p>While an event runs the rotation and the global game triggers hold off, so the
 * only games are the event's. Each step starts through the normal game code; the
 * step ends when {@link ChatGamesEndEvent} fires for it, however it ended (solved,
 * timed out or stopped). Everything here runs on the main/global thread; placeholders
 * read {@link #getRunning()} from any.</p>
 */
public final class EventManager implements Listener {

    /** How often scheduled start times are checked. */
    private static final long CHECK_TICKS = 30 * 20L;
    /** A step waits this many times, two seconds apart, for a game started by hand to end. */
    private static final int BUSY_RETRIES = 15;
    private static final long FINISH_DELAY_TICKS = 3 * 20L;

    public enum StartResult { STARTED, UNKNOWN, ALREADY_RUNNING, NOT_ENOUGH_PLAYERS, CANCELLED }

    private final ChatGames plugin;
    private volatile EventConfig config = EventConfig.EMPTY;
    @Nullable private volatile Run run;
    @Nullable private ChatGamesScheduler.Task checker;
    private ZonedDateTime lastCheck;

    public EventManager(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
        load();
    }

    /** Reads events.yml and restarts the schedule checks. A running event keeps its old settings. */
    public void load() {
        config = EventConfig.load(ConfigManager.getEvents(), problem -> plugin.getLogger().warning("events.yml: " + problem));
        ChatGamesScheduler.cancel(checker);
        lastCheck = ZonedDateTime.now(zone());
        checker = plugin.getScheduler().globalTimer(this::checkSchedules, CHECK_TICKS, CHECK_TICKS);
    }

    /** Stops the checks and drops a running event without results, for shutdown. */
    public void shutdown() {
        ChatGamesScheduler.cancel(checker);
        checker = null;
        Run current = run;
        run = null;
        if (current != null) ChatGamesScheduler.cancel(current.pending);
    }

    @Nonnull
    public EventConfig getConfig() {
        return config;
    }

    /** Events follow the challenges' time zone, so "SATURDAY 20:00" means the same everywhere. */
    @Nonnull
    public ZoneId zone() {
        return plugin.getProgressionManager().getChallenges().getZone();
    }

    public boolean isRunning() {
        return run != null;
    }

    /** @return the running event, or null */
    @Nullable
    public Run getRunning() {
        return run;
    }

    /**
     * Starts an event: its first game follows after its pause.
     *
     * @param scheduled true when started by its schedule, which needs its minimum players
     */
    @Nonnull
    public StartResult start(@Nonnull String id, boolean scheduled) {
        EventDefinition definition = config.get(id);
        if (definition == null) return StartResult.UNKNOWN;
        if (run != null) return StartResult.ALREADY_RUNNING;
        if (scheduled && plugin.getGameManager().getGamePlayerManager().getOnlinePlayers() < definition.getMinimumPlayers()) {
            return StartResult.NOT_ENOUGH_PLAYERS;
        }
        ChatGamesEventStartEvent event = new ChatGamesEventStartEvent(definition, scheduled);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return StartResult.CANCELLED;
        Run started = new Run(definition);
        run = started;
        broadcast(Msg.of(messages("start")), started);
        scheduleStep(started, definition.getPauseSeconds());
        return StartResult.STARTED;
    }

    /**
     * Ends the running event now, with no results or rewards; its running game stops too.
     *
     * @return the event's definition, or null if none was running
     */
    @Nullable
    public EventDefinition stop() {
        Run current = run;
        if (current == null) return null;
        run = null;
        ChatGamesScheduler.cancel(current.pending);
        if (current.awaiting) plugin.getGameManager().stopCurrent();
        broadcast(Msg.of(messages("stopped")), current);
        Bukkit.getPluginManager().callEvent(new ChatGamesEventEndEvent(current.definition, Collections.emptyList(), true));
        return current.definition;
    }

    private void checkSchedules() {
        ZonedDateTime now = ZonedDateTime.now(zone());
        ZonedDateTime from = lastCheck;
        lastCheck = now;
        for (EventDefinition definition : config.getAll()) {
            for (EventSchedule schedule : definition.getSchedule()) {
                if (!schedule.isDue(from, now)) continue;
                StartResult result = start(definition.getId(), true);
                if (result == StartResult.NOT_ENOUGH_PLAYERS) {
                    plugin.getLogger().info("The event " + definition.getId() + " didn't start: fewer than "
                            + definition.getMinimumPlayers() + " players online.");
                } else if (result == StartResult.ALREADY_RUNNING) {
                    plugin.getLogger().info("The event " + definition.getId() + " didn't start: another event is running.");
                }
                break;
            }
        }
    }

    private void scheduleStep(Run current, int seconds) {
        current.pending = plugin.getScheduler().globalLater(() -> runStep(current, 0), Math.max(1, seconds) * 20L);
    }

    private void runStep(Run current, int attempt) {
        if (run != current) return;
        EventStep step = current.definition.getSteps().get(current.step);
        if (plugin.isInGame()) {
            if (attempt < BUSY_RETRIES) {
                current.pending = plugin.getScheduler().globalLater(() -> runStep(current, attempt + 1), 40L);
            } else {
                plugin.getLogger().warning("Event " + current.definition.getId() + ": skipped game " + (current.step + 1)
                        + " (" + step.getType().getConfigKey() + ") because another game kept running.");
                advance(current);
            }
            return;
        }

        broadcast(Msg.of(messages("step")).with("%game%", step.getType().getConfigKey()), current);
        current.awaiting = true;
        boolean started;
        try {
            started = plugin.getGameManager().startGame(step.getType(),
                    step.toOptions(plugin.getGlobalGameManager().getConfig()), step.getArgs());
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Event " + current.definition.getId() + ": game "
                    + (current.step + 1) + " failed to start", e);
            started = false;
        }
        if (!started) {
            current.awaiting = false;
            plugin.getLogger().warning("Event " + current.definition.getId() + ": game " + (current.step + 1)
                    + " (" + step.getType().getConfigKey() + ") couldn't start; skipped. Check its args and settings.");
            advance(current);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameEnd(ChatGamesEndEvent event) {
        Run current = run;
        if (current == null || !current.awaiting
                || event.getType() != current.definition.getSteps().get(current.step).getType()) return;
        current.awaiting = false;

        List<UUID> byPlace = new ArrayList<>();
        for (PlayerResult result : event.getWinners()) byPlace.add(result.getUuid());
        Map<UUID, Integer> earned = current.scores.addStep(byPlace, current.definition.getPlacePoints(),
                current.definition.getParticipationPoints());
        for (Map.Entry<UUID, Integer> entry : earned.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                Msg.of(message("step_points")).with("%points%", entry.getValue())
                        .with("%total%", current.scores.points(entry.getKey()))
                        .with("%event%", current.definition.getName()).send(player);
            }
        }
        advance(current);
    }

    private void advance(Run current) {
        current.step++;
        if (current.step < current.definition.getSteps().size()) {
            scheduleStep(current, current.definition.getPauseSeconds());
        } else {
            current.step--;
            current.pending = plugin.getScheduler().globalLater(() -> finish(current), FINISH_DELAY_TICKS);
        }
    }

    private void finish(Run current) {
        if (run != current) return;
        run = null;
        EventDefinition definition = current.definition;
        List<EventScores.Standing> ranking = current.scores.ranking();

        String none = ConfigManager.getMessagesConfig().getString("no_winner_placeholder", "---");
        Msg end = Msg.of(messages(ranking.isEmpty() ? "nobody" : "end")).with("%count%", ranking.size());
        List<String> discordLines = new ArrayList<>();
        for (int place = 1; place <= 3; place++) {
            EventScores.Standing standing = place <= ranking.size() ? ranking.get(place - 1) : null;
            String name = standing == null ? null : nameOf(standing.getUuid());
            end.with("%winner_" + place + "%", name != null ? name : none)
                    .with("%points_" + place + "%", standing != null ? String.valueOf(standing.getPoints()) : none);
            if (standing != null) {
                discordLines.add(ConfigManager.getEvents().getString("discord.line", "#%place% %player% - %points% points")
                        .replace("%place%", String.valueOf(place)).replace("%player%", name != null ? name : none)
                        .replace("%points%", String.valueOf(standing.getPoints())));
            }
        }
        broadcast(end, current);

        for (EventScores.Standing standing : ranking) {
            int points = standing.getPoints();
            int place = standing.getPlace();
            ChatGamesPlayerManager.modify(standing.getUuid(), data -> {
                data.incrementStat(stat(definition, "played"), 1);
                data.incrementStat(stat(definition, "points"), points);
                if (place == 1) data.incrementStat(stat(definition, "wins"), 1);
                if (points > data.getStat(stat(definition, "best"))) data.setStat(stat(definition, "best"), points);
                data.incrementStat(Stat.EVENTS_PLAYED, 1);
                if (place == 1) data.incrementStat(Stat.EVENT_WINS, 1);
                long best = data.getStat(Stat.EVENT_BEST_PLACE);
                if (best == 0 || place < best) data.setStat(Stat.EVENT_BEST_PLACE, place);
            });
            Player player = Bukkit.getPlayer(standing.getUuid());
            if (player == null) continue;
            Msg.of(message("result")).with("%place%", place).with("%points%", points)
                    .with("%count%", ranking.size()).with("%event%", definition.getName()).send(player);
            plugin.getGameUtils().giveLineRewards("event " + definition.getId() + " place " + place, player,
                    definition.rewardsFor(place));
            plugin.getProgressionManager().record(player, Activity.event());
        }

        if (!ranking.isEmpty() && ConfigManager.getEvents().getBoolean("discord.enabled", true)) {
            String title = ConfigManager.getEvents().getString("discord.title", "%event% results");
            DiscordListener.sendLines(plugin, title.replace("%event%", definition.getName()), discordLines);
        }
        Bukkit.getPluginManager().callEvent(new ChatGamesEventEndEvent(definition, ranking, false));
    }

    /** @return the stat holding one of a player's totals for an event, e.g. {@code event.weekend.points} */
    @Nonnull
    public static String stat(@Nonnull EventDefinition definition, @Nonnull String name) {
        return "event." + definition.getId() + "." + name;
    }

    @Nullable
    private static String nameOf(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName();
    }

    private void broadcast(Msg message, Run current) {
        message.with("%event%", current.definition.getName())
                .with("%step%", current.step + 1)
                .with("%steps%", current.definition.getSteps().size())
                .with("%pause%", current.definition.getPauseSeconds());
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            if (plugin.getGameManager().worldEnabled(player)) message.send(player);
        }
    }

    @Nullable
    private static String message(String key) {
        return ConfigManager.getEvents().getString("messages." + key);
    }

    private static List<String> messages(String key) {
        return ConfigManager.getEvents().getStringList("messages." + key);
    }

    /** One run of an event. */
    public static final class Run {
        private final EventDefinition definition;
        private final EventScores scores = new EventScores();
        /** The step playing or next to play, from 0. */
        private volatile int step;
        /** True while a step's game runs and its end is awaited. */
        private volatile boolean awaiting;
        @Nullable private volatile ChatGamesScheduler.Task pending;

        Run(EventDefinition definition) {
            this.definition = definition;
        }

        @Nonnull
        public EventDefinition getDefinition() {
            return definition;
        }

        @Nonnull
        public EventScores getScores() {
            return scores;
        }

        /** @return the game playing or next to play, from 1 */
        public int getStepNumber() {
            return step + 1;
        }
    }
}
