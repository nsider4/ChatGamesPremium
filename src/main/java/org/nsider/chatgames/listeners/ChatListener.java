package org.nsider.chatgames.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.events.ChatGamesEndEvent;
import org.nsider.chatgames.games.GameController;
import org.nsider.chatgames.games.utils.PlayerResult;
import org.nsider.chatgames.managers.TopManager;
import org.nsider.chatgames.utils.management.TimeUtils;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.chatgames.utils.messages.TextMatchUtils;
import org.nsider.chatgames.games.Game;

import java.util.List;

public class ChatListener implements Listener {

    private final ChatGames plugin;

    public ChatListener(ChatGames instance) {
        this.plugin = instance;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (!plugin.getGameUtils().canCheckGame(player)) {
            return;
        }
        if (!TextMatchUtils.textEqualsWord(player, message, plugin.getGameManager().getGame(), plugin)) {
            return;
        }

        handleCorrectAnswer(event, player);
    }

    /**
     * Handles logic for a player who gives a correct answer.
     *
     * @param event  The chat event.
     * @param player The player who gave the correct answer.
     */
    private void handleCorrectAnswer(AsyncPlayerChatEvent event, Player player) {
        Game currentGame = plugin.getGameManager().getGame();
        int maxWinners = plugin.getGameConfigManager().getGameWinners(currentGame.getGameConfigName());

        if (plugin.getConfig().getBoolean("cancel-message-when-winning")) {
            event.setCancelled(true);
        }

        if (GameController.getResults().size() < maxWinners) {
            processWinner(player, currentGame);
        }

        if (GameController.getResults().size() == maxWinners) {
            concludeGame(currentGame);
        }
    }

    /**
     * Processes the actions for a winning player.
     *
     * @param player The winning player.
     * @param game   The current game.
     */
    private void processWinner(Player player, Game game) {
        plugin.getGameUtils().addWinner(player);
        plugin.getGameUtils().giveRewards(game.getGameConfigName(), player);
        TopManager.addPoint(player, 1);

        handleFastestTyper(player);
        notifyAdditionalWinners(player, game);
        TimeUtils.startTimer();
    }

    /**
     * Handles logic for updating the fastest typer.
     *
     * @param player The player being checked for fastest typing.
     */
    private void handleFastestTyper(Player player) {
        if (!plugin.getConfig().getBoolean("fastest-typers-top")) {
            return;
        }

        boolean isFaster = TopManager.isFasterThan(player, TimeUtils.getLastTimer());

        if(isFaster) {
            TopManager.setFastestTime(player, TimeUtils.getLastTimer());
        }
    }

    /**
     * Notifies the player if additional winners are needed.
     *
     * @param player The current winning player.
     * @param game   The current game.
     */
    private void notifyAdditionalWinners(Player player, Game game) {
        int maxWinners = plugin.getGameConfigManager().getGameWinners(game.getGameConfigName());

        if (maxWinners > 1) {
            String waitingMessage = ConfigManager.getMessagesConfig().getString("waiting_for_other_winners");
            if (!waitingMessage.isEmpty()) {
                MessageUtils.sendMessage(player, waitingMessage);
            }
        }
    }

    /**
     * Concludes the current game and announces the winners.
     *
     * @param game The game to conclude.
     */
    private void concludeGame(Game game) {
        List<PlayerResult> results = GameController.getResults();
        ChatGamesEndEvent gameEndEvent = new ChatGamesEndEvent(plugin, game.getGameType(), results);

        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getGameManager().sendWinnerMessages();
            Bukkit.getServer().getPluginManager().callEvent(gameEndEvent);
            plugin.getGameManager().endGame();
        });
    }
}
