package org.nsider.chatgames.reward;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import java.util.List;

public class Reward {
    private final String name;
    private final int minPlayers;
    private final double chance;
    private final List<String> commands;

    public Reward(String name, int minPlayers, double chance, List<String> commands) {
        this.name = name;
        this.minPlayers = minPlayers;
        this.chance = chance;
        this.commands = commands;
    }

    public String getName() {
        return name;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public double getChance() {
        return chance;
    }

    public List<String> getCommands() {
        return commands;
    }

    public static Reward fromConfig(ConfigurationSection section) {
        if (section == null) return null;

        String name = section.getName();
        int minPlayers = section.getInt("minimum-players", 0);
        double chance = section.getDouble("chance", 1.0);
        List<String> commands = section.getStringList("data");

        return new Reward(name, minPlayers, chance, commands);
    }

    public boolean isEligible(int onlinePlayers) {
        return onlinePlayers >= minPlayers;
    }
}
