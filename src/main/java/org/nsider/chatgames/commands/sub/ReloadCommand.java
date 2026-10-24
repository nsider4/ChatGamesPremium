package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;

/**
 * {@code /chatgames reload}: reloads every configuration file without a restart.
 */
public class ReloadCommand extends SubCommand {

    private final ChatGames plugin;

    public ReloadCommand(@Nonnull ChatGames plugin) {
        super("reload", PermissionKey.RELOAD.getPermission(), false);
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        // Reloading restarts the global timers and touches the running game: main/global thread
        plugin.getScheduler().onGlobal(() -> MessageUtils.sendMessage(sender, ConfigManager.getMessages().get(
                plugin.reloadAll() ? MessageKey.CONFIG_RELOADED : MessageKey.CONFIG_RELOAD_FAILED)));
    }
}
