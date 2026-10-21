package org.nsider.chatgames.menus;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.sub.ChallengesCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.progression.Challenge;
import org.nsider.chatgames.progression.ChallengeConfig;
import org.nsider.chatgames.progression.ChallengeTracker;
import org.nsider.chatgames.progression.Period;
import org.nsider.chatgames.progression.ProgressionManager;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.menu.Menu;
import org.nsider.inventorymanager.menu.MenuManager;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily and weekly challenges ({@code challenges [daily|weekly]}). Each one is an entry
 * drawn with the {@code in-progress}, {@code ready} or {@code claimed} template; a ready
 * one is claimed with {@code [chatgames:claim-challenge] <period> <id>}.
 */
final class ChallengesMenu {

    private final ChatGames plugin;

    ChallengesMenu(ChatGames plugin) {
        this.plugin = plugin;
    }

    void register(@Nonnull MenuManager manager) {
        manager.registerMenu("challenges", this::build);
        manager.registerAction("chatgames:claim-challenge", this::claim);
    }

    private void build(Menu menu) {
        Player player = menu.getPlayer();
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data == null) {
            CommandSupport.send(player, MessageKey.DATA_LOADING);
            menu.cancel();
            return;
        }
        ProgressionManager progression = plugin.getProgressionManager();
        ChallengeConfig config = progression.getChallenges();
        LocalDate today = progression.today();
        Period period = period(menu.arg(0, Period.DAILY.getKey()));
        List<Challenge> active = config.active(period, today);

        menu.set("%period%", ProgressionManager.periodName(period))
                .set("%period_key%", period.getKey())
                .set("%completed%", ChallengeTracker.completed(data, config, period, today))
                .set("%total%", active.size())
                .set("%resets%", ChallengesCommand.timeLeft(config, period, today));

        for (Challenge challenge : active) {
            long progress = ChallengeTracker.progress(data, config, challenge, today);
            boolean claimed = ChallengeTracker.isClaimed(data, config, challenge, today);
            boolean ready = !claimed && progress >= challenge.getAmount();
            Map<String, Object> values = new HashMap<>();
            values.put("%challenge%", challenge.getName());
            values.put("%id%", challenge.getId());
            values.put("%description%", challenge.getDescription());
            values.put("%progress%", progress);
            values.put("%amount%", challenge.getAmount());
            values.put("%percent%", progress * 100 / Math.max(1, challenge.getAmount()));
            values.put("%bar%", bar(menu.getSection().getConfigurationSection("progress-bar"), progress, challenge.getAmount()));
            values.put("%icon%", challenge.getIcon());
            menu.addEntry(claimed ? "claimed" : ready ? "ready" : "in-progress", values);
        }
    }

    /** {@code [chatgames:claim-challenge] <period> <id>} */
    private void claim(Menu menu, String argument) {
        String[] parts = argument.trim().split("\\s+", 2);
        if (parts.length < 2) return;
        ProgressionManager progression = plugin.getProgressionManager();
        for (Challenge challenge : progression.getChallenges().active(period(parts[0]), progression.today())) {
            if (challenge.getId().equals(parts[1])) {
                if (!progression.claimChallenge(menu.getPlayer(), challenge)) {
                    CommandSupport.send(menu.getPlayer(), MessageKey.NOTHING_TO_CLAIM);
                }
                return;
            }
        }
        // The challenges changed (a reset or reload) since the menu opened
        CommandSupport.send(menu.getPlayer(), MessageKey.NOTHING_TO_CLAIM);
    }

    @Nonnull
    static Period period(@Nonnull String key) {
        for (Period period : Period.values()) {
            if (period.getKey().equalsIgnoreCase(key)) return period;
        }
        return Period.DAILY;
    }

    /** @return a progress bar drawn with the menu's {@code progress-bar} settings */
    @Nonnull
    static String bar(ConfigurationSection section, long progress, long amount) {
        int length = section == null ? 10 : Math.max(1, section.getInt("length", 10));
        String done = section == null ? "&a|" : section.getString("done", "&a|");
        String left = section == null ? "&7|" : section.getString("left", "&7|");
        int filled = (int) Math.min(length, progress * length / Math.max(1, amount));
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < length; i++) bar.append(i < filled ? done : left);
        return MessageUtils.color(bar.toString());
    }
}
