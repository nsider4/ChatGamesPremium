package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.managers.CooldownManager;
import org.nsider.chatgames.managers.TopManager;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * {@code /chatgames points [player]}: your own points, or another player's with
 * {@code chatgames.points.others}.
 */
public class PointsCommand extends SubCommand {

    private final ChatGames plugin;

    public PointsCommand(@Nonnull ChatGames plugin) {
        super("points", PermissionKey.POINTS.getPermission(), false);
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                CommandSupport.usage(sender, "/chatgames points <player>");
                return;
            }
            Player player = (Player) sender;
            if (!CooldownManager.checkCooldown(player)) {
                CommandSupport.send(sender, MessageKey.IN_COOLDOWN, "%time_left%", String.valueOf(CooldownManager.getTimeLeft(player)));
                return;
            }
            if (!ChatGamesPlayerManager.isLoaded(player.getUniqueId())) {
                CommandSupport.send(sender, MessageKey.DATA_LOADING);
                return;
            }
            CommandSupport.send(sender, MessageKey.PLAYER_POINTS, "%amount%", String.valueOf(TopManager.getPoints(player)));
            return;
        }

        if (!sender.hasPermission(PermissionKey.POINTS_OTHERS.getPermission())) {
            CommandSupport.send(sender, MessageKey.NOT_ENOUGH_PERMISSIONS);
            return;
        }
        CommandSupport.findPlayer(plugin, sender, args[0], target ->
                ChatGamesPlayerManager.loadOffline(target.getUniqueId(), data -> {
                    if (data == null) return;
                    String points = String.valueOf(data.getPoints());
                    plugin.getScheduler().global(() -> CommandSupport.send(sender, MessageKey.PLAYER_POINTS_OTHER,
                            "%player%", CommandSupport.nameOf(target, args[0]), "%amount%", points));
                }));
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        return args.length == 1 && sender.hasPermission(PermissionKey.POINTS_OTHERS.getPermission())
                ? CommandSupport.onlinePlayerNames(args[0]) : Collections.<String>emptyList();
    }
}
