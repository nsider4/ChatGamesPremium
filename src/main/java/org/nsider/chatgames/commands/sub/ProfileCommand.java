package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * {@code /chatgames profile [player]}: opens a player's stats menu, online or not.
 */
public class ProfileCommand extends SubCommand {

    private final ChatGames plugin;

    public ProfileCommand(@Nonnull ChatGames plugin) {
        super("profile", PermissionKey.PROFILE.getPermission(), true, "stats");
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        Player player = (Player) sender;
        if (args.length == 0) {
            plugin.getMenus().getProfile().open(player, player);
            return;
        }
        if (!sender.hasPermission(PermissionKey.PROFILE_OTHERS.getPermission())) {
            CommandSupport.send(sender, MessageKey.NOT_ENOUGH_PERMISSIONS);
            return;
        }
        CommandSupport.findPlayer(plugin, sender, args[0], target -> plugin.getMenus().getProfile().open(player, target));
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        return args.length == 1 && sender.hasPermission(PermissionKey.PROFILE_OTHERS.getPermission())
                ? CommandSupport.onlinePlayerNames(args[0]) : Collections.<String>emptyList();
    }
}
