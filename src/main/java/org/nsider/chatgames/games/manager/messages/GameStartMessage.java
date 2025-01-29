package org.nsider.chatgames.games.manager.messages;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.games.manager.GameMessageManager;
import org.nsider.chatgames.games.manager.GameSoundManager;

public class GameStartMessage extends GameMessageManager {

    public GameStartMessage(ChatGames plugin) {
        super(plugin);
    }

    public void sendGameStartedMessage(String game, String variable, String word) {
        YamlConfig messages = ConfigManager.getMessagesConfig();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().worldEnabled(player) && plugin.getGameManager().gamesIsToggled(player)) {
                GameSoundManager.playGameSound(player, "game-start");

                sendListMessage(player, messages.getStringList(game + ".gameStartAnnouncement"),
                        variable, word,
                        "%timeToGuess%", "" + plugin.getConfig().getInt(game + ".timeToGuess_seconds"));

                String titleMessage = plugin.getConfig().getString(game + ".titleMessage");
                String subtitleMessage = plugin.getConfig().getString(game + ".subtitleMessage");
                if (!titleMessage.isEmpty() && !subtitleMessage.isEmpty()) {
                    player.sendTitle(color(titleMessage).replaceAll(variable, word), color(subtitleMessage).replaceAll(variable, word));
                }
            }
        }
    }
}
