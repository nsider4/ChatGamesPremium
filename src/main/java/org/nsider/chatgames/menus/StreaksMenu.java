package org.nsider.chatgames.menus;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.progression.ProgressionManager;
import org.nsider.chatgames.progression.StreakConfig;
import org.nsider.chatgames.progression.StreakTracker;
import org.nsider.chatgames.progression.StreakType;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.menu.Menu;
import org.nsider.inventorymanager.menu.MenuManager;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * The three streaks. Each has placeholders ({@code %streak_<type>%}, {@code _name},
 * {@code _longest}, {@code _unclaimed}, {@code _enabled}) and a 7-day ladder: the list named
 * after the type, showing the week the streak is in now (days 8-14 after a week...).
 * {@code [chatgames:claim-streak] <type>} claims every unclaimed day of a streak.
 */
final class StreaksMenu {

    private static final int LADDER = StreakConfig.LADDER_DAYS;

    private final ChatGames plugin;

    StreaksMenu(ChatGames plugin) {
        this.plugin = plugin;
    }

    void register(@Nonnull MenuManager manager) {
        manager.registerMenu("streaks", this::build);
        manager.registerAction("chatgames:claim-streak", this::claim);
    }

    private void build(Menu menu) {
        Player player = menu.getPlayer();
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data == null) {
            CommandSupport.send(player, MessageKey.DATA_LOADING);
            menu.cancel();
            return;
        }
        ProgressionManager progression = plugin.getProgressionManager();
        StreakConfig streaks = progression.getStreaks();
        LocalDate today = progression.today();

        for (StreakType type : StreakType.values()) {
            String prefix = "%streak_" + type.getKey();
            boolean enabled = streaks.isEnabled(type);
            int current = StreakTracker.current(data, type, today);
            int claimed = StreakTracker.claimed(data, type, today);
            menu.set(prefix + "_enabled%", enabled)
                    .set(prefix + "_name%", streaks.getName(type))
                    .set(prefix + "%", current)
                    .set(prefix + "_longest%", StreakTracker.longest(data, type))
                    .set(prefix + "_unclaimed%", Math.max(0, current - claimed));
            if (!enabled) continue;

            // The day today counts as: already reached, or the one to reach
            int todayDay = StreakTracker.countedToday(data, type, today) ? current : current + 1;
            int base = (Math.max(1, todayDay) - 1) / LADDER * LADDER;
            for (int i = 0; i < LADDER; i++) {
                int day = base + i + 1;
                String status = day <= claimed ? "claimed" : day <= current ? "ready" : day == todayDay ? "today" : "locked";
                Map<String, Object> values = new HashMap<>();
                values.put("%type%", type.getKey());
                values.put("%streak%", streaks.getName(type));
                values.put("%day%", day);
                values.put("%ladder_day%", streaks.ladderDay(day));
                values.put("%today%", day == todayDay);
                menu.addEntry(type.getKey(), "day-" + status, values, null);
            }
        }
    }

    /** {@code [chatgames:claim-streak] <type>} */
    private void claim(Menu menu, String argument) {
        for (StreakType type : StreakType.values()) {
            if (type.getKey().equalsIgnoreCase(argument.trim())) {
                if (plugin.getProgressionManager().claimStreak(menu.getPlayer(), type) == 0) {
                    CommandSupport.send(menu.getPlayer(), MessageKey.NOTHING_TO_CLAIM);
                }
                return;
            }
        }
    }
}
