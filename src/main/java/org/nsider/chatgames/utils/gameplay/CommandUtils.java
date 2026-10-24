package org.nsider.chatgames.utils.gameplay;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.nsider.inventorymanager.extras.Nonnull;

/**
 * Runs reward commands that need more than the console.
 */
public final class CommandUtils {

    private CommandUtils() {}

    /**
     * Runs a command as the player with operator rights, for {@code [player_as_op]} rewards.
     *
     * <p>1.9.9 used a fake {@code CommandSender} here, which modern servers reject for
     * vanilla commands and which breaks on interface methods added after 1.8. The player
     * is now made operator only for the duration of this one synchronous dispatch, and
     * the {@code finally} takes it away again even if the command throws. Must run on
     * the player's thread (the main thread outside Folia).</p>
     */
    public static void executeAsOp(@Nonnull Player player, @Nonnull String command) {
        if (player.isOp()) {
            Bukkit.dispatchCommand(player, command);
            return;
        }
        player.setOp(true);
        try {
            Bukkit.dispatchCommand(player, command);
        } finally {
            player.setOp(false);
        }
    }
}
