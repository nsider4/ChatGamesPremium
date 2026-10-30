package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.ConfigLoadError;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.Messages;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.List;

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
        plugin.getScheduler().onGlobal(() -> {
            List<ConfigLoadError> errors = plugin.reloadAll();
            Messages messages = ConfigManager.getMessages();
            if (errors.isEmpty()) {
                MessageUtils.sendMessage(sender, messages.get(MessageKey.CONFIG_RELOADED));
                return;
            }
            MessageUtils.sendMessage(sender, messages.get(MessageKey.CONFIG_RELOAD_FAILED));
            // Staff in game can't see the console, so each broken file and its line is named here
            for (ConfigLoadError error : errors) {
                MessageUtils.sendMessage(sender, error.hasPosition()
                        ? messages.get(MessageKey.CONFIG_RELOAD_FAILED_FILE, "%file%", error.getFile(),
                                "%line%", String.valueOf(error.getLine()), "%column%", String.valueOf(error.getColumn()))
                        : messages.get(MessageKey.CONFIG_RELOAD_FAILED_UNREADABLE, "%file%", error.getFile()));
            }
        });
    }
}
