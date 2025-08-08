package org.nsider.chatgames.model.player;

import lombok.Getter;

/**
 * Data holder for a player's main ChatGames data.
 */
@Getter
public class PlayerMainData {
    private final int points;
    private final boolean toggled;
    private final boolean soundEnabled;

    public PlayerMainData(int points, boolean toggled, boolean soundEnabled) {
        this.points = points;
        this.toggled = toggled;
        this.soundEnabled = soundEnabled;
    }
}

