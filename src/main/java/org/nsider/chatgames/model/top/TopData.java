package org.nsider.chatgames.model.top;

import lombok.Getter;
import org.nsider.chatgames.enums.TopType;

/**
 * One line of a {@code /chatgames top} leaderboard.
 */
@Getter
public class TopData {
    private final int rank;
    private final String playerName;
    private final double value;

    public TopData(int rank, String playerName, double value) {
        this.rank = rank;
        this.playerName = playerName;
        this.value = value;
    }

    public String getFormattedValue(TopType type) {
        if (type == TopType.POINTS) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.3f", value);
        }
    }
}
