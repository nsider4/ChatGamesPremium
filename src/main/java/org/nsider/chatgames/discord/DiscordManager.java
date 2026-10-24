package org.nsider.chatgames.discord;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.api.PlayerResult;
import org.nsider.chatgames.api.event.ChatGamesChallengeCompleteEvent;
import org.nsider.chatgames.api.event.ChatGamesEventEndEvent;
import org.nsider.chatgames.api.event.ChatGamesEventStartEvent;
import org.nsider.chatgames.api.event.ChatGamesStreakEvent;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.gameevents.EventDefinition;
import org.nsider.chatgames.games.Game;
import org.nsider.chatgames.games.round.GameSettings;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.globalgames.CoopProgress;
import org.nsider.chatgames.globalgames.GlobalSession;
import org.nsider.chatgames.menus.Menus;
import org.nsider.chatgames.placeholders.LeaderboardSnapshot;
import org.nsider.chatgames.races.Race;
import org.nsider.chatgames.races.RaceManager;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.chatgames.utils.management.TimeUtils;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

/**
 * The Discord add-on (discord.yml): turns what happens in game into Discord messages.
 *
 * <p>The game systems call in on the main thread (the global region on Folia) as things start
 * and end; events, streaks and challenges come in as API events. Messages are rendered right
 * there, from values that can't change any more, and sent by {@link DiscordQueue}.</p>
 *
 * <p>A "live" post is made when a game starts and edited into its result when it ends; a global
 * game's post is also edited as the server's progress grows.</p>
 */
public final class DiscordManager implements Listener {

    /** Every message /chatgames discord test can send, as "section.message". */
    public static final List<String> MESSAGES = Collections.unmodifiableList(Arrays.asList(
            "games.start", "games.win", "games.expired", "games.stopped",
            "races.start", "races.win", "races.expired", "races.stopped",
            "global.progress", "global.completed", "global.failed",
            "events.start", "events.end", "streaks.message", "challenges.message", "leaderboard.message"));
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("HH:mm");
    private static final String DISCORDSRV = "DiscordSRV";

    private final ChatGames plugin;
    private final DiscordQueue queue;
    private volatile Settings settings = Settings.OFF;

    // Main/global thread only
    @Nullable private DiscordQueue.LiveMessage gamePost;
    /** The game {@link #gamePost} is about: one stopped during Memorize has no Game to ask. */
    @Nullable private GameType gamePostType;
    @Nullable private DiscordQueue.LiveMessage racePost;
    @Nullable private DiscordQueue.LiveMessage globalPost;
    @Nullable private ChatGamesScheduler.Task globalUpdates;
    @Nullable private ChatGamesScheduler.Task leaderboardTask;
    private int globalShownProgress = -1;
    @Nullable private String lastLeaderboard;

    public DiscordManager(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
        this.queue = new DiscordQueue((message, error) -> {
            if (error == null) plugin.getLogger().warning("Discord: " + message);
            else plugin.getLogger().log(Level.WARNING, "Discord: " + message, error);
        });
    }

    /** Reads discord.yml. Called on enable and reload. */
    public void load() {
        ConfigurationSection file = ConfigManager.getDiscord();
        settings = file == null ? Settings.OFF : Settings.read(file, plugin);
        ChatGamesScheduler.cancel(leaderboardTask);
        leaderboardTask = null;
        if (settings.enabled && settings.isOn("leaderboard")) {
            // Checked twice a minute, so each listed time is caught once
            leaderboardTask = plugin.getScheduler().globalTimer(this::checkLeaderboardTime, 20L * 30, 20L * 30);
        }
    }

    public void shutdown() {
        ChatGamesScheduler.cancel(leaderboardTask);
        ChatGamesScheduler.cancel(globalUpdates);
        queue.shutdown();
    }

    /** @return true if discord.yml has at least one channel that can be posted to */
    public boolean hasChannels() {
        return settings.enabled && !settings.channels.isEmpty();
    }

    /* ------------------------------------------ chat games ------------------------------------------ */

    /** A chat game has been announced. */
    public void gameStarted(@Nonnull Round round, @Nonnull GameSettings game) {
        gamePost = null;
        Settings s = settings;
        if (!s.posts("games", round.getType())) return;
        if (!s.section("games").getBoolean("live", true)) return;
        Map<String, String> values = gameValues(s, round.getType());
        putQuestion(s, values, round);
        int seconds = round.getMemorizeSeconds() + game.getTimeSeconds();
        timing(values, seconds);
        values.put("%winners_needed%", String.valueOf(game.getWinners()));
        values.put("%points%", String.valueOf(game.getPoints()));
        gamePost = post(s, "games", "start", values);
        gamePostType = round.getType();
    }

    /** A chat game ended with winners or ran out of time. */
    public void gameEnded(@Nonnull Game game) {
        DiscordQueue.LiveMessage live = gamePost;
        gamePost = null;
        Settings s = settings;
        if (!s.posts("games", game.getGameType())) return;
        List<PlayerResult> results = game.getResultsByPlace();
        Map<String, String> values = gameValues(s, game.getGameType());
        values.put("%answer%", DiscordText.escape(DiscordText.stripColors(game.getAnswer())));
        Round round = game.getRound();
        if (round != null) putQuestion(s, values, round);
        GameSettings gameSettings = game.getSettings();
        values.put("%time_limit%", String.valueOf(gameSettings != null ? gameSettings.getTimeSeconds() : 0));
        values.put("%points%", String.valueOf(gameSettings != null ? gameSettings.getPoints() : 0));
        if (results.isEmpty()) {
            if (!s.section("games.expired").getBoolean("enabled", true)) {
                // Not posted: a live post is still told the round is over
                if (live != null) finish(s, live, "games", "stopped", values);
                return;
            }
            finish(s, live, "games", "expired", values);
            return;
        }
        putWinners(s, values, results);
        DiscordMessage message = render(s, "games", "win", values);
        if (message != null) allowWinnerPing(s, message, results.get(0).getUuid());
        deliver(s, live, "games", message);
    }

    /** The running chat game was stopped by a command. */
    public void gameStopped() {
        DiscordQueue.LiveMessage live = gamePost;
        GameType type = gamePostType;
        gamePost = null;
        if (live == null || type == null) return;
        Settings s = settings;
        finish(s, live, "games", "stopped", gameValues(s, type));
    }

    /* --------------------------------------------- races --------------------------------------------- */

    public void raceStarted(@Nonnull Race race) {
        racePost = null;
        Settings s = settings;
        if (race.isGlobal() || !s.posts("races", race.getType()) || !s.section("races").getBoolean("live", true)) return;
        Map<String, String> values = raceValues(s, race);
        racePost = post(s, "races", "start", values);
    }

    public void raceWon(@Nonnull Race race, @Nonnull Player winner, double seconds, int points) {
        DiscordQueue.LiveMessage live = racePost;
        racePost = null;
        Settings s = settings;
        if (race.isGlobal() || !s.posts("races", race.getType())) return;
        Map<String, String> values = raceValues(s, race);
        values.put("%points%", String.valueOf(points));
        putWinners(s, values, Collections.singletonList(new PlayerResult(winner.getUniqueId(), seconds, 1)));
        DiscordMessage message = render(s, "races", "win", values);
        if (message != null) allowWinnerPing(s, message, winner.getUniqueId());
        deliver(s, live, "races", message);
    }

    public void raceExpired(@Nonnull Race race) {
        DiscordQueue.LiveMessage live = racePost;
        racePost = null;
        Settings s = settings;
        if (race.isGlobal() || !s.posts("races", race.getType())) return;
        String message = s.section("races.expired").getBoolean("enabled", true) ? "expired" : "stopped";
        if (message.equals("stopped") && live == null) return;
        finish(s, live, "races", message, raceValues(s, race));
    }

    public void raceStopped(@Nonnull Race race) {
        DiscordQueue.LiveMessage live = racePost;
        racePost = null;
        if (live == null) return;
        Settings s = settings;
        finish(s, live, "races", "stopped", raceValues(s, race));
    }

    /* ------------------------------------------ global games ------------------------------------------ */

    public void globalStarted(@Nonnull GlobalSession session) {
        stopGlobalUpdates();
        Settings s = settings;
        if (!s.posts("global", session.getType())) return;
        Map<String, String> values = globalValues(s, session);
        globalPost = post(s, "global", "progress", values);
        globalShownProgress = session.getProgress().getProgress();
        if (globalPost != null && s.section("global").getBoolean("live", true)) {
            long period = 20L * Math.max(5, s.section("global").getInt("update-seconds", 15));
            globalUpdates = plugin.getScheduler().globalTimer(() -> updateGlobal(session), period, period);
        }
    }

    private void updateGlobal(GlobalSession session) {
        DiscordQueue.LiveMessage live = globalPost;
        if (live == null || plugin.getGlobalGameManager().getCurrent() != session) {
            stopGlobalUpdates();
            return;
        }
        int progress = session.getProgress().getProgress();
        if (progress == globalShownProgress) return;
        globalShownProgress = progress;
        Settings s = settings;
        DiscordMessage message = render(s, "global", "progress", globalValues(s, session));
        if (message != null) queue.edit(live, message);
    }

    /** A global game ended; {@code ranking} is every contributor, biggest first. */
    public void globalEnded(@Nonnull GlobalSession session, @Nonnull CoopProgress.Outcome outcome,
                            @Nonnull List<CoopProgress.Contributor> ranking) {
        DiscordQueue.LiveMessage live = globalPost;
        stopGlobalUpdates();
        Settings s = settings;
        if (!s.posts("global", session.getType())) return;
        Map<String, String> values = globalValues(s, session);
        int top = Math.max(1, s.section("global").getInt("top-size", 5));
        List<String> lines = new ArrayList<>();
        for (CoopProgress.Contributor contributor : ranking) {
            if (lines.size() >= top) break;
            lines.add(s.line("contributor", contributor.getPlace(), name(contributor.getUuid()), "%amount%", String.valueOf(contributor.getAmount())));
        }
        values.put("%top_contributors%", lines.isEmpty() ? s.listEmpty : String.join("\n", lines));
        if (outcome == CoopProgress.Outcome.STOPPED) {
            // Nothing to celebrate; a live post just stops showing a running game
            if (live != null) finish(s, live, "global", "failed", values);
            return;
        }
        finish(s, live, "global", outcome == CoopProgress.Outcome.COMPLETED ? "completed" : "failed", values);
    }

    private void stopGlobalUpdates() {
        ChatGamesScheduler.cancel(globalUpdates);
        globalUpdates = null;
        globalPost = null;
        globalShownProgress = -1;
    }

    /* ------------------------------------- events, streaks, challenges ------------------------------------- */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEventStart(ChatGamesEventStartEvent event) {
        Settings s = settings;
        if (!s.isOn("events")) return;
        Map<String, String> values = s.base();
        values.put("%event%", DiscordText.escape(DiscordText.stripColors(event.getEventName())));
        EventDefinition definition = plugin.getEventManager().getConfig().get(event.getEventId());
        values.put("%games%", String.valueOf(definition != null ? definition.getSteps().size() : 0));
        post(s, "events", "start", values);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEventEnd(ChatGamesEventEndEvent event) {
        Settings s = settings;
        if (!s.isOn("events") || event.wasStopped()) return;
        Map<String, String> values = s.base();
        values.put("%event%", DiscordText.escape(DiscordText.stripColors(event.getEventName())));
        int size = Math.max(1, s.section("events").getInt("standings-size", 10));
        List<String> lines = new ArrayList<>();
        for (ChatGamesEventEndEvent.Standing standing : event.getStandings()) {
            if (lines.size() >= size) break;
            lines.add(s.line("standing", standing.getPlace(), name(standing.getUuid()), "%points%", String.valueOf(standing.getPoints())));
        }
        values.put("%standings%", lines.isEmpty() ? s.listEmpty : String.join("\n", lines));
        values.put("%players%", String.valueOf(event.getStandings().size()));
        UUID first = event.getStandings().isEmpty() ? null : event.getStandings().get(0).getUuid();
        putPlayer(s, values, "winner", first);
        DiscordMessage message = render(s, "events", "end", values);
        if (message != null && first != null) allowWinnerPing(s, message, first);
        if (message != null) queue.send(s.targets("events"), message, null);
    }

    // Streaks and challenges can fire off the main thread; PlaceholderAPI is only used on it
    @EventHandler(priority = EventPriority.MONITOR)
    public void onStreak(ChatGamesStreakEvent event) {
        Settings s = settings;
        if (!s.isOn("streaks") || !s.section("streaks").getIntegerList("days").contains(event.getDays())) return;
        UUID player = event.getPlayer().getUniqueId();
        String streak = event.getStreak();
        int days = event.getDays();
        plugin.getScheduler().onGlobal(() -> {
            Map<String, String> values = s.base();
            putPlayer(s, values, "player", player);
            String name = ConfigManager.getStreaks() == null ? null : ConfigManager.getStreaks().getString(streak + ".name");
            values.put("%streak%", DiscordText.escape(DiscordText.stripColors(name != null ? name : streak)));
            values.put("%days%", String.valueOf(days));
            post(s, "streaks", "message", values);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChallenge(ChatGamesChallengeCompleteEvent event) {
        Settings s = settings;
        if (!s.isOn("challenges")) return;
        UUID player = event.getPlayer().getUniqueId();
        String challenge = event.getChallengeName();
        String period = event.getPeriod().toLowerCase(Locale.ROOT);
        plugin.getScheduler().onGlobal(() -> {
            Map<String, String> values = s.base();
            putPlayer(s, values, "player", player);
            values.put("%challenge%", DiscordText.escape(DiscordText.stripColors(challenge)));
            values.put("%period%", s.section("challenges").getString("periods." + period, period));
            post(s, "challenges", "message", values);
        });
    }

    /* ------------------------------------------- leaderboard ------------------------------------------- */

    private void checkLeaderboardTime() {
        Settings s = settings;
        if (!s.isOn("leaderboard")) return;
        String now = LocalTime.now().format(MINUTE);
        String key = LocalDate.now() + " " + now;
        if (key.equals(lastLeaderboard) || !s.section("leaderboard").getStringList("times").contains(now)) return;
        lastLeaderboard = key;
        postLeaderboard();
    }

    /** Posts the leaderboard now (also for /chatgames discord leaderboard). @return false if it is off or has no channel */
    public boolean postLeaderboard() {
        Settings s = settings;
        if (!s.enabled || s.targets("leaderboard").isEmpty()) return false;
        Map<String, String> values = leaderboardValues(s);
        return post(s, "leaderboard", "message", values) != null;
    }

    private Map<String, String> leaderboardValues(Settings s) {
        Map<String, String> values = s.base();
        ConfigurationSection section = s.section("leaderboard");
        boolean fastest = "fastest".equalsIgnoreCase(section.getString("type", "points"));
        int size = Math.max(1, Math.min(25, section.getInt("size", 10)));
        LeaderboardSnapshot snapshot = plugin.getLeaderboardCache().getSnapshot();
        List<String> lines = new ArrayList<>();
        for (int position = 1; position <= size; position++) {
            LeaderboardSnapshot.Entry entry = fastest ? snapshot.fastest(position) : snapshot.points(position);
            if (entry == null) break;
            String value = fastest ? seconds(entry.getValue()) + "s" : String.valueOf((long) entry.getValue());
            lines.add(s.line("leaderboard", entry.getRank(), DiscordText.escape(entry.getName()), "%value%", value));
        }
        values.put("%leaderboard%", lines.isEmpty() ? s.listEmpty : String.join("\n", lines));
        values.put("%total_points%", String.valueOf(snapshot.getTotalPoints()));
        return values;
    }

    /* ---------------------------------------------- test ---------------------------------------------- */

    /**
     * Sends one message with made-up values, to see how it looks.
     *
     * @param name one of {@link #MESSAGES}
     * @return SENT if it was queued (a failed delivery is reported in the console), otherwise why not
     */
    @Nonnull
    public TestResult test(@Nonnull String name, @Nullable Player viewer) {
        Settings s = settings;
        if (!s.enabled) return TestResult.DISABLED;
        int dot = name.indexOf('.');
        if (dot < 0 || !MESSAGES.contains(name)) return TestResult.UNKNOWN;
        String section = name.substring(0, dot);
        String message = name.substring(dot + 1);
        if (s.targets(section).isEmpty()) return TestResult.NO_CHANNEL;
        DiscordTemplate template = DiscordTemplate.read(s.section(section + "." + message));
        if (template == null) return TestResult.EMPTY;

        UUID uuid = viewer != null ? viewer.getUniqueId() : UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");
        String player = viewer != null ? viewer.getName() : "Steve";
        GameType game = section.equals("races") ? GameType.MINE : section.equals("global") ? GameType.HUNT : GameType.UNSCRAMBLE;
        Map<String, String> values = gameValues(s, game);
        values.put("%question%", "tsoen");
        values.put("%answer%", "stone");
        values.put("%points%", "1");
        values.put("%winners_needed%", "1");
        values.put("%target%", "Stone");
        values.put("%amount%", "16");
        timing(values, 60);
        putWinners(s, values, Arrays.asList(new PlayerResult(uuid, 2.41, 1)));
        values.put("%winner%", DiscordText.escape(player));
        values.put("%winner_head%", s.head(player, uuid));
        values.put("%winner_mention%", DiscordText.escape(player));
        values.put("%winners%", s.line("winner", 1, DiscordText.escape(player), "%time%", "2.41") + "\n" + s.line("winner", 2, "Alex", "%time%", "3.05"));
        values.put("%other_winners%", s.line("winner", 2, "Alex", "%time%", "3.05"));
        values.put("%winner_count%", "2");
        values.put("%goal%", "40 Zombie");
        values.put("%progress%", "26");
        values.put("%goal_amount%", "40");
        values.put("%percent%", "65");
        values.put("%bar%", s.bar(26, 40));
        values.put("%contributors%", "7");
        values.put("%top_contributors%", s.line("contributor", 1, DiscordText.escape(player), "%amount%", "11") + "\n"
                + s.line("contributor", 2, "Alex", "%amount%", "8") + "\n" + s.line("contributor", 3, "Notch", "%amount%", "4"));
        values.put("%event%", "Weekend Cup");
        values.put("%games%", "5");
        values.put("%standings%", s.line("standing", 1, DiscordText.escape(player), "%points%", "14") + "\n"
                + s.line("standing", 2, "Alex", "%points%", "9") + "\n" + s.line("standing", 3, "Notch", "%points%", "5"));
        values.put("%players%", "12");
        values.put("%player%", DiscordText.escape(player));
        values.put("%player_head%", s.head(player, uuid));
        values.put("%player_mention%", DiscordText.escape(player));
        values.put("%streak%", "Daily win streak");
        values.put("%days%", "7");
        values.put("%challenge%", "Win 5 games");
        values.put("%period%", s.section("challenges").getString("periods.daily", "daily"));
        if (section.equals("leaderboard")) values.putAll(leaderboardValues(s));
        DiscordMessage rendered = template.render(values, papi());
        if (rendered.isEmpty()) return TestResult.EMPTY;
        rendered.allowRoles(s.roles);
        s.identity(rendered, s.targetChannel(section));
        queue.send(s.targets(section), rendered, null);
        return TestResult.SENT;
    }

    /** What {@link #test} did. */
    public enum TestResult { SENT, DISABLED, UNKNOWN, NO_CHANNEL, EMPTY }

    /* --------------------------------------------- values --------------------------------------------- */

    private Map<String, String> gameValues(Settings s, GameType type) {
        Map<String, String> values = s.base();
        values.put("%game%", DiscordText.escape(DiscordText.stripColors(Menus.gameName(type))));
        values.put("%game_key%", type.getConfigKey());
        Style style = s.style(type);
        values.put("%game_emoji%", style.emoji);
        values.put("%game_color%", style.color);
        return values;
    }

    private Map<String, String> raceValues(Settings s, Race race) {
        Map<String, String> values = gameValues(s, race.getType());
        values.put("%target%", DiscordText.escape(DiscordText.stripColors(race.getDisplayName())));
        values.put("%amount%", String.valueOf(race.getAmount()));
        values.put("%points%", String.valueOf(Math.max(0, RaceManager.section(race.getType()).getInt("points", 1))));
        int left = Math.max(0, (int) Math.round(race.getTimeLimitSeconds() - race.getElapsedSeconds()));
        values.put("%time_limit%", String.valueOf(race.getTimeLimitSeconds()));
        values.put("%ends_unix%", String.valueOf(System.currentTimeMillis() / 1000 + left));
        return values;
    }

    private Map<String, String> globalValues(Settings s, GlobalSession session) {
        Map<String, String> values = gameValues(s, session.getType());
        CoopProgress progress = session.getProgress();
        values.put("%goal%", DiscordText.escape(DiscordText.stripColors(session.getGoalText())));
        values.put("%progress%", String.valueOf(progress.getProgress()));
        values.put("%goal_amount%", String.valueOf(progress.getGoal()));
        values.put("%percent%", String.valueOf(progress.getPercent()));
        values.put("%bar%", s.bar(progress.getProgress(), progress.getGoal()));
        values.put("%contributors%", String.valueOf(progress.contributors()));
        int left = Math.max(0, session.getSecondsLeft());
        values.put("%time_limit%", String.valueOf(left));
        values.put("%ends_unix%", String.valueOf(System.currentTimeMillis() / 1000 + left));
        return values;
    }

    private void putQuestion(Settings s, Map<String, String> values, Round round) {
        boolean hidden = round.getMemorizeSeconds() > 0 || round.getType() == GameType.MEMORIZE;
        String question = DiscordText.escape(DiscordText.stripColors(round.getDisplay()).trim());
        values.put("%question%", hidden || question.isEmpty() ? s.section("games").getString("hidden-question", "") : question);
    }

    private static void timing(Map<String, String> values, int seconds) {
        values.put("%time_limit%", String.valueOf(seconds));
        values.put("%ends_unix%", String.valueOf(System.currentTimeMillis() / 1000 + seconds));
    }

    private void putWinners(Settings s, Map<String, String> values, List<PlayerResult> results) {
        PlayerResult first = results.get(0);
        putPlayer(s, values, "winner", first.getUuid());
        values.put("%time%", seconds(first.getTime()));
        List<String> all = new ArrayList<>();
        for (PlayerResult result : results) {
            all.add(s.line("winner", result.getPlace(), name(result.getUuid()), "%time%", seconds(result.getTime())));
        }
        values.put("%winners%", String.join("\n", all));
        values.put("%other_winners%", String.join("\n", all.subList(1, all.size())));
        values.put("%winner_count%", String.valueOf(results.size()));
    }

    /** %<key>% %<key>_head% %<key>_mention% for a player (empty if there is none). */
    private void putPlayer(Settings s, Map<String, String> values, String key, @Nullable UUID uuid) {
        if (uuid == null) {
            values.put("%" + key + "%", "");
            values.put("%" + key + "_head%", "");
            values.put("%" + key + "_mention%", "");
            return;
        }
        String raw = rawName(uuid);
        String name = DiscordText.escape(raw);
        values.put("%" + key + "%", name);
        values.put("%" + key + "_head%", s.head(raw, uuid));
        String linked = s.discordSrv ? DiscordSrvHook.linkedId(uuid) : null;
        values.put("%" + key + "_mention%", linked != null ? "<@" + linked + ">" : name);
    }

    private void allowWinnerPing(Settings s, DiscordMessage message, UUID winner) {
        if (s.pingLinked && s.discordSrv) message.allowUser(DiscordSrvHook.linkedId(winner));
    }

    private static String seconds(double value) {
        return String.valueOf(TimeUtils.roundTo2Decimals(value));
    }

    private static String name(UUID uuid) {
        return DiscordText.escape(rawName(uuid));
    }

    private static String rawName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : uuid.toString().substring(0, 8);
    }

    /* --------------------------------------------- sending --------------------------------------------- */

    /** Renders and posts; @return the live post, or null if nothing was posted */
    @Nullable
    private DiscordQueue.LiveMessage post(Settings s, String section, String name, Map<String, String> values) {
        if (!s.isOn(section)) return null;
        DiscordMessage message = render(s, section, name, values);
        List<DiscordTarget> targets = s.targets(section);
        if (message == null || targets.isEmpty()) return null;
        DiscordQueue.LiveMessage live = new DiscordQueue.LiveMessage(targets);
        queue.send(targets, message, live);
        return live;
    }

    /** Edits the live post into this message, or posts it if there is no live post. */
    private void finish(Settings s, @Nullable DiscordQueue.LiveMessage live, String section, String name, Map<String, String> values) {
        deliver(s, live, section, render(s, section, name, values));
    }

    private void deliver(Settings s, @Nullable DiscordQueue.LiveMessage live, String section, @Nullable DiscordMessage message) {
        if (message == null) return;
        if (live != null) queue.edit(live, message);
        else if (s.isOn(section)) queue.send(s.targets(section), message, null);
    }

    @Nullable
    private DiscordMessage render(Settings s, String section, String name, Map<String, String> values) {
        DiscordTemplate template = s.template(section + "." + name);
        if (template == null) return null;
        DiscordMessage message = template.render(values, papi());
        if (message.isEmpty()) return null;
        message.allowRoles(s.roles);
        s.identity(message, s.targetChannel(section));
        return message;
    }

    @Nullable
    private UnaryOperator<String> papi() {
        if (!plugin.isPlaceHolderAPIEnabled()) return null;
        return text -> {
            try {
                return PlaceholderAPI.setPlaceholders((OfflinePlayer) null, text);
            } catch (RuntimeException | LinkageError e) {
                return text;
            }
        };
    }

    /* --------------------------------------------- settings --------------------------------------------- */

    /** A game's colour and emoji. */
    static final class Style {
        final String color;
        final String emoji;

        Style(String color, String emoji) {
            this.color = color;
            this.emoji = emoji;
        }
    }

    /** One channel from discord.yml: where it posts and as whom. */
    static final class Channel {
        final List<DiscordTarget> targets;
        @Nullable final String username;
        @Nullable final String avatar;

        Channel(List<DiscordTarget> targets, @Nullable String username, @Nullable String avatar) {
            this.targets = targets;
            this.username = username;
            this.avatar = avatar;
        }
    }

    /** discord.yml, read once per (re)load. */
    static final class Settings {
        static final Settings OFF = new Settings(new MemoryConfiguration(), false, Collections.<String, Channel>emptyMap(), false);

        final ConfigurationSection file;
        final boolean enabled;
        final Map<String, Channel> channels;
        final boolean discordSrv;
        final String serverName;
        final String headUrl;
        final List<String> roles;
        final boolean pingLinked;
        final Set<String> ignored = new HashSet<>();
        final Map<String, Style> styles = new HashMap<>();
        final Map<String, DiscordTemplate> templates = new HashMap<>();
        final List<String> medals;
        final String otherPlaces;
        final String listEmpty;
        final int barLength;
        final String barFull;
        final String barEmpty;

        private Settings(ConfigurationSection file, boolean enabled, Map<String, Channel> channels, boolean discordSrv) {
            this.file = file;
            this.enabled = enabled;
            this.channels = channels;
            this.discordSrv = discordSrv;
            this.serverName = DiscordText.escape(file.getString("server-name", ""));
            this.headUrl = file.getString("player-head-url", "");
            this.roles = file.getStringList("allowed-role-pings");
            this.pingLinked = file.getBoolean("ping-linked-players", false);
            for (String game : file.getStringList("ignored-games")) ignored.add(game.toLowerCase(Locale.ROOT));
            ConfigurationSection styleSection = file.getConfigurationSection("styles");
            if (styleSection != null) {
                for (String key : styleSection.getKeys(false)) {
                    styles.put(key.toLowerCase(Locale.ROOT), new Style(styleSection.getString(key + ".color", ""),
                            DiscordText.emoji(styleSection.getString(key + ".emoji", ""))));
                }
            }
            for (String name : MESSAGES) {
                DiscordTemplate template = DiscordTemplate.read(file.getConfigurationSection(name));
                if (template != null) templates.put(name, template);
            }
            List<String> medals = new ArrayList<>();
            for (String medal : file.getStringList("lists.medals")) medals.add(DiscordText.emoji(medal));
            this.medals = medals;
            this.otherPlaces = file.getString("lists.other-places", "#%place%");
            this.listEmpty = file.getString("lists.empty", "-");
            this.barLength = file.getInt("global.bar.length", 16);
            this.barFull = file.getString("global.bar.full", "█");
            this.barEmpty = file.getString("global.bar.empty", "░");
        }

        static Settings read(ConfigurationSection file, ChatGames plugin) {
            boolean enabled = file.getBoolean("enabled", true);
            boolean discordSrv = Bukkit.getPluginManager().isPluginEnabled(DISCORDSRV);
            Map<String, Channel> channels = new LinkedHashMap<>();
            ConfigurationSection section = file.getConfigurationSection("channels");
            if (enabled && section != null) {
                for (String name : section.getKeys(false)) {
                    List<DiscordTarget> targets = new ArrayList<>();
                    String webhook = section.getString(name + ".webhook", "").trim();
                    if (!webhook.isEmpty()) {
                        WebhookTarget target = WebhookTarget.of(name, webhook);
                        if (target != null) targets.add(target);
                        else plugin.getLogger().warning("discord.yml: channels." + name + ".webhook isn't a Discord webhook URL"
                                + " (it should look like https://discord.com/api/webhooks/<id>/<token>).");
                    }
                    String srv = section.getString(name + ".discordsrv", "").trim();
                    if (!srv.isEmpty()) {
                        if (discordSrv) targets.add(DiscordSrvHook.target(name, srv));
                        else plugin.getLogger().warning("discord.yml: channels." + name + ".discordsrv is set, but DiscordSRV isn't installed.");
                    }
                    if (!targets.isEmpty()) {
                        channels.put(name, new Channel(targets, section.getString(name + ".username"), section.getString(name + ".avatar")));
                    }
                }
            }
            return new Settings(file, enabled, channels, discordSrv);
        }

        ConfigurationSection section(String path) {
            ConfigurationSection section = file.getConfigurationSection(path);
            return section != null ? section : new MemoryConfiguration();
        }

        /** @return true if this notification is on and has somewhere to go */
        boolean isOn(String notification) {
            return enabled && section(notification).getBoolean("enabled", true) && !targets(notification).isEmpty();
        }

        boolean posts(String notification, GameType type) {
            return isOn(notification) && !ignored.contains(type.getConfigKey());
        }

        @Nullable
        DiscordTemplate template(String name) {
            return templates.get(name);
        }

        List<DiscordTarget> targets(String notification) {
            List<DiscordTarget> targets = new ArrayList<>();
            for (String name : channelNames(notification)) {
                Channel channel = channels.get(name);
                if (channel != null) targets.addAll(channel.targets);
            }
            return targets;
        }

        /** The first channel a notification uses, for the webhook name and picture. */
        @Nullable
        Channel targetChannel(String notification) {
            for (String name : channelNames(notification)) {
                Channel channel = channels.get(name);
                if (channel != null) return channel;
            }
            return null;
        }

        private List<String> channelNames(String notification) {
            ConfigurationSection section = section(notification);
            if (section.isList("channels")) return section.getStringList("channels");
            String one = section.getString("channels");
            return one != null ? Collections.singletonList(one) : Collections.singletonList("main");
        }

        /** The channel's webhook name and picture, unless the message sets its own. */
        void identity(DiscordMessage message, @Nullable Channel channel) {
            if (channel == null) return;
            if (message.getUsername() == null) message.username(channel.username);
            if (message.getAvatarUrl() == null) message.avatarUrl(channel.avatar);
        }

        Map<String, String> base() {
            Map<String, String> values = new HashMap<>();
            values.put("%server%", serverName);
            return values;
        }

        Style style(GameType type) {
            Style fallback = styles.get("default");
            Style style = styles.get(type.getConfigKey());
            String color = style != null && !style.color.isEmpty() ? style.color : fallback != null ? fallback.color : "";
            String emoji = style != null && !style.emoji.isEmpty() ? style.emoji : fallback != null ? fallback.emoji : "";
            return new Style(color, emoji);
        }

        String head(String name, UUID uuid) {
            return headUrl.replace("%name%", name).replace("%uuid%", uuid.toString());
        }

        String bar(int progress, int goal) {
            return DiscordText.bar(progress, goal, barLength, barFull, barEmpty);
        }

        /** A list line: lists.<format> with %medal% %place% %player% and one more value. */
        String line(String format, int place, String player, String key, String value) {
            String medal = place >= 1 && place <= medals.size() ? medals.get(place - 1) : otherPlaces.replace("%place%", String.valueOf(place));
            return DiscordText.emoji(file.getString("lists." + format, "%medal% %player%")
                    .replace("%medal%", medal).replace("%place%", String.valueOf(place))
                    .replace("%player%", player).replace(key, value));
        }
    }
}
