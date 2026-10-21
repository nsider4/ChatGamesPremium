package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.progression.ProgressionManager;
import org.nsider.chatgames.progression.StreakConfig;
import org.nsider.chatgames.progression.StreakTracker;
import org.nsider.chatgames.progression.StreakType;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code /chatgames streaks}: your three streaks. {@code /chatgames streak set <player>
 * <type> <days>} sets one by hand.
 */
public class StreaksCommand extends SubCommand {

    private final ChatGames plugin;

    public StreaksCommand(@Nonnull ChatGames plugin) {
        super("streaks", PermissionKey.STREAKS.getPermission(), false, "streak");
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("set")) {
            set(sender, args);
            return;
        }
        if (!(sender instanceof Player)) {
            CommandSupport.usage(sender, "/chatgames streak set <player> <challenge|daily|global> <days>");
            return;
        }
        ChatGamesPlayer data = ChatGamesPlayerManager.get(((Player) sender).getUniqueId());
        if (data == null) {
            CommandSupport.send(sender, MessageKey.DATA_LOADING);
            return;
        }

        ProgressionManager progression = plugin.getProgressionManager();
        StreakConfig streaks = progression.getStreaks();
        LocalDate today = progression.today();
        boolean any = false;
        for (StreakType type : StreakType.values()) {
            if (!streaks.isEnabled(type)) continue;
            if (!any) CommandSupport.send(sender, MessageKey.STREAKS_HEADER);
            any = true;
            int unclaimed = StreakTracker.unclaimed(data, type, today);
            String status = unclaimed > 0
                    ? CommandSupport.message(MessageKey.STREAK_STATUS_READY, "%unclaimed%", String.valueOf(unclaimed))
                    : CommandSupport.message(StreakTracker.countedToday(data, type, today)
                    ? MessageKey.STREAK_STATUS_TODAY : MessageKey.STREAK_STATUS_WAITING);
            CommandSupport.send(sender, MessageKey.STREAKS_ENTRY,
                    "%streak%", streaks.getName(type),
                    "%days%", String.valueOf(StreakTracker.current(data, type, today)),
                    "%longest%", String.valueOf(StreakTracker.longest(data, type)),
                    "%status%", status);
        }
        if (!any) CommandSupport.send(sender, MessageKey.STREAKS_NONE);
    }

    private void set(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionKey.STREAKS_SET.getPermission())) {
            CommandSupport.send(sender, MessageKey.NOT_ENOUGH_PERMISSIONS);
            return;
        }
        if (args.length != 4) {
            CommandSupport.usage(sender, "/chatgames streak set <player> <challenge|daily|global> <days>");
            return;
        }
        StreakType type = StreakType.fromName(args[2]);
        if (type == null) {
            CommandSupport.send(sender, MessageKey.STREAK_UNKNOWN, "%type%", args[2]);
            return;
        }
        Integer days = CommandSupport.parseAmount(sender, args[3]);
        if (days == null) return;

        ProgressionManager progression = plugin.getProgressionManager();
        CommandSupport.findPlayer(plugin, sender, args[1], target -> {
            LocalDate today = progression.today();
            ChatGamesPlayerManager.modify(target.getUniqueId(), data -> StreakTracker.set(data, type, days, today));
            CommandSupport.send(sender, MessageKey.STREAK_SET, "%player%", CommandSupport.nameOf(target, args[1]),
                    "%streak%", progression.getStreaks().getName(type), "%days%", String.valueOf(days));
        });
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (!sender.hasPermission(PermissionKey.STREAKS_SET.getPermission())) return Collections.emptyList();
        if (args.length == 1) return MessageUtils.filterSuggestions(Collections.singletonList("set"), args[0]);
        if (!args[0].equalsIgnoreCase("set")) return Collections.emptyList();
        if (args.length == 2) return CommandSupport.onlinePlayerNames(args[1]);
        if (args.length == 3) {
            List<String> types = new ArrayList<>();
            for (StreakType type : StreakType.values()) types.add(type.getKey());
            return MessageUtils.filterSuggestions(types, args[2]);
        }
        return Collections.emptyList();
    }
}
