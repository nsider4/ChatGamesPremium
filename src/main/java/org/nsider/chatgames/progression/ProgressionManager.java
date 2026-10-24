package org.nsider.chatgames.progression;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.api.event.ChatGamesChallengeCompleteEvent;
import org.nsider.chatgames.api.event.ChatGamesStreakEvent;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Daily/weekly challenges and streaks: counts what players do, tells them when something
 * is complete, and pays rewards when they claim. The counting itself is in
 * {@link ChallengeTracker} and {@link StreakTracker}.
 *
 * <p>{@link #record} is called from wherever the activity happened (chat threads, region
 * threads); rewards run on the main/global thread.</p>
 */
public final class ProgressionManager {

    private final ChatGames plugin;
    private volatile ChallengeConfig challenges = ChallengeConfig.EMPTY;
    private volatile StreakConfig streaks = StreakConfig.EMPTY;

    public ProgressionManager(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
        load();
    }

    /** Reads challenges.yml and streaks.yml. */
    public void load() {
        challenges = ChallengeConfig.load(ConfigManager.getChallenges(),
                problem -> plugin.getLogger().warning("challenges.yml: " + problem));
        streaks = StreakConfig.load(ConfigManager.getStreaks(),
                problem -> plugin.getLogger().warning("streaks.yml: " + problem));
    }

    @Nonnull
    public ChallengeConfig getChallenges() {
        return challenges;
    }

    @Nonnull
    public StreakConfig getStreaks() {
        return streaks;
    }

    /** @return today in the challenges' time zone */
    @Nonnull
    public LocalDate today() {
        return challenges.today();
    }

    /**
     * Counts an activity towards the player's challenges and streaks. Any thread.
     */
    public void record(@Nonnull Player player, @Nonnull Activity activity) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data == null) return;
        ChallengeConfig challenges = this.challenges;
        LocalDate today = challenges.today();

        ChallengeTracker.Result result = ChallengeTracker.record(data, challenges, activity, today);
        for (Challenge challenge : result.getCompleted()) {
            send(player, MessageKey.CHALLENGE_COMPLETED, "%challenge%", challenge.getName(),
                    "%period%", periodName(challenge.getPeriod()));
            Bukkit.getPluginManager().callEvent(new ChatGamesChallengeCompleteEvent(player, challenge.getId(), challenge.getName(),
                    challenge.getPeriod().getKey()));
        }

        if (result.isAllDailyCompleted()) grow(player, data, StreakType.CHALLENGE, today);
        if (activity.isWin()) grow(player, data, StreakType.DAILY, today);
        if (activity.getKind() == Activity.Kind.GLOBAL) grow(player, data, StreakType.GLOBAL, today);
    }

    private void grow(Player player, ChatGamesPlayer data, StreakType type, LocalDate today) {
        StreakConfig streaks = this.streaks;
        if (!streaks.isEnabled(type)) return;
        int days = StreakTracker.advance(data, type, today);
        if (days > 0) {
            send(player, MessageKey.STREAK_GREW, "%streak%", streaks.getName(type), "%days%", String.valueOf(days));
            Bukkit.getPluginManager().callEvent(new ChatGamesStreakEvent(player, type.getKey(), days));
        }
    }

    /**
     * Claims every finished challenge and every unclaimed streak day, and gives their rewards.
     *
     * @return how many rewards were claimed
     */
    public int claim(@Nonnull Player player) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data == null) return 0;
        ChallengeConfig challenges = this.challenges;
        StreakConfig streaks = this.streaks;
        LocalDate today = challenges.today();
        int claimed = 0;

        for (Challenge challenge : ChallengeTracker.claimAll(data, challenges, today)) {
            payChallenge(player, challenge);
            claimed++;
        }
        for (StreakType type : StreakType.values()) {
            if (streaks.isEnabled(type)) claimed += payStreak(player, streaks, type, StreakTracker.claim(data, type, today));
        }
        return claimed;
    }

    /**
     * Claims one finished challenge, e.g. clicked in the menu.
     *
     * @return false if it isn't finished, was already claimed, or is no longer active
     */
    public boolean claimChallenge(@Nonnull Player player, @Nonnull Challenge challenge) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data == null || !ChallengeTracker.claim(data, challenges, challenge, today())) return false;
        payChallenge(player, challenge);
        return true;
    }

    /**
     * Claims every unclaimed day of one streak.
     *
     * @return how many days were claimed
     */
    public int claimStreak(@Nonnull Player player, @Nonnull StreakType type) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        StreakConfig streaks = this.streaks;
        if (data == null || !streaks.isEnabled(type)) return 0;
        return payStreak(player, streaks, type, StreakTracker.claim(data, type, today()));
    }

    private void payChallenge(Player player, Challenge challenge) {
        List<String> rewards = new ArrayList<>();
        for (String line : challenge.getRewards()) rewards.add(challenge.fill(line));
        plugin.getGameUtils().giveLineRewards(challenge.getPeriod().getKey() + " challenge " + challenge.getId(), player, rewards);
        send(player, MessageKey.CHALLENGE_CLAIMED, "%challenge%", challenge.getName());
    }

    /** @param days the first and last streak day just claimed, or null for none */
    private int payStreak(Player player, StreakConfig streaks, StreakType type, @Nullable int[] days) {
        if (days == null) return 0;
        String name = streaks.getName(type);
        for (int day = days[0]; day <= days[1]; day++) {
            List<String> rewards = new ArrayList<>();
            for (String line : streaks.getRewards(type, day)) {
                rewards.add(line.replace("%streak%", name).replace("%day%", String.valueOf(day)));
            }
            plugin.getGameUtils().giveLineRewards(type.getKey() + " streak day " + day, player, rewards);
        }
        send(player, MessageKey.STREAK_CLAIMED, "%streak%", name,
                "%days%", days[0] == days[1] ? String.valueOf(days[0]) : days[0] + "-" + days[1]);
        return days[1] - days[0] + 1;
    }

    /** @return "Daily" or "Weekly" from messages.yml */
    @Nonnull
    public static String periodName(@Nonnull Period period) {
        return ConfigManager.getMessages().get(period == Period.DAILY ? MessageKey.CHALLENGES_DAILY : MessageKey.CHALLENGES_WEEKLY);
    }

    private static void send(Player player, MessageKey key, String... placeholders) {
        String message = ConfigManager.getMessages().get(key, placeholders);
        if (!message.isEmpty()) MessageUtils.sendMessage(player, message);
    }
}
