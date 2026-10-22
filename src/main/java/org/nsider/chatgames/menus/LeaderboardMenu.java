package org.nsider.chatgames.menus;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.gameevents.EventDefinition;
import org.nsider.chatgames.placeholders.LeaderboardSnapshot;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.menu.Menu;
import org.nsider.inventorymanager.menu.MenuManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Leaderboards from the placeholder cache (refreshed in the background, so opening a menu
 * never queries the database): {@code leaderboards} (top points, fastest overall, your
 * ranks), {@code leaderboard-games} (fastest per game) and {@code leaderboard-events}.
 */
final class LeaderboardMenu {

    private final ChatGames plugin;

    LeaderboardMenu(ChatGames plugin) {
        this.plugin = plugin;
    }

    void register(@Nonnull MenuManager manager) {
        manager.registerMenu("leaderboards", this::buildMain);
        manager.registerMenu("leaderboard-games", this::buildGames);
        manager.registerMenu("leaderboard-events", this::buildEvents);
    }

    private void buildMain(Menu menu) {
        LeaderboardSnapshot snapshot = plugin.getLeaderboardCache().getSnapshot();
        menu.set("%points_rank%", Menus.rank(snapshot.pointsRank(menu.getPlayer().getUniqueId())))
                .set("%fastest_rank%", Menus.rank(snapshot.fastestRank(menu.getPlayer().getUniqueId())))
                .set("%total_points%", snapshot.getTotalPoints());
        menu.list("%top_points%", lines(menu, snapshot::points, false));
        menu.list("%top_fastest%", lines(menu, snapshot::fastest, true));
    }

    /** One entry per game and race; %top% points at that game's own list of lines. */
    private void buildGames(Menu menu) {
        LeaderboardSnapshot snapshot = plugin.getLeaderboardCache().getSnapshot();
        for (GameType game : GameType.values()) {
            if (game.isGlobalOnly()) continue; // no times to rank
            String key = "%top_" + game.getConfigKey() + "%";
            menu.list(key, lines(menu, position -> snapshot.fastest(game, position), true));
            Map<String, Object> values = Menus.gameValues(game);
            values.put("%top%", key);
            menu.addEntry("game", values);
        }
    }

    private void buildEvents(Menu menu) {
        for (EventDefinition event : plugin.getEventManager().getConfig().getAll()) {
            Map<String, Object> values = new HashMap<>();
            values.put("%event%", event.getName());
            values.put("%id%", event.getId());
            values.put("%steps%", event.getSteps().size());
            menu.addEntry("event", values);
        }
    }

    private static List<String> lines(Menu menu, IntFunction<LeaderboardSnapshot.Entry> entries, boolean times) {
        String format = menu.getSection().getString("formats.entry", "&e#%rank% &f%name% &7- &e%value%");
        int size = Math.max(1, menu.getSection().getInt("top-size", 10));
        List<String> lines = new ArrayList<>();
        for (int position = 1; position <= size; position++) {
            LeaderboardSnapshot.Entry entry = entries.apply(position);
            if (entry == null) break;
            String value = times ? Menus.seconds(entry.getValue()) + "s" : String.valueOf((long) entry.getValue());
            lines.add(format.replace("%rank%", String.valueOf(entry.getRank()))
                    .replace("%name%", entry.getName() != null ? entry.getName() : "?").replace("%value%", value));
        }
        if (lines.isEmpty()) lines.add(menu.getSection().getString("formats.empty", "&7Nobody yet"));
        return lines;
    }
}
