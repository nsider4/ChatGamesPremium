package org.nsider.chatgames.games.manager;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.database.queries.ValueQueries;

public class GameSoundManager {
    private static ChatGames plugin;

    protected static void initialize(ChatGames plugin) {
        GameSoundManager.plugin = plugin;
    }

    public static void playGameSound(Player player, String type) {
        boolean toggleSound = ValueQueries.getPlayerToggleSound(player);

        if (toggleSound) {
            String soundType = plugin.getGameConfigManager().getGameSound(type);
            float volume = plugin.getGameConfigManager().getGameSoundVolume();

            if (!soundType.isEmpty()) {
                try {
                    player.playSound(player.getLocation(), soundType, volume, volume);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid sound type: " + soundType);
                }
            }
        }
    }
}
