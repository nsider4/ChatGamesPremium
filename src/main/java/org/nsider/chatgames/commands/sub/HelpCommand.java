package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.commands.HelpFilter;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.utils.messages.Msg;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.function.Function;

/**
 * {@code /chatgames help} (also plain {@code /chatgames}): the command list from messages.yml,
 * without the commands the sender can't use.
 */
public class HelpCommand extends SubCommand {

    private final Function<CommandSender, HelpFilter.Access> access;

    /**
     * @param access what each sender can use, for {@link HelpFilter}
     */
    public HelpCommand(@Nonnull Function<CommandSender, HelpFilter.Access> access) {
        super("help", PermissionKey.HELP.getPermission(), false);
        this.access = access;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        Msg.of(HelpFilter.filter(ConfigManager.getMessagesConfig().getStringList("help_command"), access.apply(sender))).send(sender);
    }
}
