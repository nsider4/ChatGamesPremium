package org.nsider.chatgames.games.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.games.Game;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.Collections;

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
    public boolean canCheckGame(Player player, Game game) {
        if (!plugin.isInGame() || game == null || game.isFinished()) {
            return false;
        }

        return !game.hasResult(player.getUniqueId())
                && plugin.getGameManager().gamesIsToggled(player)
                && plugin.getGameManager().worldEnabled(player);
    }

    /**
     * Gives rewards to a player based on their game performance.
     *
     * @param game   The game type.
     * @param player The player to reward.
     * @param place  The player's place, 1 for first.
     */
    public void giveRewards(String game, Player player, int place) {
        if (player == null) {
            return;
        }

        plugin.getScheduler().global(() -> processRewards(game, player, place));
    }

    /**
     * Processes and distributes rewards to the player.
     *
     * @param game   The game type.
     * @param player The player receiving rewards.
     * @param place  The player's place, 1 for first.
     */
    private void processRewards(String game, Player player, int place) {
        for (String reward : plugin.getRewardManager().loadGameRewards(game, player, place)) {
            processReward(player, reward);
        }
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
