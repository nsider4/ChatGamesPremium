package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * {@code /chatgames remove <player>}: deletes everything stored about a player
 * (points, times, stats). An online player starts over from zero.
 */
public class RemoveCommand extends SubCommand {

    private final ChatGames plugin;

    public RemoveCommand(@Nonnull ChatGames plugin) {
        super("remove", PermissionKey.REMOVE.getPermission(), false);
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length != 1) {
            CommandSupport.usage(sender, "/chatgames remove <player>");
            return;
        }
        CommandSupport.findPlayer(plugin, sender, args[0], target ->
                ChatGamesPlayerManager.delete(target.getUniqueId()).whenComplete((existed, error) ->
                        plugin.getScheduler().global(() -> {
                            String name = CommandSupport.nameOf(target, args[0]);
                            if (error != null) {
                                plugin.getLogger().log(Level.SEVERE, "Could not remove " + name, error);
                            } else if (existed) {
                                CommandSupport.send(sender, MessageKey.PLAYER_DELETED, "%player%", name);
                            } else {
                                CommandSupport.send(sender, MessageKey.PLAYER_NOT_FOUND, "%player%", name);
                            }
                        })));
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        return args.length == 1 ? CommandSupport.onlinePlayerNames(args[0]) : Collections.<String>emptyList();
    }
}
