package org.nsider.chatgames.menus;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.games.round.AnswerChecker;
import org.nsider.chatgames.games.round.GameSettings;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;
import org.nsider.inventorymanager.menu.Menu;
import org.nsider.inventorymanager.menu.MenuManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code /chatgames debug}: generates a round of any chat game for one admin only and shows
 * its question and answers. The {@code debug-session} buttons feed mock answers through the
 * real answer check (capitalisation, anti-macro minimum, time limit) and report the result
 * and timing in chat. Nothing reaches the running game, rewards or stats.
 *
 * <p>Actions: {@code [chatgames:debug] <game>}, {@code [chatgames:debug-answer]
 * <correct|wrong|wrong-case> [ms]}, {@code [chatgames:debug-timeout]},
 * {@code [chatgames:debug-reroll]} and {@code [chatgames:debug-end]}.</p>
 *
 * <p>A mock answer's time counts from the click, as a real answer's counts from the round
 * opening. Delays are rounded to server ticks (50 ms); the report shows the real time taken.</p>
 */
public final class DebugMenu implements Listener {

    private final ChatGames plugin;
    private final Menus menus;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    DebugMenu(ChatGames plugin, Menus menus) {
        this.plugin = plugin;
        this.menus = menus;
    }

    void register(@Nonnull MenuManager manager) {
        manager.registerMenu("debug", this::buildPicker);
        manager.registerMenu("debug-session", this::buildSession);
        manager.registerAction("chatgames:debug", (menu, argument) -> {
            GameType type = GameType.fromName(argument.trim());
            if (type != null && start(menu.getPlayer(), type)) menu.open("debug-session");
        });
        manager.registerAction("chatgames:debug-answer", this::answer);
        manager.registerAction("chatgames:debug-timeout", (menu, argument) -> timeout(menu.getPlayer()));
        manager.registerAction("chatgames:debug-reroll", (menu, argument) -> {
            Session session = sessions.get(menu.getPlayer().getUniqueId());
            if (session != null && start(menu.getPlayer(), session.round.getType())) menu.refresh();
        });
        manager.registerAction("chatgames:debug-end", (menu, argument) -> end(menu.getPlayer().getUniqueId()));
    }

    /**
     * Generates a fresh private round for the player (opening {@code debug-session} is up to
     * the caller).
     *
     * @param args optional values, as after {@code /chatgames <game>}
     * @return false if it couldn't (a race, or the generator failed); the player is told why
     */
    public boolean start(@Nonnull Player player, @Nonnull GameType type, @Nonnull String... args) {
        if (type.isRace() || type.isGlobalOnly()) {
            Menus.message(player, "debug-race");
            return false;
        }
        end(player.getUniqueId());
        long before = System.nanoTime();
        Round round = plugin.getGameBuilder().generate(type, args);
        double generateMillis = (System.nanoTime() - before) / 1_000_000.0;
        if (round == null) {
            Menus.message(player, "debug-failed", "%game%", Menus.gameName(type));
            return false;
        }
        sessions.put(player.getUniqueId(), new Session(round, plugin.getGameConfigManager().settings(type), generateMillis));
        Menus.message(player, "debug-round", "%game%", Menus.gameName(type),
                "%answer%", round.getMainAnswer(), "%generated%", String.format(Locale.ROOT, "%.2f", generateMillis));
        return true;
    }

    /** Cancels a player's pending mock answers, e.g. when they leave. */
    public void end(@Nonnull UUID player) {
        Session session = sessions.remove(player);
        if (session != null) session.cancelTasks();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        end(event.getPlayer().getUniqueId());
    }

    /** Every chat game (races are played in the world). */
    private void buildPicker(Menu menu) {
        for (GameType type : GameType.values()) {
            if (!type.isRace() && !type.isGlobalOnly()) menu.addEntry("game", Menus.gameValues(type));
        }
    }

    private void buildSession(Menu menu) {
        Session session = sessions.get(menu.getPlayer().getUniqueId());
        if (session == null) {
            menu.cancel();
            menus.open(menu.getPlayer(), "debug");
            return;
        }
        Round round = session.round;
        GameSettings settings = session.settings;
        menu.set("%game%", Menus.gameName(round.getType()))
                .set("%key%", round.getType().getConfigKey())
                .set("%icon%", Menus.gameIcon(round.getType()))
                .set("%question%", round.getDisplay())
                .set("%answer%", round.getMainAnswer())
                .set("%time%", settings.getTimeSeconds())
                .set("%min_ms%", settings.getMinAnswerMillis())
                .set("%case_sensitive%", CommandSupport.state(settings.isCaseSensitive()))
                .set("%winners%", settings.getWinners())
                .set("%points%", settings.getPoints())
                .set("%hints%", round.getHints().size())
                .set("%generated%", String.format(Locale.ROOT, "%.2f", session.generateMillis));
        List<String> placeholders = new ArrayList<>();
        for (Map.Entry<String, String> entry : round.getPlaceholders().entrySet()) {
            placeholders.add("&7" + entry.getKey() + " &8= &f" + entry.getValue());
        }
        menu.list("%placeholders%", placeholders);
        List<String> answers = new ArrayList<>();
        for (String answer : round.getAnswers()) answers.add("&f" + answer);
        menu.list("%answers%", answers);

        // The "answer correctly after X ms" buttons: one entry per configured delay
        for (int delay : menu.getSection().getIntegerList("delays")) {
            Map<String, Object> values = new HashMap<>();
            values.put("%ms%", delay);
            menu.addEntry("delays", "correct-after", values, null);
        }
    }

    /** {@code [chatgames:debug-answer] <correct|wrong|wrong-case> [ms]} */
    private void answer(Menu menu, String argument) {
        Player player = menu.getPlayer();
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        String[] parts = argument.trim().split("\\s+");
        String main = session.round.getMainAnswer();
        int delay = 0;
        if (parts.length > 1) {
            try {
                delay = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
                // No delay
            }
        }
        switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "wrong":
                mock(player, session, "zz" + main + "zz", delay);
                break;
            case "wrong-case":
                String swapped = swapCase(main);
                if (swapped.equals(main)) {
                    Menus.message(player, "debug-no-letters");
                } else {
                    mock(player, session, swapped, delay);
                }
                break;
            default:
                mock(player, session, main, delay);
        }
    }

    private void timeout(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        Round round = session.round;
        int seconds = session.settings.getTimeSeconds();
        Menus.message(player, "debug-timeout-started", "%time%", String.valueOf(seconds));
        session.add(plugin.getScheduler().asyncLater(() -> {
            if (sessions.get(player.getUniqueId()) == session) {
                Menus.message(player, "debug-timeout", "%game%", Menus.gameName(round.getType()), "%answer%", round.getMainAnswer());
            }
        }, seconds * 20L));
    }

    private void mock(Player player, Session session, String input, int delayMillis) {
        long clicked = System.nanoTime();
        if (delayMillis <= 0) {
            report(player, session, input, clicked);
            return;
        }
        long ticks = Math.max(1L, Math.round(delayMillis / 50.0));
        session.add(plugin.getScheduler().asyncLater(() -> {
            if (sessions.get(player.getUniqueId()) == session) report(player, session, input, clicked);
        }, ticks));
    }

    private void report(Player player, Session session, String input, long since) {
        long millis = (System.nanoTime() - since) / 1_000_000L;
        GameSettings settings = session.settings;
        AnswerChecker.Result result = AnswerChecker.check(session.round, input, settings.isCaseSensitive());
        String verdict;
        if (result == AnswerChecker.Result.WRONG) {
            verdict = "wrong";
        } else if (result == AnswerChecker.Result.WRONG_CASE) {
            verdict = "wrong-case";
        } else if (millis < settings.getMinAnswerMillis()) {
            verdict = "macro";
        } else if (millis > settings.getTimeSeconds() * 1000L) {
            verdict = "late";
        } else {
            verdict = "accepted";
        }
        String text = ConfigManager.getMenus().getString("messages.debug-verdicts." + verdict, verdict)
                .replace("%min_ms%", String.valueOf(settings.getMinAnswerMillis()))
                .replace("%points%", String.valueOf(settings.getPoints()));
        Menus.message(player, "debug-result", "%game%", Menus.gameName(session.round.getType()), "%input%", input,
                "%ms%", String.valueOf(millis), "%verdict%", text);
    }

    @Nonnull
    static String swapCase(@Nonnull String text) {
        StringBuilder swapped = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            swapped.append(Character.isUpperCase(c) ? Character.toLowerCase(c)
                    : Character.isLowerCase(c) ? Character.toUpperCase(c) : c);
        }
        return swapped.toString();
    }

    /** One player's private round and its pending mock answers. */
    private static final class Session {
        final Round round;
        final GameSettings settings;
        final double generateMillis;
        private final List<ChatGamesScheduler.Task> tasks = new ArrayList<>();

        Session(Round round, GameSettings settings, double generateMillis) {
            this.round = round;
            this.settings = settings;
            this.generateMillis = generateMillis;
        }

        synchronized void add(@Nullable ChatGamesScheduler.Task task) {
            if (task != null) tasks.add(task);
        }

        synchronized void cancelTasks() {
            for (ChatGamesScheduler.Task task : tasks) ChatGamesScheduler.cancel(task);
            tasks.clear();
        }
    }
}
