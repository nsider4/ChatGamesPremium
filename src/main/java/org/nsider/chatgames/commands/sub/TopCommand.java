package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.database.queries.TopPlayerQueries;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.enums.TopType;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * {@code /chatgames top} (most points) and {@code /chatgames fastest [game]} (fastest
 * winning times, overall or for one game).
 */
public class TopCommand extends SubCommand {

    private final boolean fastest;

    private TopCommand(String name, PermissionKey permission, boolean fastest) {
        super(name, permission.getPermission(), false);
        this.fastest = fastest;
    }

    @Nonnull
    public static TopCommand points() {
        return new TopCommand("top", PermissionKey.TOP, false);
    }

    @Nonnull
    public static TopCommand fastest() {
        return new TopCommand("fastest", PermissionKey.FASTEST_TOP, true);
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (!fastest) {
            TopPlayerQueries.sendTopMessage(sender, TopType.POINTS);
            return;
        }
        if (!ConfigValue.getBoolean(ConfigPaths.FASTEST_TYPERS_TOP)) {
            CommandSupport.send(sender, MessageKey.FASTEST_DISABLED);
            return;
        }
        if (args.length == 0) {
            TopPlayerQueries.sendTopMessage(sender, TopType.TypersTop);
            return;
        }
        GameType game = GameType.fromName(args[0]);
        if (game == null) {
            CommandSupport.send(sender, MessageKey.UNKNOWN_GAME, "%game%", args[0]);
            return;
        }
        TopPlayerQueries.sendTopMessage(sender, TopType.GameTypersTop, game);
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        return fastest && args.length == 1 ? CommandSupport.chatGameNames(args[0]) : Collections.<String>emptyList();
    }
}
