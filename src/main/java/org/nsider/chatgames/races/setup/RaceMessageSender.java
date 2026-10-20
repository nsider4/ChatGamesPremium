package org.nsider.chatgames.races.setup;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.games.manager.GameMessageManager;
import org.nsider.chatgames.games.manager.GameSoundManager;
import org.nsider.chatgames.races.RaceUtils;

/**
 * Handles sending race start messages to eligible players.
 */
public class RaceMessageSender extends GameMessageManager {
    private final ChatGames plugin;
    private final RaceUtils raceUtils;

    public RaceMessageSender(ChatGames plugin, RaceUtils raceUtils) {
        super(plugin);
        this.plugin = plugin;
        this.raceUtils = raceUtils;
    }

    /**
     * Sends the race start message and sound to all eligible players.
     *
     * @param game      Name of the race game (used as config key prefix).
     * @param variable1 Variable placeholder in the message.
     * @param amount    Value to replace variable1 with.
     */
    public void sendRaceStartedMessage(String game, String variable1, int amount) {
        YamlConfig messages = ConfigManager.getMessagesConfig();
        String value = raceUtils.getValueName();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().worldEnabled(player)) {
                if (plugin.getGameManager().gamesIsToggled(player)) {
                    GameSoundManager.playGameSound(player, "game-start");

                    String timeKey = "RACE_" + game.toUpperCase() + "_TIME_LIMIT";
                    String timeToComplete = "0";

                    try {
                        ConfigPaths timePath = ConfigPaths.valueOf(timeKey);
                        timeToComplete = String.valueOf(ConfigValue.getInt(timePath));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid config key: " + timeKey);
                    }

                    sendListMessage(
                            player,
                            messages.getStringList(game + ".gameStartAnnouncement"),
                            "%value%", value,
                            variable1, String.valueOf(amount),
                            "%timeToComplete%", timeToComplete
                    );
                }
            }
        }
    }

    public void sendTimeExpiredToWorlds(String game) {
        YamlConfig messages = ConfigManager.getMessagesConfig();

        String var = raceUtils.getValueName();

        for(Player player : Bukkit.getOnlinePlayers()) {
            if(plugin.getGameManager().worldEnabled(player)) {
                GameSoundManager.playGameSound(player, "time-expired");

                String timeKey = "RACE_" + game.toUpperCase() + "_TIME_LIMIT";
                String timeToComplete = "0";

                try {
                    ConfigPaths timePath = ConfigPaths.valueOf(timeKey);
                    timeToComplete = String.valueOf(ConfigValue.getInt(timePath));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid config key: " + timeKey);
                }

                sendListMessage(player, messages.getStringList(game+".time_expired_message"),
                        "%timeToComplete%", timeToComplete,
                        "%value%", var,
                        "%amount%", ""+plugin.getRaceBuilder().getRace().getAmount());
            }
        }
    }
}
