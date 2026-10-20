package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.inventorymanager.extras.Nonnull;

/**
 * {@code /chatgames claim}: claims every finished challenge and unclaimed streak day.
 */
public class ClaimCommand extends SubCommand {

    private final ChatGames plugin;

    public ClaimCommand(@Nonnull ChatGames plugin) {
        super("claim", PermissionKey.CLAIM.getPermission(), true);
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        Player player = (Player) sender;
        if (!ChatGamesPlayerManager.isLoaded(player.getUniqueId())) {
            CommandSupport.send(sender, MessageKey.DATA_LOADING);
            return;
        }
        if (plugin.getProgressionManager().claim(player) == 0) {
            CommandSupport.send(sender, MessageKey.NOTHING_TO_CLAIM);
        }
    }
}
