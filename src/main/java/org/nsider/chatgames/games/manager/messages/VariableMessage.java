package org.nsider.chatgames.games.manager.messages;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.games.manager.GameMessageManager;
import org.nsider.chatgames.games.manager.GameSoundManager;
import org.nsider.chatgames.utils.messages.CenteredMessageUtils;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class VariableMessage extends GameMessageManager {

    public VariableMessage(ChatGames plugin) {
        super(plugin);
    }

    public void sendVariableGameMessage(List<String> game, List<String> list, String replace, String replacement, String replace1, String replacement1) {
        List<String> messagesToSend = new ArrayList<>();

        for (String msg : list) {
            String newMsg = msg
                    .replaceAll("%line_1%", game.get(0))
                    .replaceAll("%line_2%", game.get(1))
                    .replaceAll("%line_3%", game.get(2))
                    .replaceAll(replace, replacement)
                    .replaceAll(replace1, replacement1);

            if (msg.contains("<center>")) {
                List<String> centeredMessages = CenteredMessageUtils.getCenteredMessage(newMsg.replaceAll("<center>", ""));
                messagesToSend.addAll(centeredMessages);
            } else {
                messagesToSend.add(newMsg);
            }
        }

        String symbolToGet = ConfigValue.getString(ConfigPaths.VARIABLE_TO_GET_SYMBOL);
        String titleMessage = ConfigValue.getString(ConfigPaths.VARIABLE_TITLE_MESSAGE).replaceAll("%symbol%", symbolToGet);
        String subtitleMessage = ConfigValue.getString(ConfigPaths.VARIABLE_SUBTITLE_MESSAGE).replaceAll("%symbol%", symbolToGet);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().worldEnabled(p) && plugin.getGameManager().gamesIsToggled(p)) {
                GameSoundManager.playGameSound(p, "game-start");

                for (String message : messagesToSend) {
                    MessageUtils.sendMessage(p, color(message));
                }

                if (!titleMessage.isEmpty() && !subtitleMessage.isEmpty()) {
                    p.sendTitle(color(titleMessage), color(subtitleMessage));
                }
            }
        }
    }
}
