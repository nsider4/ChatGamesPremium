package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.utils.messages.Msg;
import org.nsider.inventorymanager.extras.Nonnull;

/**
 * {@code /chatgames help} (also plain {@code /chatgames}): the command list from messages.yml.
 */
public class HelpCommand extends SubCommand {

    public HelpCommand() {
        super("help", PermissionKey.HELP.getPermission(), false);
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        Msg.of(ConfigManager.getMessagesConfig().getStringList("help_command")).send(sender);
    }
}
