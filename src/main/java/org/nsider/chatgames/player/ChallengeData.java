package org.nsider.chatgames.player;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ChallengeData {
    public final int winsToday;
    public final boolean rewardClaimed;
    public final LocalDate lastUpdated;
    public final int challengeStreak;
    public final int dailyStreak;
    public final int globalStreak;
    public final int weeklyWins;

    public ChallengeData(int winsToday, boolean rewardClaimed, LocalDate lastUpdated, int challengeStreak,
                         int dailyStreak, int globalStreak, int weeklyWins) {
        this.winsToday = winsToday;
        this.rewardClaimed = rewardClaimed;
        this.lastUpdated = lastUpdated;
        this.challengeStreak = challengeStreak;
        this.dailyStreak = dailyStreak;
        this.globalStreak = globalStreak;
        this.weeklyWins = weeklyWins;
    }

    public boolean isComplete() {
        return winsToday >= 3;
    }
}
