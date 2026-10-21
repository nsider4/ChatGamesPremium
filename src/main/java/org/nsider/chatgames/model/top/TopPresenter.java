package org.nsider.chatgames.model.top;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.enums.TopType;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.List;

/**
 * Sends a leaderboard using its section of messages.yml ({@code top}, {@code TypersTop}
 * or {@code GameTypersTop}). An empty message line is skipped.
 */
public class TopPresenter {

    private final CommandSender sender;
    private final YamlConfig messages;
    private final TopType top;
    @Nullable private final GameType game;

    public TopPresenter(@Nonnull CommandSender sender, @Nonnull YamlConfig messages, @Nonnull TopType top, @Nullable GameType game) {
        this.sender = sender;
        this.messages = messages;
        this.top = top;
        this.game = game;
    }

    /**
     * @param personalValue the viewer's own points or best time; 0 if none or not a player
     * @param totalPoints   points of every player together, shown under the points top
     */
    public void present(@Nonnull List<TopData> rows, double personalValue, int totalPoints) {
        if (rows.isEmpty()) {
            send(line("no_data_message"));
            return;
        }

        send(line("header").replace("%game%", game != null ? game.getConfigKey() : ""));
        send(line("separators"));
        for (TopData row : rows) {
            String value = row.getFormattedValue(top);
            send(line("format")
                    .replace("%spot%", String.valueOf(row.getRank()))
                    .replace("%player%", row.getPlayerName())
                    .replace("%points%", value)
                    .replace("%time%", value));
        }
        send(line("separators"));

        if (personalValue > 0) {
            send(line("player-stat-format")
                    .replace("%fastest_time%", String.format("%.3f", personalValue))
                    .replace("%player_points%", String.valueOf((int) personalValue)));
        }
        if (top == TopType.POINTS && totalPoints > 0) {
            send(line("total-stat-format").replace("%total_wins%", String.valueOf(totalPoints)));
        }
        send(line("footer"));
    }

    private String line(String key) {
        return messages.getString(sectionOf(top) + "." + key, "");
    }

    private void send(String message) {
        if (!message.isEmpty()) MessageUtils.sendMessage(sender, message);
    }

    /**
     * @return the messages.yml section of a leaderboard
     */
    @Nonnull
    static String sectionOf(@Nonnull TopType top) {
        return top == TopType.POINTS ? "top" : top.name();
    }
}
