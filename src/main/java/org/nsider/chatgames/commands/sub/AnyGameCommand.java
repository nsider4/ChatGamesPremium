package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.enums.GameType;
import org.nsider.inventorymanager.extras.Nonnull;

/**
 * {@code /chatgames any}: starts the game the rotation would pick next.
 */
public class AnyGameCommand extends SubCommand {

    private final ChatGames plugin;

    public AnyGameCommand(@Nonnull ChatGames plugin) {
        super("any", PermissionKey.START_ALL.getPermission(), false, "random_game");
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        GameType type = plugin.getMainTask().pickNext();
        if (type == null) {
            CommandSupport.send(sender, MessageKey.NO_GAMES_ENABLED);
            return;
        }
        StartGameCommand.start(plugin, sender, type, new String[0]);
    }
}
