package org.nsider.chatgames.menus;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.gameevents.EventDefinition;
import org.nsider.chatgames.placeholders.LeaderboardSnapshot;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Leaderboards from the placeholder cache (refreshed in the background, so opening the
 * menu never queries the database): top points, fastest overall, fastest per game.
 * Event leaderboards are printed in chat by {@code /chatgames event top}.
 */
public final class LeaderboardMenu {

    private final ChatGames plugin;
    private final Menus menus;

    LeaderboardMenu(ChatGames plugin, Menus menus) {
        this.plugin = plugin;
        this.menus = menus;
    }

    public void open(@Nonnull Player player) {
        LeaderboardSnapshot snapshot = plugin.getLeaderboardCache().getSnapshot();
        MenuBuilder menu = MenuBuilder.of(plugin, "leaderboards", player)
                .with("%points_rank%", MainMenu.rank(snapshot.pointsRank(player.getUniqueId())))
                .with("%fastest_rank%", MainMenu.rank(snapshot.fastestRank(player.getUniqueId())))
                .with("%total_points%", snapshot.getTotalPoints());
        menu.withList("%top_points%", lines(menu, snapshot::points, false));
        menu.withList("%top_fastest%", lines(menu, snapshot::fastest, true));

        menu.item("points", null);
        menu.item("fastest", null);
        menu.item("you", null);
        menu.item("games", () -> openGames(player, 0));
        if (player.hasPermission(PermissionKey.EVENT.getPermission())) menu.item("events", () -> openEvents(player, 0));
        menu.item("back", () -> menus.getMain().open(player));
        menu.item("close", player::closeInventory);
        menu.open();
    }

    /** One item per game and race, with its fastest winners. */
    public void openGames(@Nonnull Player player, int page) {
        LeaderboardSnapshot snapshot = plugin.getLeaderboardCache().getSnapshot();
        MenuBuilder menu = MenuBuilder.of(plugin, "leaderboard-games", player);
        menu.paged(Arrays.asList(GameType.values()), page, (slot, game) -> {
            Map<String, String> values = new HashMap<>();
            values.put("%game%", Menus.gameName(game));
            values.put("%icon%", Menus.gameIcon(game));
            String key = "%top_" + game.getConfigKey() + "%";
            menu.withList(key, lines(menu, position -> snapshot.fastest(game, position), true));
            values.put("%top%", key);
            menu.place(slot, menu.template("game"), values, null);
        }, other -> openGames(player, other));
        menu.item("back", () -> open(player));
        menu.item("close", player::closeInventory);
        menu.open();
    }

    /** One item per event; clicking prints its all-time leaderboard in chat. */
    public void openEvents(@Nonnull Player player, int page) {
        MenuBuilder menu = MenuBuilder.of(plugin, "leaderboard-events", player);
        List<EventDefinition> events = new ArrayList<>(plugin.getEventManager().getConfig().getAll());
        if (events.isEmpty()) menu.item("none", null);
        menu.paged(events, page, (slot, event) -> {
            Map<String, String> values = new HashMap<>();
            values.put("%event%", event.getName());
            values.put("%id%", event.getId());
            values.put("%steps%", String.valueOf(event.getSteps().size()));
            menu.place(slot, menu.template("event"), values, () -> {
                player.closeInventory();
                player.performCommand("chatgames event top " + event.getId());
            });
        }, other -> openEvents(player, other));
        menu.item("back", () -> open(player));
        menu.item("close", player::closeInventory);
        menu.open();
    }

    private static List<String> lines(MenuBuilder menu, IntFunction<LeaderboardSnapshot.Entry> entries, boolean times) {
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
