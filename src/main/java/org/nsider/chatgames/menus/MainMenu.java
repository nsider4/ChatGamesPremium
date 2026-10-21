package org.nsider.chatgames.menus;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.config.MessageKey;
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
import org.nsider.inventorymanager.menu.Menu;
import org.nsider.inventorymanager.menu.MenuProvider;

import java.time.LocalDate;

/**
 * {@code /chatgames menu}: profile, challenges, streaks, leaderboards and your toggles.
 */
final class MainMenu implements MenuProvider {

    private final ChatGames plugin;

    MainMenu(ChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public void build(@Nonnull Menu menu) {
        Player player = menu.getPlayer();
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data == null) {
            CommandSupport.send(player, MessageKey.DATA_LOADING);
            menu.cancel();
            return;
        }
        ProgressionManager progression = plugin.getProgressionManager();
        ChallengeConfig challenges = progression.getChallenges();
        LocalDate today = progression.today();

        menu.set("%points%", data.getPoints())
                .set("%rank%", Menus.rank(plugin.getLeaderboardCache().getSnapshot().pointsRank(player.getUniqueId())))
                .set("%wins%", data.getStat(Stat.WINS))
                .set("%claimable%", claimable(data, today))
                .set("%games_on%", data.isToggled())
                .set("%sound_on%", data.isSoundEnabled())
                .set("%games_state%", CommandSupport.state(data.isToggled()))
                .set("%sound_state%", CommandSupport.state(data.isSoundEnabled()));
        for (Period period : Period.values()) {
            menu.set("%" + period.getKey() + "_completed%", ChallengeTracker.completed(data, challenges, period, today))
                    .set("%" + period.getKey() + "_total%", challenges.active(period, today).size());
        }
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
}
