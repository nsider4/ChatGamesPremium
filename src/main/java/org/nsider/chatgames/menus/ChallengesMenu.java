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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily and weekly challenges, one tab each. A finished challenge is claimed by clicking it.
 */
public final class ChallengesMenu {

    private final ChatGames plugin;
    private final Menus menus;

    ChallengesMenu(ChatGames plugin, Menus menus) {
        this.plugin = plugin;
        this.menus = menus;
    }

    public void open(@Nonnull Player player, @Nonnull Period period) {
        open(player, period, 0);
    }

    private void open(Player player, Period period, int page) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data == null) {
            CommandSupport.send(player, MessageKey.DATA_LOADING);
            return;
        }
        ProgressionManager progression = plugin.getProgressionManager();
        ChallengeConfig config = progression.getChallenges();
        LocalDate today = progression.today();
        List<Challenge> active = config.active(period, today);

        MenuBuilder menu = MenuBuilder.of(plugin, "challenges", player)
                .with("%period%", ProgressionManager.periodName(period))
                .with("%completed%", ChallengeTracker.completed(data, config, period, today))
                .with("%total%", active.size())
                .with("%resets%", ChallengesCommand.timeLeft(config, period, today));

        menu.item("tab-daily", period == Period.DAILY, () -> open(player, Period.DAILY));
        menu.item("tab-weekly", period == Period.WEEKLY, () -> open(player, Period.WEEKLY));
        menu.item("info", null);
        if (active.isEmpty()) menu.item("none", null);

        menu.paged(active, page, (slot, challenge) -> {
            long progress = ChallengeTracker.progress(data, config, challenge, today);
            boolean claimed = ChallengeTracker.isClaimed(data, config, challenge, today);
            boolean ready = !claimed && progress >= challenge.getAmount();
            ConfigurationSection template = menu.template(claimed ? "claimed" : ready ? "ready" : "in-progress");
            menu.place(slot, template, entry(menu, challenge, progress), ready ? () -> {
                plugin.getProgressionManager().claimChallenge(player, challenge);
                open(player, period, page);
            } : null);
        }, other -> open(player, period, other));

        menu.item("claim-all", () -> {
            player.performCommand("chatgames claim");
            open(player, period, page);
        });
        menu.item("back", () -> menus.getMain().open(player));
        menu.item("close", player::closeInventory);
        menu.open();
    }

    private static Map<String, String> entry(MenuBuilder menu, Challenge challenge, long progress) {
        Map<String, String> values = new HashMap<>();
        values.put("%challenge%", challenge.getName());
        values.put("%description%", challenge.getDescription());
        values.put("%progress%", String.valueOf(progress));
        values.put("%amount%", String.valueOf(challenge.getAmount()));
        values.put("%percent%", String.valueOf(progress * 100 / Math.max(1, challenge.getAmount())));
        values.put("%bar%", bar(menu, progress, challenge.getAmount()));
        values.put("%icon%", challenge.getIcon());
        return values;
    }

    /** @return a progress bar from menus.yml {@code progress-bar} */
    @Nonnull
    static String bar(@Nonnull MenuBuilder menu, long progress, long amount) {
        ConfigurationSection section = menu.getSection().getConfigurationSection("progress-bar");
        int length = section == null ? 10 : Math.max(1, section.getInt("length", 10));
        String done = section == null ? "&a|" : section.getString("done", "&a|");
        String left = section == null ? "&7|" : section.getString("left", "&7|");
        int filled = (int) Math.min(length, progress * length / Math.max(1, amount));
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < length; i++) bar.append(i < filled ? done : left);
        return MessageUtils.color(bar.toString());
    }
}
