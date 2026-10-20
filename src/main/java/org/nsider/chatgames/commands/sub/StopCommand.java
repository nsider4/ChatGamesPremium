package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.inventorymanager.extras.Nonnull;

/**
 * {@code /chatgames stop}: ends the running game or race with no winner and no announcement.
 */
public class StopCommand extends SubCommand {

    private final ChatGames plugin;

    public StopCommand(@Nonnull ChatGames plugin) {
        super("stop", PermissionKey.STOP.getPermission(), false);
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        plugin.getScheduler().onGlobal(() -> CommandSupport.send(sender,
                plugin.getGameManager().stopCurrent() ? MessageKey.GAME_STOPPED : MessageKey.NO_GAME_RUNNING));
    }
}
