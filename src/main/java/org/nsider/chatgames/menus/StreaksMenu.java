package org.nsider.chatgames.menus;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The three streaks, each with its 7-day ladder. The ladder shows the week the streak is
 * in now (days 8-14 after a week, and so on), with today's day highlighted. Clicking a
 * claimable day, or the streak itself, claims every unclaimed day of that streak.
 */
public final class StreaksMenu {

    private static final int LADDER = StreakConfig.LADDER_DAYS;

    private final ChatGames plugin;
    private final Menus menus;

    StreaksMenu(ChatGames plugin, Menus menus) {
        this.plugin = plugin;
        this.menus = menus;
    }

    public void open(@Nonnull Player player) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data == null) {
            CommandSupport.send(player, MessageKey.DATA_LOADING);
            return;
        }
        ProgressionManager progression = plugin.getProgressionManager();
        StreakConfig streaks = progression.getStreaks();
        LocalDate today = progression.today();
        MenuBuilder menu = MenuBuilder.of(plugin, "streaks", player);

        for (StreakType type : StreakType.values()) {
            if (!streaks.isEnabled(type)) continue;
            int current = StreakTracker.current(data, type, today);
            int claimed = StreakTracker.claimed(data, type, today);
            boolean counted = StreakTracker.countedToday(data, type, today);
            // The day today counts as: already reached, or the one to reach
            int todayDay = counted ? current : current + 1;
            Runnable claim = () -> {
                if (plugin.getProgressionManager().claimStreak(player, type) == 0) {
                    CommandSupport.send(player, MessageKey.NOTHING_TO_CLAIM);
                }
                open(player);
            };

            Map<String, String> values = new HashMap<>();
            values.put("%streak%", streaks.getName(type));
            values.put("%days%", String.valueOf(current));
            values.put("%longest%", String.valueOf(StreakTracker.longest(data, type)));
            values.put("%unclaimed%", String.valueOf(current - claimed));
            ConfigurationSection info = menu.getSection().getConfigurationSection("items." + type.getKey());
            if (info != null) {
                menu.place(info.getInt("slot", -1), info, values, current > claimed ? claim : null);
            }

            List<Integer> slots = menu.listSlots("ladders." + type.getKey());
            int base = (Math.max(1, todayDay) - 1) / LADDER * LADDER;
            for (int i = 0; i < Math.min(LADDER, slots.size()); i++) {
                int day = base + i + 1;
                String status = day <= claimed ? "claimed" : day <= current ? "ready" : day == todayDay ? "today" : "locked";
                ConfigurationSection template = menu.template("day-" + status);
                if (template == null) continue;
                if (day == todayDay && !status.equals("today")) template = glowing(template);
                Map<String, String> dayValues = new HashMap<>(values);
                dayValues.put("%day%", String.valueOf(day));
                dayValues.put("%ladder_day%", String.valueOf(streaks.ladderDay(day)));
                menu.place(slots.get(i), template, dayValues, status.equals("ready") ? claim : null);
            }
        }

        menu.item("back", () -> menus.getMain().open(player));
        menu.item("close", player::closeInventory);
        menu.open();
    }

    private static ConfigurationSection glowing(ConfigurationSection template) {
        MemoryConfiguration glow = new MemoryConfiguration();
        glow.set("glow", true);
        return MenuBuilder.overlay(template, glow);
    }
}
