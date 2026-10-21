package org.nsider.chatgames.listeners;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.events.ChatGamesEndEvent;
import org.nsider.chatgames.games.utils.PlayerResult;
import org.nsider.chatgames.other.DiscordWebhook;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class DiscordListener implements Listener {

    private final ChatGames plugin;

    public DiscordListener(ChatGames instance) {
        this.plugin = instance;
    }

    @EventHandler
    public void onChatGamesEnd(ChatGamesEndEvent event) {
        event.getWinners().stream()
                .filter(result -> result.getPlace() == 1)
                .findFirst()
                .ifPresent(winner -> scheduleDiscordMessage(winner, event.getType()));
    }

    /**
     * Posts a titled list (e.g. an event's final standings) to the configured webhook,
     * off the main thread. Does nothing if no webhook URL is set.
     */
    public static void sendLines(ChatGames plugin, String title, List<String> lines) {
        String webhookUrl = ConfigValue.getString(ConfigPaths.DISCORD_WEBHOOK_URL);
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) return;
        DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
        webhook.setAvatarUrl(ConfigValue.getString(ConfigPaths.DISCORD_IMAGE_URL));
        webhook.setUsername(ConfigValue.getString(ConfigPaths.DISCORD_USERNAME));
        webhook.setTts(false);
        webhook.addEmbed(new DiscordWebhook.EmbedObject()
                .setTitle(plain(title))
                .setDescription(plain(String.join("\n", lines)))
                .setColor(Color.ORANGE));
        plugin.getScheduler().async(() -> {
            try {
                webhook.execute();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    /** Discord shows colour codes as text. */
    private static String plain(String text) {
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
    }

    private void scheduleDiscordMessage(PlayerResult winner, GameType type) {
        plugin.getScheduler().async(() -> sendWinnerDiscordMessage(winner, type));
    }

    private void sendWinnerDiscordMessage(PlayerResult winner, GameType type) {
        String webhookUrl = ConfigValue.getString(ConfigPaths.DISCORD_WEBHOOK_URL);
        // No URL means the Discord add-on is off
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) return;

        try {
            DiscordWebhook webhook = createDiscordWebhook(winner, type, webhookUrl);
            webhook.execute();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to send Discord webhook: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private DiscordWebhook createDiscordWebhook(PlayerResult winner, GameType type, String webhookUrl) {
        DiscordWebhook webhook = new DiscordWebhook(webhookUrl);

        webhook.setContent(ConfigValue.getString(ConfigPaths.DISCORD_MESSAGE_CONTENT));
        webhook.setAvatarUrl(ConfigValue.getString(ConfigPaths.DISCORD_IMAGE_URL));
        webhook.setUsername(ConfigValue.getString(ConfigPaths.DISCORD_USERNAME));
        webhook.setTts(false);

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                .setTitle(ConfigValue.getString(ConfigPaths.DISCORD_TITLE))
                .setDescription(ConfigValue.getString(ConfigPaths.DISCORD_DESCRIPTION))
                .setColor(Color.RED)
                .addField(
                        ConfigValue.getString(ConfigPaths.DISCORD_WINNER_FIELD)
                                .replace("%winner%", winner.getPlayer().getName())
                                .replace("%time%", String.valueOf(winner.getTime())),
                        "",
                        true
                )
                .addField(
                        ConfigValue.getString(ConfigPaths.DISCORD_GAME_FIELD)
                                .replace("%game%", type.name()),
                        "",
                        true
                )
                .setThumbnail(ConfigValue.getString(ConfigPaths.DISCORD_IMAGE_URL));

        webhook.addEmbed(embed);
        return webhook;
    }
}