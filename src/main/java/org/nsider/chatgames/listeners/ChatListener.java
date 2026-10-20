package org.nsider.chatgames.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.events.ChatGamesEndEvent;
import org.nsider.chatgames.games.Game;
import org.nsider.chatgames.games.utils.PlayerResult;
import org.nsider.chatgames.managers.TopManager;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.chatgames.utils.messages.TextMatchUtils;


public class ChatListener implements Listener {

    private final ChatGames plugin;

    public ChatListener(ChatGames instance) {
        this.plugin = instance;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        // Read once: the current game can change on another thread while this runs
        Game game = plugin.getGameManager().getGame();

        if (!plugin.getGameUtils().canCheckGame(player, game)) return;
        if (!TextMatchUtils.textEqualsWord(player, event.getMessage(), game, plugin)) return;

        PlayerResult result = game.claimPlace(player.getUniqueId());
        if (result == null) return; // every place was taken a moment earlier

        if (ConfigValue.getBoolean(ConfigPaths.CANCEL_MESSAGE_ON_WIN)) {
            event.setCancelled(true);
        }

        plugin.getGameUtils().giveRewards(game.getGameConfigName(), player, result.getPlace());
        handleFastestTyper(player, game.getGameType(), result.getTime());

        if (game.isFull()) {
            concludeGame(game);
        } else {
            notifyAdditionalWinners(player, game);
        }
    }

    private void handleFastestTyper(Player player, GameType gameType, double time) {
        if (!ConfigValue.getBoolean(ConfigPaths.FASTEST_TYPERS_TOP)) return;

        TopManager.recordTime(player, gameType, time);
    }

    private void notifyAdditionalWinners(Player player, Game game) {
        if (game.getMaxWinners() > 1) {
            String waitingMessage = ConfigManager.getMessagesConfig().getString("waiting_for_other_winners");
            if (!waitingMessage.isEmpty()) {
                MessageUtils.sendMessage(player, waitingMessage);
            }
        }
    }

    private void concludeGame(Game game) {
        plugin.getScheduler().global(() -> {
            // The time limit may have ended this game first
            if (!game.finish()) return;
            ChatGamesEndEvent gameEndEvent = new ChatGamesEndEvent(game.getGameType(), game.getAnswer(), game.getResultsByPlace());
            plugin.getGameManager().sendWinnerMessages();
            Bukkit.getServer().getPluginManager().callEvent(gameEndEvent);
            plugin.getGameManager().endGame();
        });
    }
}