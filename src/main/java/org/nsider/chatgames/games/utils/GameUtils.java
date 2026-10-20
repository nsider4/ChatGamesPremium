package org.nsider.chatgames.games.utils;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.List;
import java.util.Optional;

/**
 * Game utilities.
 */
public class GameUtils {
    private final ChatGames plugin;

    public GameUtils(ChatGames instance) {
        this.plugin = instance;
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

        plugin.getScheduler().global(() -> processRewards(plugin.getRewardManager().chatGameRewards(game, player, place), player));
    }

    /**
     * Gives a race winner the rewards races.yml lists for the race's target and amount.
     *
     * @param names reward names from races.yml, found in rewards.yml under {@code races.rewards}
     */
    public void giveRaceRewards(String race, Player player, List<String> names) {
        plugin.getScheduler().global(() -> processRewards(plugin.getRewardManager().raceRewards(race, player, names), player));
    }

    /**
     * Gives a player who made it in a global game their rewards from global_games.yml.
     */
    public void giveGlobalRewards(GameType type, Player player, int place) {
        plugin.getScheduler().global(() -> processRewards(plugin.getRewardManager().globalRewards(type, player, place), player));
    }

    /**
     * Gives a player reward lines in the rewards.yml {@code data} format (challenges, streaks).
     *
     * @param what names the source in console warnings, e.g. "daily challenge win_5"
     */
    public void giveLineRewards(String what, Player player, List<String> lines) {
        if (lines.isEmpty()) return;
        plugin.getScheduler().global(() -> processRewards(plugin.getRewardManager().lineRewards(what, player, lines), player));
    }

    private void processRewards(List<String> rewards, Player player) {
        for (String reward : rewards) {
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
            plugin.getRewardManager().executeCommand(reward, player);
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
     * Gives the item of a "<prefix> give <player> <item> [amount]" reward. Item names may be
     * legacy or modern (XSeries maps them to this server's version). If the inventory is
     * full the item drops at the player's feet.
     */
    private void giveItemReward(Player player, String[] parts) {
        Optional<XMaterial> material = XMaterial.matchXMaterial(parts[3]);
        ItemStack item = material.isPresent() ? material.get().parseItem() : null;
        if (item == null) {
            plugin.getLogger().warning("Unknown item in a reward: " + parts[3]);
            return;
        }
        int amount = 1;
        if (parts.length > 4) {
            try {
                amount = Math.max(1, Integer.parseInt(parts[4]));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid amount in a reward: " + parts[4]);
            }
        }
        item.setAmount(amount);
        // Inventories belong to the player's region on Folia
        plugin.getScheduler().atPlayer(player, () -> {
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
                MessageUtils.sendMessage(player, ConfigManager.getMessagesConfig().getString("inventory_full"));
            } else {
                player.getInventory().addItem(item);
            }
        });
    }
}
