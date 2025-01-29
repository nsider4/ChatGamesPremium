package org.nsider.chatgames.games.manager.messages;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.manager.GameMessageManager;
import org.nsider.chatgames.games.manager.GameSoundManager;

import java.util.List;

public class MemorizationMessage extends GameMessageManager {

    public MemorizationMessage(ChatGames plugin) {
        super(plugin);
    }

    // Send memorization start message to players
    public void sendMemorizationStartMessage(String displayList, String selected) {
        List<String> gameStartAnnouncement = ConfigManager.getMessagesConfig().getStringList("shoppinglist.gameStartAnnouncement");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().worldEnabled(player) && plugin.getGameManager().gamesIsToggled(player)) {
                GameSoundManager.playGameSound(player, "game-start");

                int timeToMemorize = plugin.getConfig().getInt("shoppinglist.timeToMemorize", 5);
                sendListMessage(player, gameStartAnnouncement, "%selection%", displayList, "%timeToMemorize%", String.valueOf(timeToMemorize));

                int timeToGuess = plugin.getConfig().getInt("shoppinglist.timeToGuess_seconds", 10);

                String titleMessage = plugin.getConfig().getString("shoppinglist.titleMessage").replaceAll("%timeToGuess%", String.valueOf(timeToGuess));
                String subtitleMessage = plugin.getConfig().getString("shoppinglist.subtitleMessage").replaceAll("%timeToGuess%", String.valueOf(timeToGuess));

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    clearChat(player);
                    if (!titleMessage.isEmpty() && !subtitleMessage.isEmpty()) {
                        player.sendTitle(color(titleMessage), color(subtitleMessage));
                    }
                    sendMemorizedMessage(player, displayList, timeToGuess);
                    plugin.getGameBuilder().initGame(GameType.SHOPPINGLIST, selected);
                    plugin.getExpirationTask().timeExpiredTask(plugin.getGameManager().getGame().getGameConfigName());
                }, timeToMemorize * 20L);
            }
        }
    }

    // Helper method to clear the player's chat
    private void clearChat(Player player) {
        for (int i = 0; i < 100; i++) {
            player.sendMessage("");
        }
    }

    // Helper method to send memorized message
    private void sendMemorizedMessage(Player player, String displayList, int timeToGuess) {
        List<String> memorizedMessage = ConfigManager.getMessagesConfig().getStringList("shoppinglist.messageAfterMemorized");
        sendListMessage(player, memorizedMessage, "%selection%", displayList, "%timeToGuess%", String.valueOf(timeToGuess));
    }
}
