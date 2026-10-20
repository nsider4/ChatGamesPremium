package org.nsider.chatgames.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.sub.ReloadCommand;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /chatgames}: routes the first argument to a {@link SubCommand}.
 */
public class ChatGamesCommand implements TabExecutor {

    /** Sub-commands by name and alias, in registration order for tab completion. */
    private final ChatGames plugin;
    private final Map<String, SubCommand> commands = new LinkedHashMap<>();
    private final List<SubCommand> registered = new ArrayList<>();

    public ChatGamesCommand(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
        register(new ReloadCommand(plugin));
    }

    /**
     * Makes this the executor and tab completer of {@code /chatgames}.
     */
    public void bind(@Nonnull PluginCommand command) {
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    private void register(SubCommand command) {
        registered.add(command);
        commands.put(command.getName(), command);
        for (String alias : command.getAliases()) {
            commands.put(alias.toLowerCase(Locale.ROOT), command);
        }
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
        if (args.length == 0) {
            // `help` arrives in step 6; until then an empty command is unknown
            send(sender, MessageKey.UNKNOWN_COMMAND);
            return true;
        }

        SubCommand sub = commands.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            // Clicking the Clickable game's text runs /chatgames <key>
            if (args.length == 1 && sender instanceof Player
                    && plugin.getGameManager().getAnswerHandler().onClick((Player) sender, args[0])) {
                return true;
            }
            send(sender, MessageKey.UNKNOWN_COMMAND);
            return true;
        }
        if (!sub.canUse(sender)) {
            send(sender, MessageKey.NOT_ENOUGH_PERMISSIONS);
            return true;
        }
        if (sub.isPlayerOnly() && !(sender instanceof Player)) {
            send(sender, MessageKey.PLAYER_ONLY_COMMAND);
            return true;
        }

        sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String alias, @Nonnull String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (SubCommand sub : registered) {
                if (sub.canUse(sender)) names.add(sub.getName());
            }
            return MessageUtils.filterSuggestions(names, args[0]);
        }

        SubCommand sub = commands.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null || !sub.canUse(sender)) {
            return Collections.emptyList();
        }
        return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private static void send(CommandSender sender, MessageKey key) {
        MessageUtils.sendMessage(sender, ConfigManager.getMessages().get(key));
    }
}
