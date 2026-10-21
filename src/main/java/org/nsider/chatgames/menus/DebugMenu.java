package org.nsider.chatgames.menus;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.round.AnswerChecker;
import org.nsider.chatgames.games.round.GameSettings;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code /chatgames debug}: generates a round of any chat game for one admin only and
 * shows its question and answers. The buttons feed mock answers through the real answer
 * check (capitalisation, anti-macro minimum, time limit) and report the result and timing
 * in chat. Nothing reaches the running game, rewards or stats.
 *
 * <p>A mock answer's time counts from the click, as a real answer's counts from the
 * round opening. Delays are rounded to server ticks (50 ms); the report shows the real
 * time taken.</p>
 */
public final class DebugMenu {

    private final ChatGames plugin;
    private final Menus menus;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    DebugMenu(ChatGames plugin, Menus menus) {
        this.plugin = plugin;
        this.menus = menus;
    }

    /** The game picker. */
    public void open(@Nonnull Player player) {
        open(player, 0);
    }

    private void open(Player player, int page) {
        if (!player.hasPermission(PermissionKey.DEBUG.getPermission())) {
            CommandSupport.send(player, MessageKey.NOT_ENOUGH_PERMISSIONS);
            return;
        }
        MenuBuilder menu = MenuBuilder.of(plugin, "debug", player);
        List<GameType> games = new ArrayList<>();
        for (GameType type : GameType.values()) {
            if (!type.isRace()) games.add(type);
        }
        menu.paged(games, page, (slot, game) ->
                menu.place(slot, menu.template("game"), AdminMenu.gameValues(game), () -> start(player, game)),
                other -> open(player, other));
        if (player.hasPermission(PermissionKey.ADMIN.getPermission())) menu.item("back", () -> menus.getAdmin().open(player));
        menu.item("close", player::closeInventory);
        menu.open();
    }

    /**
     * Generates a fresh private round and opens its test menu.
     *
     * @param args optional values, as after {@code /chatgames <game>}
     */
    public void start(@Nonnull Player player, @Nonnull GameType type, @Nonnull String... args) {
        if (type.isRace()) {
            Menus.message(player, "debug-race");
            return;
        }
        end(player.getUniqueId());
        long before = System.nanoTime();
        Round round = plugin.getGameBuilder().generate(type, args);
        double generateMillis = (System.nanoTime() - before) / 1_000_000.0;
        if (round == null) {
            Menus.message(player, "debug-failed", "%game%", Menus.gameName(type));
            return;
        }
        Session session = new Session(round, plugin.getGameConfigManager().settings(type), generateMillis);
        sessions.put(player.getUniqueId(), session);
        Menus.message(player, "debug-round", "%game%", Menus.gameName(type),
                "%answer%", round.getMainAnswer(), "%generated%", String.format(Locale.ROOT, "%.2f", generateMillis));
        openSession(player);
    }

    /** Cancels a player's pending mock answers, e.g. when they leave. */
    public void end(@Nonnull UUID player) {
        Session session = sessions.remove(player);
        if (session != null) session.cancelTasks();
    }

    private void openSession(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            open(player);
            return;
        }
        Round round = session.round;
        GameSettings settings = session.settings;
        MenuBuilder menu = MenuBuilder.of(plugin, "debug-session", player)
                .with("%game%", Menus.gameName(round.getType()))
                .with("%icon%", Menus.gameIcon(round.getType()))
                .with("%question%", round.getDisplay())
                .with("%answer%", round.getMainAnswer())
                .with("%time%", settings.getTimeSeconds())
                .with("%min_ms%", settings.getMinAnswerMillis())
                .with("%case_sensitive%", CommandSupport.state(settings.isCaseSensitive()))
                .with("%winners%", settings.getWinners())
                .with("%points%", settings.getPoints())
                .with("%hints%", round.getHints().size())
                .with("%generated%", String.format(Locale.ROOT, "%.2f", session.generateMillis));
        List<String> placeholders = new ArrayList<>();
        for (Map.Entry<String, String> entry : round.getPlaceholders().entrySet()) {
            placeholders.add("&7" + entry.getKey() + " &8= &f" + entry.getValue());
        }
        menu.withList("%placeholders%", placeholders);
        List<String> answers = new ArrayList<>();
        for (String answer : round.getAnswers()) answers.add("&f" + answer);
        menu.withList("%answers%", answers);

        menu.item("question", null);
        menu.item("answer", null);
        menu.item("settings", null);
        menu.item("correct-now", () -> mock(player, session, round.getMainAnswer(), 0));
        List<Integer> delays = menu.getSection().getIntegerList("delays");
        List<Integer> slots = menu.listSlots("delay-slots");
        for (int i = 0; i < Math.min(delays.size(), slots.size()); i++) {
            int delay = delays.get(i);
            Map<String, String> values = new HashMap<>();
            values.put("%ms%", String.valueOf(delay));
            menu.place(slots.get(i), menu.template("correct-after"), values, () -> mock(player, session, round.getMainAnswer(), delay));
        }
        menu.item("wrong", () -> mock(player, session, "zz" + round.getMainAnswer() + "zz", 0));
        menu.item("wrong-case", () -> {
            String swapped = swapCase(round.getMainAnswer());
            if (swapped.equals(round.getMainAnswer())) {
                Menus.message(player, "debug-no-letters");
            } else {
                mock(player, session, swapped, 0);
            }
        });
        menu.item("timeout", () -> {
            Menus.message(player, "debug-timeout-started", "%time%", String.valueOf(settings.getTimeSeconds()));
            session.add(plugin.getScheduler().asyncLater(() -> {
                if (sessions.get(player.getUniqueId()) == session) {
                    Menus.message(player, "debug-timeout", "%game%", Menus.gameName(round.getType()), "%answer%", round.getMainAnswer());
                }
            }, settings.getTimeSeconds() * 20L));
        });
        menu.item("reroll", () -> start(player, round.getType()));
        menu.item("back", () -> {
            end(player.getUniqueId());
            open(player);
        });
        menu.item("close", player::closeInventory);
        menu.open();
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
