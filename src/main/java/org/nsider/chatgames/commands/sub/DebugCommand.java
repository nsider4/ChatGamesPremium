package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.enums.GameType;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@code /chatgames debug [game] [values]}: the debug menu, or straight to a private test
 * round of one chat game (optionally with your own word/values).
 */
public class DebugCommand extends SubCommand {

    private final ChatGames plugin;

    public DebugCommand(@Nonnull ChatGames plugin) {
        super("debug", PermissionKey.DEBUG.getPermission(), true);
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        Player player = (Player) sender;
        if (args.length == 0) {
            plugin.getMenus().getDebug().open(player);
            return;
        }
        GameType type = GameType.fromName(args[0]);
        if (type == null) {
            CommandSupport.send(sender, MessageKey.UNKNOWN_GAME, "%game%", args[0]);
            return;
        }
        // A race is explained by start()
        plugin.getMenus().getDebug().start(player, type, Arrays.copyOfRange(args, 1, args.length));
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        return args.length == 1 ? CommandSupport.chatGameNames(args[0]) : Collections.<String>emptyList();
    }
}
