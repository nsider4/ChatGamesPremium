package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.tasks.MainTask;
import org.nsider.inventorymanager.extras.Nonnull;

/**
 * {@code /chatgames toggleAll}: turns the automatic game rotation off or back on for
 * everyone. A running game finishes normally; games can still be started by command.
 */
public class ToggleAllCommand extends SubCommand {

    private final ChatGames plugin;

    public ToggleAllCommand(@Nonnull ChatGames plugin) {
        super("toggleAll", PermissionKey.TOGGLE_ALL.getPermission(), false);
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        plugin.getScheduler().onGlobal(() -> {
            MainTask rotation = plugin.getMainTask();
            boolean on = !rotation.isTaskEnabled();
            if (on) {
                rotation.enable();
            } else {
                rotation.cancel();
            }
            CommandSupport.send(sender, MessageKey.TOGGLE_ALL, "%state%", CommandSupport.state(on));
        });
    }
}
