package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.utils.management.LogUtils;
import org.nsider.chatgames.utils.messages.Msg;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@code /chatgames logs [amount]}: prints the last entries of the game log file
 * (10 by default), each in its messages.yml {@code logs.entries} format.
 */
public class LogsCommand extends SubCommand {

    private static final int DEFAULT_AMOUNT = 10;
    /** Keeps a typo like "logs 100000" from flooding chat. */
    private static final int MAX_AMOUNT = 200;

    private final ChatGames plugin;

    public LogsCommand(@Nonnull ChatGames plugin) {
        super("logs", PermissionKey.LOGS.getPermission(), false);
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (!ConfigValue.getBoolean(ConfigPaths.LOG_TO_FILE)) {
            CommandSupport.send(sender, MessageKey.CANT_LOG_LOCAL);
            return;
        }
        int amount = DEFAULT_AMOUNT;
        if (args.length > 0) {
            Integer parsed = CommandSupport.parseAmount(sender, args[0]);
            if (parsed == null) return;
            if (parsed == 0) {
                CommandSupport.send(sender, MessageKey.NUMBER_NOT_FOUND);
                return;
            }
            amount = Math.min(parsed, MAX_AMOUNT);
        }

        int requested = amount;
        // Reading the file is disk work: off the main thread
        plugin.getScheduler().async(() -> {
            List<String> logs = plugin.getLogUtils().getLastLogs(requested);
            plugin.getScheduler().global(() -> print(sender, logs, requested));
        });
    }

    private void print(CommandSender sender, List<String> logs, int amount) {
        if (logs.isEmpty()) {
            CommandSupport.send(sender, MessageKey.LOGS_EMPTY);
            return;
        }
        CommandSupport.send(sender, MessageKey.LOGS_HEADER, "%amount%", String.valueOf(logs.size()));
        LogUtils utils = plugin.getLogUtils();
        for (String log : logs) {
            String type = utils.identifyLogType(log);
            String format = ConfigManager.getMessagesConfig().getString(formatPath(type), "");
            if (format.isEmpty()) {
                Msg.of("&7" + log).send(sender);
                continue;
            }
            Msg.of(format)
                    .with("%game%", utils.extractValueFromLog(log, "Game"))
                    .with("%race%", utils.extractValueFromLog(log, "Race"))
                    .with("%hasAnswer%", utils.extractValueFromLog(log, "Has Answer"))
                    .with("%answer%", utils.extractValueFromLog(log, "Answer"))
                    .with("%hasWinner%", utils.extractValueFromLog(log, "Has Winner"))
                    .with("%winners%", utils.extractWinnersFromLog(log))
                    .with("%winner%", utils.extractWinnersFromLog(log))
                    .with("%material%", utils.extractValueFromLog(log, "Material"))
                    .with("%entity%", utils.extractValueFromLog(log, "Entity"))
                    .with("%amount%", utils.extractValueFromLog(log, "Amount"))
                    .send(sender);
        }
    }

    private static String formatPath(String type) {
        switch (type) {
            case "Game Start": return "logs.entries.game.start";
            case "Game End": return "logs.entries.game.end";
            case "Race Start": return "logs.entries.race.start";
            case "Race End": return "logs.entries.race.end";
            case "Hunt Start": return "logs.entries.hunt.start";
            case "Hunt End": return "logs.entries.hunt.end";
            default: return "";
        }
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        return args.length == 1 ? Arrays.asList("10", "25", "50", "100") : Collections.<String>emptyList();
    }
}
