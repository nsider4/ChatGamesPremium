package org.nsider.chatgames.model.player;

import lombok.Getter;
import lombok.Setter;
import org.nsider.chatgames.model.top.FastestData;
import org.nsider.chatgames.utils.gameplay.MilestoneUtils;

import java.time.LocalDate;
import java.util.UUID;

@Getter
public class ChatGamesPlayer {

    private final UUID uuid;
    private int points;

    @Setter private int previousPoints = 0;
    @Setter private boolean toggled;
    @Setter private boolean soundEnabled;

    @Setter private int winsToday;
    @Setter private boolean rewardClaimed;
    @Setter private LocalDate lastUpdated;

    @Setter private int challengeStreak;
    @Setter private int dailyStreak;
    @Setter private int globalStreak;
    @Setter private int weeklyWins;

    @Setter private FastestData fastestData;

    public ChatGamesPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isDailyComplete() {
        return winsToday >= 3;
    }

    public boolean canClaimReward() {
        return isDailyComplete() && !rewardClaimed;
    }

    public void incrementWins() {
        if (!isDailyComplete() && !rewardClaimed) {
            winsToday++;
        }
    }

    public void markClaimed() {
        if (canClaimReward()) {
            rewardClaimed = true;
        }
    }

    public void resetDailyIfNeeded() {
        if (!LocalDate.now().equals(lastUpdated)) {
            winsToday = 0;
            rewardClaimed = false;
            lastUpdated = LocalDate.now();
        }
    }

    public void setPoints(int points) {
        if (points != this.points) {
            this.previousPoints = this.points;
            this.points = points;
            MilestoneUtils.checkMilestones(uuid, previousPoints, points);
        }
    }

}