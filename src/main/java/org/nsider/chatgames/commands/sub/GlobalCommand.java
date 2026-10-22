package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code /chatgames global <game> [values]}: starts a global game or race, taking the
 * same values as starting it normally.
 */
public class GlobalCommand extends SubCommand {

    private final ChatGames plugin;

    public GlobalCommand(@Nonnull ChatGames plugin) {
        super("global", PermissionKey.GLOBAL.getPermission(), false);
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length == 0) {
            CommandSupport.usage(sender, "/chatgames global <" + String.join("|", names()) + "> [values]");
            return;
        }
        GameType type = GameType.fromName(args[0]);
        if (type == null) {
            CommandSupport.send(sender, MessageKey.UNKNOWN_GAME, "%game%", args[0]);
            return;
        }
        if (plugin.getGlobalGameManager().getConfig().get(type) == null) {
            CommandSupport.send(sender, MessageKey.GLOBAL_NOT_ENABLED, "%game%", type.getConfigKey());
            return;
        }
        StartGameCommand.start(plugin, sender, type, Arrays.copyOfRange(args, 1, args.length), true);
    }

    private List<String> names() {
        List<String> names = new ArrayList<>();
        for (GameType type : plugin.getGlobalGameManager().getConfig().getEnabled()) names.add(type.getConfigKey());
        return names;
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length == 1) return MessageUtils.filterSuggestions(names(), args[0]);
        GameType type = GameType.fromName(args[0]);
        if (type == null) return new ArrayList<>();
        return StartGameCommand.suggestions(plugin, type, Arrays.copyOfRange(args, 1, args.length));
    }
}
