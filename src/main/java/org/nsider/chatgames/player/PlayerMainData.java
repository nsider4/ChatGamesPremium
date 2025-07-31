package org.nsider.chatgames.player;

import lombok.Getter;

/**
 * Data holder for a player's main ChatGames data.
 */
@Getter
public class PlayerMainData {
    private final int points;
    private final boolean toggled;
    private final boolean soundEnabled;
    private final double fastest;

    public PlayerMainData(int points, boolean toggled, boolean soundEnabled, double fastest) {
        this.points = points;
        this.toggled = toggled;
        this.soundEnabled = soundEnabled;
        this.fastest = fastest;
    }
}

