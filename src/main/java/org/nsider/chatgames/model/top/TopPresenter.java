package org.nsider.chatgames.model.top;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.enums.TopType;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.List;

public class TopPresenter {
    private final CommandSender sender;
    private final YamlConfig messages;
    private final TopType top;
    private final GameType game;

    public TopPresenter(CommandSender sender, YamlConfig messages, TopType top, GameType... game) {
        this.sender = sender;
        this.messages = messages;
        this.top = top;
        if(game.length == 1) {
            this.game = game[0];
        } else {
            this.game = null;
        }
    }

    public void present(List<TopData> rows, double personalValue, int totalPoints) {
        String header = messages.getString(top + ".header")
                .replace("%game%", game != null ? game.name() : "");
        MessageUtils.sendMessage(sender, header);
        sender.sendMessage(MessageUtils.color(messages.getString(top + ".separators")));

        for (TopData row : rows) {
            MessageUtils.sendMessage(sender, messages.getString(top + ".format")
                    .replace("%spot%", String.valueOf(row.getRank()))
                    .replace("%player%", row.getPlayerName())
                    .replace("%points%", String.valueOf(row.getValue()))
                    .replace("%time%", String.valueOf(row.getValue())));
        }

        MessageUtils.sendMessage(sender, messages.getString(top + ".separators"));

        String playerLineKey = top.name() + ".player-stat-format";
        String playerLine = messages.getString(playerLineKey);
        if (!playerLine.isEmpty()) {
            String formatted = playerLine.replace("%fastest_time%", String.format("%.3f", personalValue))
                    .replace("%player_points%", String.valueOf((int) personalValue));
            MessageUtils.sendMessage(sender, formatted);
        }

        if (top == TopType.POINTS && totalPoints > 0) {
            String footer = messages.getString("top.total-stat-format").replace("%total_wins%", String.valueOf(totalPoints));
            if (!footer.isEmpty()) MessageUtils.sendMessage(sender, footer);
        }

        String bottom = messages.getString(top + ".footer");
        if (!bottom.isEmpty()) MessageUtils.sendMessage(sender, bottom);
    }
}

