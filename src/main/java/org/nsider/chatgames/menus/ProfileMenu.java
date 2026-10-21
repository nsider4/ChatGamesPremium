package org.nsider.chatgames.menus;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.managers.MilestoneManager;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.chatgames.progression.ProgressionManager;
import org.nsider.chatgames.progression.StreakTracker;
import org.nsider.chatgames.progression.StreakType;
import org.nsider.inventorymanager.extras.Nonnull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code /chatgames profile [player]}: one player's stats. Offline players are loaded
 * from the database first.
 */
public final class ProfileMenu {

    private final ChatGames plugin;
    private final Menus menus;

    ProfileMenu(ChatGames plugin, Menus menus) {
        this.plugin = plugin;
        this.menus = menus;
    }

    public void open(@Nonnull Player viewer, @Nonnull OfflinePlayer target) {
        ChatGamesPlayerManager.loadOffline(target.getUniqueId(), data -> {
            if (data == null) CommandSupport.send(viewer, MessageKey.DATA_LOADING);
            else show(viewer, target, data);
        });
    }

    private void show(Player viewer, OfflinePlayer target, ChatGamesPlayer data) {
        ProgressionManager progression = plugin.getProgressionManager();
        LocalDate today = progression.today();
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString();

        MenuBuilder menu = MenuBuilder.of(plugin, "profile", viewer);
        String none = menu.text("formats.none", "&7-");
        long bestPlace = data.getStat(Stat.EVENT_BEST_PLACE);
        double best = data.getBestOverall();
        menu.with("%target%", name)
                .with("%points%", data.getPoints())
                .with("%rank%", MainMenu.rank(plugin.getLeaderboardCache().getSnapshot().pointsRank(target.getUniqueId())))
                .with("%wins%", data.getStat(Stat.WINS))
                .with("%race_wins%", data.getStat(Stat.RACE_WINS))
                .with("%global_played%", data.getStat(Stat.GLOBAL_PLAYED))
                .with("%global_won%", data.getStat(Stat.GLOBAL_WON))
                .with("%events_played%", data.getStat(Stat.EVENTS_PLAYED))
                .with("%event_wins%", data.getStat(Stat.EVENT_WINS))
                .with("%event_best_place%", bestPlace > 0 ? "#" + bestPlace : none)
                .with("%fastest%", best > 0 ? Menus.seconds(best) + "s" : none)
                .with("%challenges_completed%", data.getStat(Stat.CHALLENGES_COMPLETED))
                .with("%milestones%", milestonesReached(data.getPoints()))
                .with("%milestones_total%", MilestoneManager.getAllMilestones().size());
        for (StreakType type : StreakType.values()) {
            menu.with("%streak_" + type.getKey() + "%", StreakTracker.current(data, type, today))
                    .with("%streak_" + type.getKey() + "_longest%", StreakTracker.longest(data, type));
        }

        List<String> wins = new ArrayList<>();
        String winsFormat = menu.getSection().getString("formats.game-wins", "&7%game%: &f%wins%");
        for (GameType type : GameType.values()) {
            long count = data.getStat(Stat.wins(type));
            if (count > 0) wins.add(winsFormat.replace("%game%", Menus.gameName(type)).replace("%wins%", String.valueOf(count)));
        }
        if (wins.isEmpty()) wins.add(none);
        menu.withList("%game_wins%", wins);

        List<String> times = new ArrayList<>();
        String timeFormat = menu.getSection().getString("formats.game-fastest", "&7%game%: &f%time%s");
        for (Map.Entry<GameType, Double> entry : data.getFastestTimes().entrySet()) {
            times.add(timeFormat.replace("%game%", Menus.gameName(entry.getKey())).replace("%time%", Menus.seconds(entry.getValue())));
        }
        if (times.isEmpty()) times.add(none);
        menu.withList("%game_fastest%", times);

        for (String key : new String[]{"head", "wins", "fastest", "global", "events", "streaks", "progress"}) {
            menu.item(key, null);
        }
        menu.item("back", () -> menus.getMain().open(viewer));
        menu.item("close", viewer::closeInventory);
        menu.open();
    }

    private static int milestonesReached(int points) {
        int reached = 0;
        for (int milestone : MilestoneManager.getAllMilestones()) {
            if (milestone <= points) reached++;
        }
        return reached;
    }
}
