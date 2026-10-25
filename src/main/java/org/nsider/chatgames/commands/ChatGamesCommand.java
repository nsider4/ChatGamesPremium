package org.nsider.chatgames.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.sub.AnyGameCommand;
import org.nsider.chatgames.commands.sub.ChallengesCommand;
import org.nsider.chatgames.commands.sub.ClaimCommand;
import org.nsider.chatgames.commands.sub.DebugCommand;
import org.nsider.chatgames.commands.sub.MenuCommand;
import org.nsider.chatgames.commands.sub.ProfileCommand;
import org.nsider.chatgames.commands.sub.DiscordCommand;
import org.nsider.chatgames.commands.sub.EventCommand;
import org.nsider.chatgames.commands.sub.GlobalCommand;
import org.nsider.chatgames.commands.sub.HelpCommand;
import org.nsider.chatgames.commands.sub.LogsCommand;
import org.nsider.chatgames.commands.sub.ManagePointsCommand;
import org.nsider.chatgames.commands.sub.PointsCommand;
import org.nsider.chatgames.commands.sub.PresetCommand;
import org.nsider.chatgames.commands.sub.ReloadCommand;
import org.nsider.chatgames.commands.sub.RemoveCommand;
import org.nsider.chatgames.commands.sub.StartGameCommand;
import org.nsider.chatgames.commands.sub.StopCommand;
import org.nsider.chatgames.commands.sub.StreaksCommand;
import org.nsider.chatgames.commands.sub.ToggleAllCommand;
import org.nsider.chatgames.commands.sub.ToggleCommand;
import org.nsider.chatgames.commands.sub.TopCommand;
import org.nsider.chatgames.api.GameType;
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
 * {@code /chatgames}: routes the first argument to a {@link SubCommand}, ignoring case.
 * Plain {@code /chatgames} shows the help.
 */
public class ChatGamesCommand implements TabExecutor {

    private final ChatGames plugin;
    /** Sub-commands by lower-case name and alias. */
    private final Map<String, SubCommand> commands = new LinkedHashMap<>();
    /** In registration order, for tab completion. */
    private final List<SubCommand> registered = new ArrayList<>();

    public ChatGamesCommand(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
        register(new HelpCommand(this::helpAccess));
        register(MenuCommand.main(plugin));
        register(new ProfileCommand(plugin));
        register(TopCommand.points());
        register(TopCommand.fastest());
        register(new PointsCommand(plugin));
        register(new ChallengesCommand(plugin));
        register(new StreaksCommand(plugin));
        register(new ClaimCommand(plugin));
        register(new EventCommand(plugin));
        register(ToggleCommand.games());
        register(ToggleCommand.sound());
        register(new AnyGameCommand(plugin));
        register(new GlobalCommand(plugin));
        register(new StopCommand(plugin));
        register(new ToggleAllCommand(plugin));
        register(new PresetCommand(plugin));
        register(new LogsCommand(plugin));
        register(new DiscordCommand(plugin));
        register(new RemoveCommand(plugin));
        for (ManagePointsCommand command : ManagePointsCommand.all(plugin)) {
            register(command);
        }
        register(MenuCommand.admin(plugin));
        register(new DebugCommand(plugin));
        register(new ReloadCommand(plugin));
        for (GameType type : GameType.values()) {
            register(new StartGameCommand(plugin, type));
        }
    }

    /**
     * Makes this the executor and tab completer of {@code /chatgames}.
     */
    public void bind(@Nonnull PluginCommand command) {
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    /** Adds {@code /chatgames <id>} for a game another plugin added. */
    public void addGame(@Nonnull GameType type) {
        register(new StartGameCommand(plugin, type));
    }

    public void removeGame(@Nonnull GameType type) {
        SubCommand command = commands.get(type.getConfigKey());
        if (!(command instanceof StartGameCommand)) return;
        commands.remove(type.getConfigKey());
        registered.remove(command);
    }

    /** What a sender can use, for the help list. "(game)" there stands for any start command. */
    private HelpFilter.Access helpAccess(CommandSender sender) {
        return (name, action) -> {
            if (name.equals("(game)") || name.equals("<game>")) {
                for (GameType type : GameType.values()) {
                    if (CommandSupport.canStart(sender, type)) return true;
                }
                return false;
            }
            SubCommand sub = commands.get(name);
            return sub == null ? null : sub.canUse(sender) && sub.canUse(sender, action);
        };
    }

    /** @return true if a sub-command (or alias) already uses this name */
    public boolean isTaken(@Nonnull String name) {
        return commands.containsKey(name.toLowerCase(Locale.ROOT));
    }

    private void register(SubCommand command) {
        registered.add(command);
        commands.put(command.getName().toLowerCase(Locale.ROOT), command);
        for (String alias : command.getAliases()) {
            commands.put(alias.toLowerCase(Locale.ROOT), command);
        }
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
        if (args.length == 0) {
            args = new String[]{"help"};
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
