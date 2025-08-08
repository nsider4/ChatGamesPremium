package org.nsider.chatgames.games.manager.messages;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.games.manager.GameMessageManager;
import org.nsider.chatgames.games.manager.GameSoundManager;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.List;

public class GameStartComponentMessage extends GameMessageManager {

    public GameStartComponentMessage(ChatGames plugin) {
        super(plugin);
    }

    /**
     * Sends a clickable component message when a game starts.
     * Retains original behavior for message templates from messages.yml.
     */
    public void sendComponentGameMessage(Player player, TextComponent component, List<String> messages,
                                         String replace, String replacement, String variable) {
        for (String msg : messages) {
            msg = msg.replaceAll(replace, replacement);
            boolean containsVariable = msg.contains(variable);

            if (plugin.isPlaceHolderAPIEnabled()) {
                msg = PlaceholderAPI.setPlaceholders(player, msg);
            }

            if (msg.contains("<center>")) {
                msg = msg.replaceAll("<center>", "");

                if (containsVariable) {
                    if (msg.contains("%")) {
                        msg = color(msg.replaceAll(variable, ""));
                    }
                    MessageUtils.sendCenteredMessage(player, msg, component);
                } else {
                    MessageUtils.sendCenteredMessage(player, color(msg));
                }
            } else {
                if (containsVariable) {
                    if (msg.contains("%")) {
                        msg = color(msg.replaceAll(variable, ""));
                    }
                    TextComponent combinedComponent = new TextComponent(TextComponent.fromLegacyText(msg));
                    combinedComponent.addExtra(component);
                    player.spigot().sendMessage(combinedComponent);
                } else {
                    MessageUtils.sendMessage(player, color(msg));
                }
            }
        }
    }

    /**
     * Sends the game title, subtitle, and component to players.
     * Title and subtitle are read via ConfigValue for config.yml values only;
     * message templates (word-based config) are unaffected.
     */
    public void sendGameStartedComponent(String game, TextComponent component, String variable) {
        YamlConfig messages = ConfigManager.getMessagesConfig();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getGameManager().worldEnabled(player)) {
                continue;
            }
            if (!plugin.getGameManager().gamesIsToggled(player)) {
                continue;
            }

            GameSoundManager.playGameSound(player, "game-start");

            int timeToGuess = ConfigValue.getInt(ConfigPaths.valueOf(game.toUpperCase() + "_TIME_TO_GUESS"));

            sendComponentGameMessage(
                    player,
                    component,
                    messages.getStringList(game + ".gameStartAnnouncement"),
                    "%timeToGuess%",
                    "" + timeToGuess,
                    variable
            );

            String titleKey = game.toUpperCase() + "_TITLE_MESSAGE";
            String subtitleKey = game.toUpperCase() + "_SUBTITLE_MESSAGE";

            String titleMessage = ConfigValue.getString(ConfigPaths.valueOf(titleKey))
                    .replaceAll("%timeToGuess%", String.valueOf(timeToGuess));
            String subtitleMessage = ConfigValue.getString(ConfigPaths.valueOf(subtitleKey))
                    .replaceAll("%timeToGuess%", String.valueOf(timeToGuess));

            if (!titleMessage.isEmpty() && !subtitleMessage.isEmpty()) {
                player.sendTitle(color(titleMessage), color(subtitleMessage));
            }
        }
    }
}
