package org.nsider.chatgames.games.manager;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.List;

public class GameMessageManager {
    protected final ChatGames plugin;

    public GameMessageManager(ChatGames plugin) {
        this.plugin = plugin;
    }

    public void sendListMessage(Player p, List<String> lore, String... replace) {
        boolean isGameToggled = plugin.getGameManager().gamesIsToggled(p);

        if (isGameToggled) {
            for (String msg : lore) {
                if (replace.length >= 1) {
                    for (int i = 0; i < replace.length - 1; i += 2) {
                        msg = msg.replace(replace[i], replace[i + 1]);
                    }
                }
                MessageUtils.sendMessage(p, msg);
            }
        }
    }

    protected String color(String message) {
        return MessageUtils.color(message);
    }
}
