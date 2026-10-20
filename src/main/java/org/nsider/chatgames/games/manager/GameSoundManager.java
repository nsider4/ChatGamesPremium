package org.nsider.chatgames.games.manager;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;

public class GameSoundManager {
    private static ChatGames plugin;

    protected static void initialize(ChatGames plugin) {
        GameSoundManager.plugin = plugin;
    }

    public static void playGameSound(Player player, String type) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        boolean toggleSound = data == null || data.isSoundEnabled();

        if (toggleSound) {
            String soundType = plugin.getGameConfigManager().getGameSound(type);
            float volume = plugin.getGameConfigManager().getGameSoundVolume();
            float pitch = plugin.getGameConfigManager().getGameSoundPitch();

            if (!soundType.isEmpty()) {
                try {
                    player.playSound(player.getLocation(), soundType, volume, pitch);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid sound type: " + soundType);
                }
            }
        }
    }
}
