package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.managers.TopManager;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * {@code /chatgames +points|-points|setpoints <player> <amount>}. Works for offline
 * players too. Adding points runs the milestones crossed (for an offline player,
 * when they next join); removing or setting points never does.
 */
public class ManagePointsCommand extends SubCommand {

    private enum Action { ADD, REMOVE, SET }

    private final ChatGames plugin;
    private final Action action;

    private ManagePointsCommand(ChatGames plugin, String name, Action action) {
        super(name, PermissionKey.MANAGE_POINTS.getPermission(), false);
        this.plugin = plugin;
        this.action = action;
    }

    @Nonnull
    public static ManagePointsCommand[] all(@Nonnull ChatGames plugin) {
        return new ManagePointsCommand[]{
                new ManagePointsCommand(plugin, "+points", Action.ADD),
                new ManagePointsCommand(plugin, "-points", Action.REMOVE),
                new ManagePointsCommand(plugin, "setpoints", Action.SET)
        };
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length != 2) {
            CommandSupport.usage(sender, "/chatgames " + getName() + " <player> <amount>");
            return;
        }
        Integer amount = CommandSupport.parseAmount(sender, args[1]);
        if (amount == null) return;

        CommandSupport.findPlayer(plugin, sender, args[0], target -> {
            String name = CommandSupport.nameOf(target, args[0]);
            String value = String.valueOf(amount);
            switch (action) {
                case ADD:
                    TopManager.addPoints(target, amount);
                    CommandSupport.send(sender, MessageKey.POINTS_ADDED, "%player%", name, "%amount%", value);
                    break;
                case REMOVE:
                    TopManager.addPoints(target, -amount);
                    CommandSupport.send(sender, MessageKey.POINTS_REMOVED, "%player%", name, "%amount%", value);
                    break;
                default:
                    TopManager.setPoints(target, amount);
                    CommandSupport.send(sender, MessageKey.POINTS_SET, "%player%", name, "%amount%", value);
            }
        });
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        return args.length == 1 ? CommandSupport.onlinePlayerNames(args[0]) : Collections.<String>emptyList();
    }
}
