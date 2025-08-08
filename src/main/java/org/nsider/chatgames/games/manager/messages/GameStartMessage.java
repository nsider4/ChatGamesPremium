package org.nsider.chatgames.games.manager.messages;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.games.manager.GameMessageManager;
import org.nsider.chatgames.games.manager.GameSoundManager;

public class GameStartMessage extends GameMessageManager {

    public GameStartMessage(ChatGames plugin) {
        super(plugin);
    }

    public void sendGameStartedMessage(String game, String variable, String word) {
        YamlConfig messages = ConfigManager.getMessagesConfig();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getGameManager().worldEnabled(player) || !plugin.getGameManager().gamesIsToggled(player)) {
                continue;
            }

            GameSoundManager.playGameSound(player, "game-start");

            sendListMessage(
                    player,
                    messages.getStringList(game + ".gameStartAnnouncement"),
                    variable,
                    word,
                    "%timeToGuess%",
                    String.valueOf(ConfigValue.getInt(ConfigPaths.valueOf(game.toUpperCase() + "_TIME_TO_GUESS")))
            );

            String titleMessage = ConfigValue.getString(
                    ConfigPaths.valueOf(game.toUpperCase() + "_TITLE_MESSAGE")
            ).replace(variable, word);

            String subtitleMessage = ConfigValue.getString(
                    ConfigPaths.valueOf(game.toUpperCase() + "_SUBTITLE_MESSAGE")
            ).replace(variable, word);

            if (!titleMessage.isEmpty() && !subtitleMessage.isEmpty()) {
                player.sendTitle(color(titleMessage), color(subtitleMessage));
            }
        }
    }
}
