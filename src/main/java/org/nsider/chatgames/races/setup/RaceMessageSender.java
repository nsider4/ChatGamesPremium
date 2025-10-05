package org.nsider.chatgames.races.setup;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.games.manager.GameMessageManager;
import org.nsider.chatgames.games.manager.GameSoundManager;
import org.nsider.chatgames.races.RaceUtils;

public class RaceMessageSender extends GameMessageManager {
    private final ChatGames plugin;
    private final RaceUtils raceUtils;

    public RaceMessageSender(ChatGames plugin, RaceUtils raceUtils) {
        super(plugin);
        this.plugin = plugin;
        this.raceUtils = raceUtils;
    }

    public void sendRaceStartedMessage(String game, String variable1, int amount) {
        YamlConfig messages = ConfigManager.getMessagesConfig();

        String var = raceUtils.getValueName();

        for(Player player : Bukkit.getOnlinePlayers()) {
            if(plugin.getGameManager().worldEnabled(player)) {
                boolean isGameToggled = plugin.getGameManager().gamesIsToggled(player);
                if (isGameToggled) {
                    GameSoundManager.playGameSound(player, "game-start");
                    sendListMessage(player, messages.getStringList(game + ".gameStartAnnouncement"),
                            "%value%", var,
                            variable1, "" + amount,
                            "%timeToComplete%", "" + ConfigManager.getRaces().getInt(game + ".timeToComplete_seconds"));
                }
            }
        }
    }

    public void sendTimeExpiredToWorlds(String game) {
        YamlConfig messages = ConfigManager.getMessagesConfig();
        String var = raceUtils.getValueName();
        for (Player player : Bukkit.getOnlinePlayers()) {
            GameSoundManager.playGameSound(player, "time-expired");
            if (this.plugin.getGameManager().worldEnabled(player))
                sendListMessage(player, messages.getStringList(game + ".time_expired_message"),
                        "%timeToComplete%", "" +
                        ConfigManager.getRaces().getInt(game + ".timeToComplete_seconds"),
                        "%value%", var,
                        "%amount%", "" + plugin.getRaceBuilder().getRace().getAmount());
        }
    }
}
