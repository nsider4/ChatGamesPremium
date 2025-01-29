package org.nsider.chatgames.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.events.ChatGamesEndEvent;
import org.nsider.chatgames.games.utils.PlayerResult;
import org.nsider.chatgames.other.DiscordWebhook;

import java.awt.*;
import java.io.IOException;

public class DiscordListener implements Listener {

    private final ChatGames plugin;

    public DiscordListener(ChatGames instance) {
        this.plugin = instance;
    }

    @EventHandler
    public void onChatGamesEnd(ChatGamesEndEvent event) {
        // Find the first-place winner and send the Discord message
        event.getWinners().stream()
                .filter(result -> result.getPlace() == 1)
                .findFirst()
                .ifPresent(winner -> scheduleDiscordMessage(winner, event.getType()));
    }

    /**
     * Schedules the Discord winner announcement message asynchronously.
     *
     * @param winner The winner of the game.
     * @param type   The game type.
     */
    private void scheduleDiscordMessage(PlayerResult winner, GameType type) {
        new BukkitRunnable() {
            @Override
            public void run() {
                sendWinnerDiscordMessage(winner, type);
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Sends a winner announcement message to the configured Discord webhook.
     *
     * @param winner The player who won.
     * @param type   The type of game they won.
     */
    private void sendWinnerDiscordMessage(PlayerResult winner, GameType type) {
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            plugin.getLogger().warning("Discord webhook URL is not configured.");
            return;
        }

        try {
            DiscordWebhook webhook = createDiscordWebhook(winner, type, webhookUrl);
            webhook.execute();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to send Discord webhook: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates a configured Discord webhook with the winner's details.
     *
     * @param winner     The player who won.
     * @param type       The type of game.
     * @param webhookUrl The Discord webhook URL.
     * @return A configured DiscordWebhook instance.
     */
    private DiscordWebhook createDiscordWebhook(PlayerResult winner, GameType type, String webhookUrl) {
        DiscordWebhook webhook = new DiscordWebhook(webhookUrl);

        webhook.setContent(plugin.getConfig().getString("discord.message-content"));
        webhook.setAvatarUrl(plugin.getConfig().getString("discord.img-url"));
        webhook.setUsername(plugin.getConfig().getString("discord.username"));
        webhook.setTts(false);

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                .setTitle(plugin.getConfig().getString("discord.title"))
                .setDescription(plugin.getConfig().getString("discord.description"))
                .setColor(Color.RED)
                .addField(
                        plugin.getConfig().getString("discord.winner-field")
                                .replace("%winner%", winner.getPlayer().getName())
                                .replace("%time%", String.valueOf(winner.getTime())),
                        "",
                        true
                )
                .addField(
                        plugin.getConfig().getString("discord.game-field")
                                .replace("%game%", type.name()),
                        "",
                        true
                )
                .setThumbnail(plugin.getConfig().getString("discord.img-url"));

        webhook.addEmbed(embed);
        return webhook;
    }
}
