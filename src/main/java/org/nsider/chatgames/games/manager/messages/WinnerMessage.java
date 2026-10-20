package org.nsider.chatgames.games.manager.messages;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.games.manager.GameMessageManager;
import org.nsider.chatgames.games.manager.GameSoundManager;
import org.nsider.chatgames.games.utils.PlayerResult;
import org.nsider.chatgames.managers.TopManager;
import org.nsider.chatgames.utils.management.TimeUtils;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class WinnerMessage extends GameMessageManager {

    public WinnerMessage(ChatGames plugin) {
        super(plugin);
    }

    // Send messages to winners with their respective stats
    public void sendMessageForWinners() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().worldEnabled(player)) {
                sendListMessage(player,
                        getMessageForWinners(
                                ConfigManager.getMessagesConfig().getStringList(
                                        plugin.getGameManager().getGame().getGameConfigName() + ".correct_message")
                        )
                );
            }
            GameSoundManager.playGameSound(player, "win");
        }
    }

    // Helper method to get the formatted message for winners
    public List<String> getMessageForWinners(List<String> messages) {
        List<String> modifiedMessages = new ArrayList<>();

        for (String originalMsg : messages) {
            String modifiedMessage = getPlayerPoints(originalMsg);
            modifiedMessages.add(modifiedMessage);
        }

        List<String> finalMessages = new ArrayList<>();
        for (String modifiedMsg : modifiedMessages) {
            finalMessages.add(MessageUtils.removePercentSymbols(modifiedMsg));
        }

        return finalMessages;
    }

    // Helper method to format player points
    private String getPlayerPoints(String message) {
        StringBuilder updatedMessage = new StringBuilder(message);
        String defaultPlaceholder = ConfigManager.getMessagesConfig().getString("no_winner_placeholder");
        int maxWinners = plugin.getGameConfigManager().getGameWinners(plugin.getGameManager().getGame().getGameConfigName());
        boolean[] placeholdersUsed = new boolean[maxWinners + 1];

        for (PlayerResult result : plugin.getGameManager().getGame().getResultsByPlace()) {
            int place = result.getPlace();
            int points = TopManager.getPoints(result.getPlayer());

            if (place <= maxWinners && place >= 1) {
                placeholdersUsed[place] = true;

                replacePlaceholder(updatedMessage, "%winner_" + place + "%", getOrDefault(result.getPlayer().getName(), defaultPlaceholder));
                if (result.getPlayer().isOnline()) {
                    replacePlaceholder(updatedMessage, "%winner_displayname_" + place + "%", getOrDefault(result.getPlayer().getPlayer().getDisplayName(), defaultPlaceholder));
                }
                replacePlaceholder(updatedMessage, "%time_" + place + "%", getOrDefault("" + TimeUtils.roundTo2Decimals(result.getTime()), defaultPlaceholder));
                replacePlaceholder(updatedMessage, "%points_" + place + "%", getOrDefault("" + points, defaultPlaceholder));
            } else {
                plugin.getLogger().warning("Invalid place: " + place + ". Skipping placeholder update.");
            }
        }

        replacePlaceholder(updatedMessage, "%correct_answer%", getOrDefault(plugin.getGameManager().getGame().getAnswer(), defaultPlaceholder));

        for (int i = 1; i <= maxWinners; i++) {
            if (!placeholdersUsed[i]) {
                replacePlaceholder(updatedMessage, "%winner_" + i + "%", defaultPlaceholder);
                replacePlaceholder(updatedMessage, "%winner_displayname_" + i + "%", defaultPlaceholder);
                replacePlaceholder(updatedMessage, "%time_" + i + "%", defaultPlaceholder);
                replacePlaceholder(updatedMessage, "%points_" + i + "%", defaultPlaceholder);
            }
        }

        return color(updatedMessage.toString());
    }

    private void replacePlaceholder(StringBuilder message, String placeholder, String value) {
        int start;
        while ((start = message.indexOf(placeholder)) != -1) {
            message.replace(start, start + placeholder.length(), value);
        }
    }

    private String getOrDefault(String value, String defaultPlaceholder) {
        return (value == null || value.isEmpty()) ? defaultPlaceholder : value;
    }
}
