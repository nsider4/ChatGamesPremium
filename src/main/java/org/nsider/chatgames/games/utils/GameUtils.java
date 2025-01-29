package org.nsider.chatgames.games.utils;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.games.GameController;
import org.nsider.chatgames.utils.management.TimeUtils;
import org.bukkit.Bukkit;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.Collections;
import java.util.List;

/**
 * Game utilities.
 */
public class GameUtils {
    private final ChatGames plugin;

    public GameUtils(ChatGames instance) {
        this.plugin = instance;
    }

    /**
     * Determines if a player can check the current game.
     *
     * @param player The player to check.
     * @return true if the player can participate, false otherwise.
     */
    public boolean canCheckGame(Player player) {
        if (!plugin.isInGame() || plugin.getGameManager().getGame() == null) {
            return false;
        }

        return plugin.getGameManager().gamesIsToggled(player)
                && plugin.getGameManager().worldEnabled(player)
                && !GameController.playerExists(player.getUniqueId());
    }

    /**
     * Adds a winner and updates game statistics.
     *
     * @param player The player who won.
     */
    public void addWinner(OfflinePlayer player) {
        TimeUtils.stopTimer();
        double elapsedTime = Double.parseDouble(TimeUtils.getElapsedTime());
        TimeUtils.setLastTimer(TimeUtils.getLastTimer() + elapsedTime);
        GameController.addResult(player.getUniqueId(), TimeUtils.getLastTimer(), GameController.getResults().size() + 1);
    }

    /**
     * Gives rewards to a player based on their game performance.
     *
     * @param game  The game type.
     * @param player The player to reward.
     */
    public void giveRewards(String game, Player player) {
        if (player == null) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> processRewards(game, player));
    }

    /**
     * Processes and distributes rewards to the player.
     *
     * @param game   The game type.
     * @param player The player receiving rewards.
     */
    private void processRewards(String game, Player player) {
        List<String> rewards = getPlayerRewards(game, player);
        if (rewards.isEmpty()) {
            return;
        }

        for (String reward : rewards) {
            processReward(player, reward);
        }
    }

    /**
     * Retrieves rewards for a player based on game performance.
     *
     * @param game   The game type.
     * @param player The player receiving rewards.
     * @return List of rewards.
     */
    private List<String> getPlayerRewards(String game, Player player) {
        PlayerResult playerResult = GameController.getPlayerResult(player.getUniqueId());
        return (playerResult == null) ? Collections.emptyList()
                : plugin.getRewardManager().loadGameRewards(game, player, playerResult.getPlace());
    }

    /**
     * Processes a single reward command.
     *
     * @param player The player receiving the reward.
     * @param reward The reward string.
     */
    private void processReward(Player player, String reward) {
        String[] parts = reward.split(" ");

        if (isGiveCommand(parts)) {
            giveItemReward(player, parts);
        } else {
            plugin.getRewardManager().executeCommands(Collections.singletonList(reward), player);
        }
    }

    /**
     * Checks if the reward command is a "give" command.
     *
     * @param parts The reward command split into parts.
     * @return true if it's an item give command, false otherwise.
     */
    private boolean isGiveCommand(String[] parts) {
        return parts.length >= 4 && parts[1].equalsIgnoreCase("give");
    }

    /**
     * Parses and gives an item reward to the player.
     *
     * @param player The player receiving the reward.
     * @param parts  The parts of the give command.
     */
    private void giveItemReward(Player player, String[] parts) {
        Material material = Material.getMaterial(parts[3].toUpperCase());
        int amount = (parts.length > 4) ? Integer.parseInt(parts[4]) : 1;

        if (material == null) {
            return; //Invalid material, do nothing
        }

        ItemStack rewardItem = new ItemStack(material, amount);
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), rewardItem);
            MessageUtils.sendMessage(player, ConfigManager.getMessagesConfig().getString("inventory_full"));
        } else {
            player.getInventory().addItem(rewardItem);
        }
    }
}
