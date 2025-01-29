package org.nsider.chatgames.games.manager.messages;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.games.manager.GameMessageManager;
import org.nsider.chatgames.games.manager.GameSoundManager;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.List;

public class GameStartComponentMessage extends GameMessageManager {

    public GameStartComponentMessage(ChatGames plugin) {
        super(plugin);
    }

    public void sendComponentGameMessage(Player player, TextComponent component, List<String> messages, String replace, String replacement, String variable) {
        for (String msg : messages) {

            msg = msg.replaceAll(replace, replacement);

            boolean containsVariable = msg.contains(variable);

            if (plugin.isPlaceHolderAPIEnabled()) {
                msg = PlaceholderAPI.setPlaceholders(player, msg);
            }

            if (msg.contains("<center>")) {
                msg = msg.replaceAll("<center>", "");

                if (containsVariable) {
                    if(msg.contains("%")) {
                        msg = color(msg.replaceAll(variable, ""));
                    }
                    MessageUtils.sendCenteredMessage(player, msg, component);
                } else {
                    MessageUtils.sendCenteredMessage(player, color(msg));
                }
            } else {
                if (containsVariable) {
                    if(msg.contains("%")) {
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

    public void sendGameStartedComponent(String game, TextComponent component, String variable) {
        YamlConfig messages = ConfigManager.getMessagesConfig();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().worldEnabled(player)) {
                boolean isGameToggled = plugin.getGameManager().gamesIsToggled(player);
                if (isGameToggled) {
                    GameSoundManager.playGameSound(player, "game-start");

                    sendComponentGameMessage(player,
                            component,
                            messages.getStringList(game + ".gameStartAnnouncement"),
                            "%timeToGuess%", "" + plugin.getConfig().getInt(game + ".timeToGuess_seconds"),
                            variable);

                    String titleMessage = plugin.getConfig().getString(game + ".titleMessage").replaceAll("%timeToGuess%", "" + plugin.getConfig().getInt(game + ".timeToGuess_seconds"));
                    String subtitleMessage = plugin.getConfig().getString(game + ".subtitleMessage").replaceAll("%timeToGuess%", "" + plugin.getConfig().getInt(game + ".timeToGuess_seconds"));
                    if (!titleMessage.isEmpty() && !subtitleMessage.isEmpty()) {
                        player.sendTitle(color(titleMessage), color(subtitleMessage));
                    }
                }
            }
        }
    }
}
