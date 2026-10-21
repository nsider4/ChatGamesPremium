package org.nsider.chatgames.menus;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.managers.MilestoneManager;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.chatgames.progression.StreakTracker;
import org.nsider.chatgames.progression.StreakType;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.menu.Menu;
import org.nsider.inventorymanager.menu.MenuProvider;
import org.nsider.inventorymanager.menu.MenuRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code /chatgames profile [player]}: one player's stats. Offline players are loaded from
 * the database first, then the menu opens with their data ({@code [open] profile <name>}
 * from a menu works too).
 */
public final class ProfileMenu implements MenuProvider {

    private final ChatGames plugin;
    private final Menus menus;

    ProfileMenu(ChatGames plugin, Menus menus) {
        this.plugin = plugin;
        this.menus = menus;
    }

    /** Loads the target's data (from the database if offline), then opens their profile. */
    public void open(@Nonnull Player viewer, @Nonnull OfflinePlayer target) {
        ChatGamesPlayerManager.loadOffline(target.getUniqueId(), data -> {
            if (data == null) {
                CommandSupport.send(viewer, MessageKey.DATA_LOADING);
                return;
            }
            String name = target.getName() != null ? target.getName() : target.getUniqueId().toString();
            Map<String, Object> context = new HashMap<>();
            context.put("data", data);
            menus.getManager().open(viewer, new MenuRequest("profile", Collections.singletonList(name), 0, context));
        });
    }

    @Override
    public void build(@Nonnull Menu menu) {
        Player viewer = menu.getPlayer();
        ChatGamesPlayer data = menu.getContext("data");
        String name = menu.arg(0, viewer.getName());
        if (data == null) {
            if (!name.equalsIgnoreCase(viewer.getName())) {
                // Opened from a menu for someone else: load them first, then open again
                menu.cancel();
                if (!viewer.hasPermission(PermissionKey.PROFILE_OTHERS.getPermission())) {
                    CommandSupport.send(viewer, MessageKey.NOT_ENOUGH_PERMISSIONS);
                    return;
                }
                CommandSupport.findPlayer(plugin, viewer, name, target -> open(viewer, target));
                return;
            }
            data = ChatGamesPlayerManager.get(viewer.getUniqueId());
            if (data == null) {
                CommandSupport.send(viewer, MessageKey.DATA_LOADING);
                menu.cancel();
                return;
            }
        }

        LocalDate today = plugin.getProgressionManager().today();
        String none = Menus.none(menu);
        long bestPlace = data.getStat(Stat.EVENT_BEST_PLACE);
        double best = data.getBestOverall();
        menu.set("%target%", name)
                .set("%points%", data.getPoints())
                .set("%rank%", Menus.rank(plugin.getLeaderboardCache().getSnapshot().pointsRank(data.getUuid())))
                .set("%wins%", data.getStat(Stat.WINS))
                .set("%race_wins%", data.getStat(Stat.RACE_WINS))
                .set("%global_played%", data.getStat(Stat.GLOBAL_PLAYED))
                .set("%global_won%", data.getStat(Stat.GLOBAL_WON))
                .set("%contributions%", data.getStat(Stat.CONTRIBUTIONS))
                .set("%events_played%", data.getStat(Stat.EVENTS_PLAYED))
                .set("%event_wins%", data.getStat(Stat.EVENT_WINS))
                .set("%event_best_place%", bestPlace > 0 ? "#" + bestPlace : none)
                .set("%fastest%", best > 0 ? Menus.seconds(best) + "s" : none)
                .set("%challenges_completed%", data.getStat(Stat.CHALLENGES_COMPLETED))
                .set("%milestones%", milestonesReached(data.getPoints()))
                .set("%milestones_total%", MilestoneManager.getAllMilestones().size());
        for (StreakType type : StreakType.values()) {
            menu.set("%streak_" + type.getKey() + "%", StreakTracker.current(data, type, today))
                    .set("%streak_" + type.getKey() + "_longest%", StreakTracker.longest(data, type));
        }

        List<String> wins = new ArrayList<>();
        String winsFormat = menu.getSection().getString("formats.game-wins", "&7%game%: &f%wins%");
        for (GameType type : GameType.values()) {
            long count = data.getStat(Stat.wins(type));
            if (count > 0) wins.add(winsFormat.replace("%game%", Menus.gameName(type)).replace("%wins%", String.valueOf(count)));
        }
        if (wins.isEmpty()) wins.add(none);
        menu.list("%game_wins%", wins);

        List<String> times = new ArrayList<>();
        String timeFormat = menu.getSection().getString("formats.game-fastest", "&7%game%: &f%time%s");
        for (Map.Entry<GameType, Double> entry : data.getFastestTimes().entrySet()) {
            times.add(timeFormat.replace("%game%", Menus.gameName(entry.getKey())).replace("%time%", Menus.seconds(entry.getValue())));
        }
        if (times.isEmpty()) times.add(none);
        menu.list("%game_fastest%", times);
    }

    private static int milestonesReached(int points) {
        int reached = 0;
        for (int milestone : MilestoneManager.getAllMilestones()) {
            if (milestone <= points) reached++;
        }
        return reached;
    }
}
