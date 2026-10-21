package org.nsider.chatgames.menus;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.chatgames.progression.Challenge;
import org.nsider.chatgames.progression.ChallengeConfig;
import org.nsider.chatgames.progression.ChallengeTracker;
import org.nsider.chatgames.progression.Period;
import org.nsider.chatgames.progression.ProgressionManager;
import org.nsider.chatgames.progression.StreakTracker;
import org.nsider.chatgames.progression.StreakType;
import org.nsider.inventorymanager.extras.Nonnull;

import java.time.LocalDate;

/**
 * {@code /chatgames menu}: profile, challenges, streaks, leaderboards and your toggles.
 */
public final class MainMenu {

    private final ChatGames plugin;
    private final Menus menus;

    MainMenu(ChatGames plugin, Menus menus) {
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
        ChallengeConfig challenges = progression.getChallenges();
        LocalDate today = progression.today();

        MenuBuilder menu = MenuBuilder.of(plugin, "main", player)
                .with("%points%", data.getPoints())
                .with("%rank%", rank(plugin.getLeaderboardCache().getSnapshot().pointsRank(player.getUniqueId())))
                .with("%wins%", data.getStat(Stat.WINS))
                .with("%claimable%", claimable(data, today))
                .with("%games_state%", CommandSupport.state(data.isToggled()))
                .with("%sound_state%", CommandSupport.state(data.isSoundEnabled()));
        for (Period period : Period.values()) {
            menu.with("%" + period.getKey() + "_completed%", ChallengeTracker.completed(data, challenges, period, today))
                    .with("%" + period.getKey() + "_total%", challenges.active(period, today).size());
        }

        menu.item("profile", () -> menus.getProfile().open(player, player));
        menu.item("challenges", () -> menus.getChallenges().open(player, Period.DAILY));
        menu.item("streaks", () -> menus.getStreaks().open(player));
        menu.item("leaderboards", () -> menus.getLeaderboards().open(player));
        menu.item("games-toggle", data.isToggled(), () -> {
            player.performCommand("chatgames toggle");
            open(player);
        });
        menu.item("sound-toggle", data.isSoundEnabled(), () -> {
            player.performCommand("chatgames togglesound");
            open(player);
        });
        if (player.hasPermission(PermissionKey.ADMIN.getPermission())) {
            menu.item("admin", () -> menus.getAdmin().open(player));
        }
        menu.item("close", player::closeInventory);
        menu.open();
    }

    /** @return finished, unclaimed challenges plus unclaimed streak days */
    private int claimable(ChatGamesPlayer data, LocalDate today) {
        ProgressionManager progression = plugin.getProgressionManager();
        ChallengeConfig challenges = progression.getChallenges();
        int count = 0;
        for (Period period : Period.values()) {
            for (Challenge challenge : challenges.active(period, today)) {
                if (ChallengeTracker.progress(data, challenges, challenge, today) >= challenge.getAmount()
                        && !ChallengeTracker.isClaimed(data, challenges, challenge, today)) count++;
            }
        }
        for (StreakType type : StreakType.values()) {
            if (progression.getStreaks().isEnabled(type)) count += StreakTracker.unclaimed(data, type, today);
        }
        return count;
    }

    /** @return "#3", or the menus.yml text for no rank */
    @Nonnull
    static String rank(int rank) {
        return rank > 0 ? "#" + rank : ConfigManager.getMenus().getString("formats.no-rank", "-");
    }
}
