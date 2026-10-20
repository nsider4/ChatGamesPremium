package org.nsider.chatgames.games.manager.messages;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.games.manager.GameMessageManager;
import org.nsider.chatgames.games.manager.GameSoundManager;

import java.util.List;

public class TimeExpiredMessage extends GameMessageManager {

    public TimeExpiredMessage(ChatGames plugin) {
        super(plugin);
    }

    /**
     * Tells every player in an enabled world that nobody answered in time.
     *
     * @param answer the correct answer of the game
     * @param game   the config key of the game
     */
    public void sendTimeExpiredMessage(String answer, String game) {
        List<String> lines = ConfigManager.getMessagesConfig().getStringList(game + ".time_expired_message");
        String timeToGuess = String.valueOf(ConfigValue.getInt(ConfigPaths.valueOf(game.toUpperCase() + "_TIME_TO_GUESS")));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getGameManager().worldEnabled(player)) {
                continue;
            }

            GameSoundManager.playGameSound(player, "time-expired");
            sendListMessage(player, lines,
                    "%timeToGuess%", timeToGuess,
                    "%correct_answer%", answer);
        }
    }
}
