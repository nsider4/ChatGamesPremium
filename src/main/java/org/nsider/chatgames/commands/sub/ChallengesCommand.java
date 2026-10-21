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
import org.nsider.chatgames.progression.Challenge;
import org.nsider.chatgames.progression.ChallengeConfig;
import org.nsider.chatgames.progression.ChallengeTracker;
import org.nsider.chatgames.progression.Period;
import org.nsider.chatgames.progression.ProgressionManager;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

/**
 * {@code /chatgames challenges}: today's and this week's challenges with your progress.
 * {@code /chatgames challenges reset <player>} clears a player's progress.
 */
public class ChallengesCommand extends SubCommand {

    private final ChatGames plugin;

    public ChallengesCommand(@Nonnull ChatGames plugin) {
        super("challenges", PermissionKey.CHALLENGES.getPermission(), false, "challenge");
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            reset(sender, args);
            return;
        }
        if (!(sender instanceof Player)) {
            CommandSupport.usage(sender, "/chatgames challenges reset <player>");
            return;
        }
        Player player = (Player) sender;
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data == null) {
            CommandSupport.send(sender, MessageKey.DATA_LOADING);
            return;
        }

        ProgressionManager progression = plugin.getProgressionManager();
        ChallengeConfig config = progression.getChallenges();
        LocalDate today = progression.today();
        boolean any = false;
        for (Period period : Period.values()) {
            List<Challenge> active = config.active(period, today);
            if (active.isEmpty()) continue;
            any = true;
            CommandSupport.send(sender, MessageKey.CHALLENGES_HEADER,
                    "%period%", ProgressionManager.periodName(period),
                    "%completed%", String.valueOf(ChallengeTracker.completed(data, config, period, today)),
                    "%total%", String.valueOf(active.size()),
                    "%resets%", timeLeft(config, period, today));
            for (Challenge challenge : active) {
                long progress = ChallengeTracker.progress(data, config, challenge, today);
                MessageKey status = ChallengeTracker.isClaimed(data, config, challenge, today) ? MessageKey.CHALLENGE_STATUS_CLAIMED
                        : progress >= challenge.getAmount() ? MessageKey.CHALLENGE_STATUS_READY : MessageKey.CHALLENGE_STATUS_PROGRESS;
                CommandSupport.send(sender, MessageKey.CHALLENGES_ENTRY,
                        "%challenge%", challenge.getName(),
                        "%description%", challenge.getDescription(),
                        "%progress%", String.valueOf(progress),
                        "%amount%", String.valueOf(challenge.getAmount()),
                        "%status%", CommandSupport.message(status));
            }
        }
        if (!any) CommandSupport.send(sender, MessageKey.CHALLENGES_NONE);
    }

    private void reset(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionKey.CHALLENGES_RESET.getPermission())) {
            CommandSupport.send(sender, MessageKey.NOT_ENOUGH_PERMISSIONS);
            return;
        }
        if (args.length != 2) {
            CommandSupport.usage(sender, "/chatgames challenges reset <player>");
            return;
        }
        CommandSupport.findPlayer(plugin, sender, args[1], target -> {
            ChatGamesPlayerManager.modify(target.getUniqueId(), ChallengeTracker::reset);
            CommandSupport.send(sender, MessageKey.CHALLENGES_RESET, "%player%", CommandSupport.nameOf(target, args[1]));
        });
    }

    /** @return the time until the period ends, e.g. "2d 5h", "5h 12m" or "40m" */
    @Nonnull
    public static String timeLeft(@Nonnull ChallengeConfig config, @Nonnull Period period, @Nonnull LocalDate today) {
        ZonedDateTime end = config.lastDay(period, today).plusDays(1).atStartOfDay(config.getZone());
        long minutes = Math.max(1, Duration.between(ZonedDateTime.now(config.getZone()), end).toMinutes());
        long days = minutes / (24 * 60);
        long hours = minutes / 60 % 24;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes % 60 + "m";
        return minutes + "m";
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (!sender.hasPermission(PermissionKey.CHALLENGES_RESET.getPermission())) return Collections.emptyList();
        if (args.length == 1) return MessageUtils.filterSuggestions(Collections.singletonList("reset"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) return CommandSupport.onlinePlayerNames(args[1]);
        return Collections.emptyList();
    }
}
