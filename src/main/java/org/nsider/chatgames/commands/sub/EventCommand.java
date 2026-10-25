package org.nsider.chatgames.commands.sub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.gameevents.EventDefinition;
import org.nsider.chatgames.gameevents.EventManager;
import org.nsider.chatgames.gameevents.EventSchedule;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /chatgames event [list]} lists the events, {@code /chatgames event top <event>}
 * shows who scored most in one over time, and {@code /chatgames event start <event>} /
 * {@code stop} run one by hand.
 */
public class EventCommand extends SubCommand {

    private static final int TOP_SIZE = 10;
    private static final DateTimeFormatter NEXT = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.ENGLISH);

    private final ChatGames plugin;

    @Override
    public boolean canUse(@Nonnull CommandSender sender, @Nonnull String action) {
        if (action.equals("start") || action.equals("stop")) return sender.hasPermission(PermissionKey.EVENT_MANAGE.getPermission());
        return canUse(sender);
    }

    public EventCommand(@Nonnull ChatGames plugin) {
        super("event", PermissionKey.EVENT.getPermission(), false, "events");
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        String action = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "list":
                list(sender);
                return;
            case "top":
                if (args.length != 2) {
                    CommandSupport.usage(sender, "/chatgames event top <event>");
                    return;
                }
                top(sender, args[1]);
                return;
            case "start":
            case "stop":
                if (!sender.hasPermission(PermissionKey.EVENT_MANAGE.getPermission())) {
                    CommandSupport.send(sender, MessageKey.NOT_ENOUGH_PERMISSIONS);
                    return;
                }
                if (action.equals("start") && args.length != 2) {
                    CommandSupport.usage(sender, "/chatgames event start <event>");
                    return;
                }
                plugin.getScheduler().onGlobal(() -> {
                    if (action.equals("start")) start(sender, args[1]);
                    else stop(sender);
                });
                return;
            default:
                CommandSupport.usage(sender, "/chatgames event [list|top <event>|start <event>|stop]");
        }
    }

    private void list(CommandSender sender) {
        EventManager events = plugin.getEventManager();
        if (events.getConfig().getAll().isEmpty()) {
            CommandSupport.send(sender, MessageKey.EVENTS_LIST_EMPTY);
            return;
        }
        EventManager.Run running = events.getRunning();
        ZonedDateTime now = ZonedDateTime.now(events.zone());
        CommandSupport.send(sender, MessageKey.EVENTS_LIST_HEADER);
        for (EventDefinition definition : events.getConfig().getAll()) {
            String status = running != null && running.getDefinition() == definition
                    ? CommandSupport.message(MessageKey.EVENTS_LIST_RUNNING, "%step%", String.valueOf(running.getStepNumber()),
                    "%steps%", String.valueOf(definition.getSteps().size()))
                    : "";
            CommandSupport.send(sender, MessageKey.EVENTS_LIST_ENTRY,
                    "%id%", definition.getId(),
                    "%event%", definition.getName(),
                    "%steps%", String.valueOf(definition.getSteps().size()),
                    "%next%", next(definition, now),
                    "%running%", status);
        }
    }

    private static String next(EventDefinition definition, ZonedDateTime now) {
        ZonedDateTime next = null;
        for (EventSchedule schedule : definition.getSchedule()) {
            ZonedDateTime candidate = schedule.next(now);
            if (next == null || candidate.isBefore(next)) next = candidate;
        }
        return next == null ? CommandSupport.message(MessageKey.EVENTS_NOT_SCHEDULED) : NEXT.format(next);
    }

    private void top(CommandSender sender, String id) {
        EventDefinition definition = find(sender, id);
        if (definition == null) return;
        ChatGamesPlayerManager.topStat(EventManager.stat(definition, "points"), TOP_SIZE, rows -> {
            // Names may need the disk; this is the player-data thread, never the main one
            List<String> names = new ArrayList<>();
            for (Map.Entry<UUID, Long> row : rows) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(row.getKey());
                names.add(player.getName() != null ? player.getName() : row.getKey().toString().substring(0, 8));
            }
            plugin.getScheduler().onGlobal(() -> {
                if (rows.isEmpty()) {
                    CommandSupport.send(sender, MessageKey.EVENTS_TOP_EMPTY, "%event%", definition.getName());
                    return;
                }
                CommandSupport.send(sender, MessageKey.EVENTS_TOP_HEADER, "%event%", definition.getName());
                for (int i = 0; i < rows.size(); i++) {
                    CommandSupport.send(sender, MessageKey.EVENTS_TOP_ENTRY, "%spot%", String.valueOf(i + 1),
                            "%player%", names.get(i), "%points%", String.valueOf(rows.get(i).getValue()));
                }
            });
        });
    }

    private void start(CommandSender sender, String id) {
        EventManager events = plugin.getEventManager();
        switch (events.start(id, false)) {
            case STARTED:
                CommandSupport.send(sender, MessageKey.EVENTS_STARTED, "%event%", events.getConfig().get(id).getName());
                break;
            case UNKNOWN:
                CommandSupport.send(sender, MessageKey.EVENTS_UNKNOWN, "%event%", id);
                break;
            case ALREADY_RUNNING:
                EventManager.Run running = events.getRunning();
                CommandSupport.send(sender, MessageKey.EVENTS_ALREADY_RUNNING,
                        "%event%", running == null ? id : running.getDefinition().getName());
                break;
            case CANCELLED:
                CommandSupport.send(sender, MessageKey.EVENTS_CANCELLED, "%event%", events.getConfig().get(id).getName());
                break;
            default:
                break;
        }
    }

    private void stop(CommandSender sender) {
        EventDefinition stopped = plugin.getEventManager().stop();
        if (stopped == null) CommandSupport.send(sender, MessageKey.EVENTS_NOT_RUNNING);
        else CommandSupport.send(sender, MessageKey.EVENTS_STOPPED, "%event%", stopped.getName());
    }

    @Nullable
    private EventDefinition find(CommandSender sender, String id) {
        EventDefinition definition = plugin.getEventManager().getConfig().get(id);
        if (definition == null) CommandSupport.send(sender, MessageKey.EVENTS_UNKNOWN, "%event%", id);
        return definition;
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        boolean manage = sender.hasPermission(PermissionKey.EVENT_MANAGE.getPermission());
        if (args.length == 1) {
            return MessageUtils.filterSuggestions(manage ? Arrays.asList("list", "top", "start", "stop")
                    : Arrays.asList("list", "top"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("top") || (manage && args[0].equalsIgnoreCase("start")))) {
            List<String> ids = new ArrayList<>();
            for (EventDefinition definition : plugin.getEventManager().getConfig().getAll()) ids.add(definition.getId());
            return MessageUtils.filterSuggestions(ids, args[1]);
        }
        return Collections.emptyList();
    }
}
