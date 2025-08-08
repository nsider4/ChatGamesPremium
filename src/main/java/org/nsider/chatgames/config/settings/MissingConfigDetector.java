package org.nsider.chatgames.config.settings;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.config.ConfigManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MissingConfigDetector {

    public static void checkMissingKeys() {
        Set<String> configKeys = getAllKeys(ConfigManager.getConfig(), "");
        Set<String> definedKeys = Arrays.stream(ConfigPaths.values())
                .map(ConfigPaths::path)
                .collect(Collectors.toSet());

        configKeys.removeAll(definedKeys);

        if (!configKeys.isEmpty()) {
            Bukkit.getLogger().warning("Missing ConfigKey enum values for the following config paths:");
            configKeys.forEach(key -> Bukkit.getLogger().warning(" - " + key));
        } else {
            Bukkit.getLogger().info("All config keys are represented in the ConfigKey enum.");
        }
    }

    private static Set<String> getAllKeys(ConfigurationSection section, String prefix) {
        Set<String> keys = new HashSet<>();
        for (String key : section.getKeys(false)) {
            String fullPath = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = section.get(key);
            if (value instanceof ConfigurationSection) {
                keys.addAll(getAllKeys((ConfigurationSection) value, fullPath));
            } else {
                keys.add(fullPath);
            }
        }
        return keys;
    }
}

