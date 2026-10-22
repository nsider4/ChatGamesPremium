package org.nsider.chatgames.reward;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.globalgames.GlobalGameManager;
import org.nsider.chatgames.utils.gameplay.CommandUtils;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads winners' rewards from rewards.yml (see {@link RewardSelector} for how they are
 * picked) and runs reward commands.
 *
 * <p>Chat games keep rewards per place: {@code <game>.rewards.<place>.<reward>}, falling
 * back to place 1. Races list reward names in races.yml, which point into
 * {@code races.rewards.<reward>}.</p>
 */
public class RewardManager {

    private static final String RACE_REWARDS = "races.rewards";

    private final ChatGames plugin;

    public RewardManager(ChatGames instance) {
        this.plugin = instance;
    }

    /**
     * @param game  the game's config key
     * @param place the winner's place, 1 for first
     */
    @Nonnull
    public List<String> chatGameRewards(@Nonnull String game, @Nonnull Player player, int place) {
        String section = game + ".rewards." + place;
        if (!ConfigManager.getRewards().isSet(section)) section = game + ".rewards.1";
        return select(game, ConfigManager.getRewards().getConfigurationSection(section), null, player);
    }

    /**
     * @param names the reward names a race lists in races.yml
     */
    @Nonnull
    public List<String> raceRewards(@Nonnull String race, @Nonnull Player player, @Nonnull List<String> names) {
        if (names.isEmpty()) return Collections.emptyList();
        return select(race, ConfigManager.getRewards().getConfigurationSection(RACE_REWARDS), names, player);
    }

    /**
     * @param keys the reward sections under {@code rewards}, e.g. {@code completed}
     * @return their rewards, from global_games.yml
     */
    @Nonnull
    public List<String> globalRewards(@Nonnull GameType type, @Nonnull Player player, @Nonnull List<String> keys) {
        List<String> commands = new ArrayList<>();
        for (ConfigurationSection section : GlobalGameManager.rewardSections(type, keys)) {
            commands.addAll(select("global " + type.getConfigKey(), section, null, player));
        }
        return commands;
    }

    /**
     * @param lines reward lines, as in a reward's {@code data}
     */
    @Nonnull
    public List<String> lineRewards(@Nonnull String what, @Nonnull Player player, @Nonnull List<String> lines) {
        return RewardSelector.fromLines(lines, player::hasPermission, plugin.getRandom(),
                line -> plugin.getLogger().severe("Invalid chance in a " + what + " reward: " + line));
    }

    private List<String> select(String game, @Nullable ConfigurationSection rewards, @Nullable List<String> only, Player player) {
        if (rewards == null) return Collections.emptyList();
        int online = plugin.getGameManager().getGamePlayerManager().getOnlinePlayers();
        return RewardSelector.select(rewards, only, online, player::hasPermission, plugin.getRandom(),
                line -> plugin.getLogger().severe("Invalid chance in a " + game + " reward: " + line));
    }

    /**
     * Runs one reward command for the winner. Folia runs console commands only on the global
     * thread and a player's commands only on that player's thread, so each goes to its own;
     * outside Folia both are the main thread and run straight away.
     */
    public void executeCommand(@Nonnull String command, @Nonnull Player player) {
        String cmd = command.replace("%player%", player.getName());

        if (cmd.startsWith("[playercmd]")) {
            plugin.getScheduler().onPlayer(player, () -> Bukkit.dispatchCommand(player, strip(cmd, "[playercmd]")));
        } else if (cmd.startsWith("[consolecmd]")) {
            plugin.getScheduler().onGlobal(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), strip(cmd, "[consolecmd]")));
        } else if (cmd.startsWith("[playermsg]")) {
            player.sendMessage(MessageUtils.color(strip(cmd, "[playermsg]")));
        } else if (cmd.startsWith("[broadcast]")) {
            String message = MessageUtils.color(strip(cmd, "[broadcast]"));
            Bukkit.getOnlinePlayers().stream()
                    .filter(plugin.getGameManager()::worldEnabled)
                    .forEach(p -> p.sendMessage(message));
        } else if (cmd.startsWith("[player_as_op]")) {
            plugin.getScheduler().onPlayer(player, () -> CommandUtils.executeAsOp(player, strip(cmd, "[player_as_op]")));
        } else {
            plugin.getLogger().warning("Unknown reward action (expected [consolecmd], [playercmd], ...): " + command);
        }
    }

    private static String strip(String command, String action) {
        return command.substring(action.length()).trim();
    }
}
