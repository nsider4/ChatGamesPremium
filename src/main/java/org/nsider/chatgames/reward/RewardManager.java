package org.nsider.chatgames.reward;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.utils.messages.MessageUtils;

public class RewardManager extends RandomReward {
    private final ChatGames plugin;

    public RewardManager(ChatGames instance) {
        this.plugin = instance;
    }

    @SafeVarargs
    public final List<String> loadGameRewards(String game, Player player, int spot, List<String>... list) {
        String section = getSectionPath(game, spot);
        List<String> commands = (list.length == 1) ? getRewards(game, section, player, list[0]) : getRewards(game, section, player);
        commands.removeIf(String::isEmpty);
        return commands;
    }

    private String getSectionPath(String game, int spot) {
        String section = game + ".rewards." + spot;
        return ConfigManager.getRewards().isSet(section) ? section : game + ".rewards.1";
    }

    @SafeVarargs
    private final List<String> getRewards(String game, String section, Player player, List<String>... list) {
        ConfigurationSection rewardsSection = ConfigManager.getRewards().getConfigurationSection(section);
        if (rewardsSection == null) return new ArrayList<>();

        List<Reward> rewards = (list.length == 1) ? convertToRewards(list[0], rewardsSection) : getRewardList(rewardsSection);
        List<String> chosenRewards = selectEligibleRewards(rewards, player);

        if (!isEmptyReward()) {
            chosenRewards.add(getRandomReward());
            resetRewards();
        }

        return processCommands(chosenRewards, player, game);
    }

    private List<Reward> getRewardList(ConfigurationSection rewardsSection) {
        return rewardsSection.getKeys(false).stream()
                .map(rewardsSection::getConfigurationSection)
                .map(Reward::fromConfig)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Reward> convertToRewards(List<String> rewardNames, ConfigurationSection rewardsSection) {
        return rewardNames.stream()
                .map(rewardsSection::getConfigurationSection)
                .map(Reward::fromConfig)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> selectEligibleRewards(List<Reward> rewards, Player player) {
        int onlinePlayers = plugin.getGameManager().getGamePlayerManager().getOnlinePlayers();
        return rewards.stream()
                .filter(reward -> reward.isEligible(onlinePlayers))
                .map(Reward::getName)
                .collect(Collectors.toList());
    }

    private List<String> processCommands(List<String> chosenRewards, Player player, String game) {
        List<String> commands = new ArrayList<>();

        chosenRewards.stream()
                .map(rewardName -> ConfigManager.getRewards().getConfigurationSection(game + ".rewards." + rewardName))
                .map(Reward::fromConfig)
                .filter(Objects::nonNull)
                .map(Reward::getCommands)
                .flatMap(List::stream)
                .forEach(command -> processCommand(command, player, commands, game));

        if (!isEmpty()) {
            commands.add(getRandomValue());
            resetValues();
        }
        return commands;
    }

    private void processCommand(String command, Player player, List<String> commands, String game) {
        if (command.contains("has:")) {
            int startIndex = command.indexOf("has:") + 4;
            int endIndex = command.indexOf(" ", startIndex);
            String permission = (endIndex == -1) ? command.substring(startIndex) : command.substring(startIndex, endIndex);
            command = command.replace("has:" + permission, "");
            if (!player.hasPermission(permission)) {
                return;
            }
        }

        if (command.contains("%~ ")) {
            try {
                String[] split = command.split("%~ ");
                add(Double.parseDouble(split[0]), split[1]);
            } catch (NumberFormatException e) {
                plugin.getLogger().log(Level.SEVERE, "Error parsing chance for game {0} in command:", game.toUpperCase());
                plugin.getLogger().log(Level.SEVERE, "- {0}", command);
            }
        } else {
            commands.add(command);
        }
    }

    public void executeCommands(List<String> commands, Player player) {
        commands.forEach(command -> executeCommand(command, player));
    }

    private void executeCommand(String command, Player player) {
        String cmd = command.replace("%player%", player.getName());

        if (cmd.startsWith("[playercmd]")) {
            Bukkit.dispatchCommand(player, cmd.replace("[playercmd] ", ""));
        } else if (cmd.startsWith("[consolecmd]")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("[consolecmd] ", ""));
        } else if (cmd.startsWith("[playermsg]")) {
            player.sendMessage(MessageUtils.color(cmd.replace("[playermsg] ", "")));
        } else if (cmd.startsWith("[broadcast]")) {
            Bukkit.getOnlinePlayers().stream()
                    .filter(plugin.getGameManager()::worldEnabled)
                    .forEach(p -> p.sendMessage(MessageUtils.color(cmd.replace("[broadcast] ", ""))));
        }
    }
}


