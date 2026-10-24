package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.discord.DiscordManager;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /chatgames discord test <message|all>} posts a message from discord.yml with made-up
 * values, to see how it looks; {@code /chatgames discord leaderboard} posts the leaderboard now.
 */
public class DiscordCommand extends SubCommand {

    private static final String USAGE = "/chatgames discord <test <message|all>|leaderboard>";

    private final ChatGames plugin;

    public DiscordCommand(@Nonnull ChatGames plugin) {
        super("discord", PermissionKey.DISCORD.getPermission(), false);
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        String action = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        DiscordManager discord = plugin.getDiscordManager();
        if (action.equals("leaderboard")) {
            plugin.getScheduler().onGlobal(() -> CommandSupport.send(sender,
                    discord.postLeaderboard() ? MessageKey.DISCORD_LEADERBOARD_POSTED : MessageKey.DISCORD_NO_CHANNEL,
                    "%section%", "leaderboard"));
            return;
        }
        if (!action.equals("test") || args.length < 2) {
            CommandSupport.usage(sender, USAGE);
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        List<String> names = name.equals("all") ? DiscordManager.MESSAGES : Collections.singletonList(name);
        Player viewer = sender instanceof Player ? (Player) sender : null;
        plugin.getScheduler().onGlobal(() -> {
            int sent = 0;
            for (String message : names) {
                DiscordManager.TestResult result = discord.test(message, viewer);
                if (result == DiscordManager.TestResult.SENT) {
                    sent++;
                    continue;
                }
                // With "all", a section with no channel is skipped quietly; one asked for by name says why
                if (names.size() > 1 && result == DiscordManager.TestResult.NO_CHANNEL) continue;
                report(sender, result, message);
                if (result == DiscordManager.TestResult.DISABLED) return;
            }
            if (sent > 0) CommandSupport.send(sender, MessageKey.DISCORD_TEST_SENT, "%amount%", String.valueOf(sent));
        });
    }

    private static void report(CommandSender sender, DiscordManager.TestResult result, String message) {
        String section = message.contains(".") ? message.substring(0, message.indexOf('.')) : message;
        switch (result) {
            case DISABLED:
                CommandSupport.send(sender, MessageKey.DISCORD_DISABLED);
                break;
            case UNKNOWN:
                CommandSupport.send(sender, MessageKey.DISCORD_UNKNOWN_MESSAGE, "%message%", message);
                break;
            case NO_CHANNEL:
                CommandSupport.send(sender, MessageKey.DISCORD_NO_CHANNEL, "%section%", section);
                break;
            default:
                CommandSupport.send(sender, MessageKey.DISCORD_EMPTY_MESSAGE, "%message%", message);
        }
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length == 1) return Arrays.asList("test", "leaderboard");
        if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            List<String> names = new ArrayList<>(DiscordManager.MESSAGES);
            names.add("all");
            return names;
        }
        return Collections.emptyList();
    }
}
