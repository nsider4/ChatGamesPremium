package org.nsider.chatgames.races.setup;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.races.RaceUtils;

public class RaceMessageSender {
    private final ChatGames plugin;
    private final RaceUtils raceUtils;

    public RaceMessageSender(ChatGames plugin, RaceUtils raceUtils) {
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
                    plugin.getGameManager().playGameSound(player, "game-start");
                    plugin.getGameManager().sendListMessage(player, messages.getStringList(game + ".gameStartAnnouncement"),
                            "%value%", var,
                            variable1, "" + amount,
                            "%timeToComplete%", "" + ConfigManager.getChatRaces().getInt(game + ".timeToComplete_seconds"));
                }
            }
        }
    }
}
