package org.nsider.chatgames.races.setup;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public class RaceDataLoader {
    private final ChatGames plugin;

    public RaceDataLoader(ChatGames plugin) {
        this.plugin = plugin;
    }

    public HashMap<String, List<Integer>> loadRace(String game) {
        HashMap<String, List<Integer>> mobData = new HashMap<>();
        ConfigurationSection dataSection = ConfigManager.getChatRaces().getConfigurationSection(game + ".data");
        if (dataSection == null) { return mobData; }

        for (String idName : dataSection.getKeys(false)) {
            ConfigurationSection amountSection = dataSection.getConfigurationSection(idName);
            if (amountSection == null) { continue; }

            List<Integer> amounts = new ArrayList<>();
            for (String amount : amountSection.getKeys(false)) {
                try {
                    int parsedAmount = Integer.parseInt(amount);
                    amounts.add(parsedAmount);
                } catch(NumberFormatException e) {
                    plugin.getLogger().log(Level.INFO, "Error while trying to load race parameters for: {0}", idName);
                }
            }

            mobData.put(idName.toUpperCase(), amounts);
        }

        return mobData;
    }
}
