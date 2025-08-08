package org.nsider.chatgames.games.manager.messages;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.manager.GameMessageManager;
import org.nsider.chatgames.games.manager.GameSoundManager;

import java.util.List;

public class MemorizationMessage extends GameMessageManager {

    public MemorizationMessage(ChatGames plugin) {
        super(plugin);
    }

    public void sendMemorizationStartMessage(String displayList, String selected) {
        YamlConfig messages = ConfigManager.getMessagesConfig();
        List<String> gameStartAnnouncement = messages.getStringList("memorize.gameStartAnnouncement");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().worldEnabled(player) && plugin.getGameManager().gamesIsToggled(player)) {
                GameSoundManager.playGameSound(player, "game-start");

                int timeToMemorize = ConfigValue.getInt(ConfigPaths.MEMORIZE_TIME_TO_MEMORIZE);
                int timeToGuess = ConfigValue.getInt(ConfigPaths.MEMORIZE_TIME_TO_GUESS);

                sendListMessage(player, gameStartAnnouncement,
                        "%selection%", displayList,
                        "%timeToMemorize%", String.valueOf(timeToMemorize));

                String titleMessage = ConfigValue.getString(ConfigPaths.MEMORIZE_TITLE_MESSAGE)
                        .replace("%timeToGuess%", String.valueOf(timeToGuess));

                String subtitleMessage = ConfigValue.getString(ConfigPaths.MEMORIZE_SUBTITLE_MESSAGE)
                        .replace("%timeToGuess%", String.valueOf(timeToGuess));

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    clearChat(player);
                    if (!titleMessage.isEmpty() && !subtitleMessage.isEmpty()) {
                        player.sendTitle(color(titleMessage), color(subtitleMessage));
                    }
                    sendMemorizedMessage(player, displayList, timeToGuess);
                    plugin.getGameBuilder().initGame(GameType.MEMORIZE, selected);
                    plugin.getExpirationTask().timeExpiredTask(plugin.getGameManager().getGame().getGameConfigName());
                }, timeToMemorize * 20L);
            }
        }
    }

    private void clearChat(Player player) {
        for (int i = 0; i < 100; i++) {
            player.sendMessage("");
        }
    }

    private void sendMemorizedMessage(Player player, String displayList, int timeToGuess) {
        List<String> memorizedMessage = ConfigManager.getMessagesConfig().getStringList("memorize.messageAfterMemorized");
        sendListMessage(player, memorizedMessage,
                "%selection%", displayList,
                "%timeToGuess%", String.valueOf(timeToGuess));
    }
}