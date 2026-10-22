package org.nsider.chatgames.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.api.GameType;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Named sets of enabled games, stored in presets.yml. While a preset is active it
 * replaces the per-game {@code enable} flags of games.yml and races.yml.
 */
public final class PresetManager {

    public static final String NONE = "none";
    private static final String ACTIVE = "active";
    private static final String PRESETS = "presets";

    private PresetManager() {}

    /**
     * @return the active preset's name, or null if the games.yml flags are used
     */
    @Nullable
    public static String getActive() {
        String active = file().getString(ACTIVE, NONE);
        return active == null || active.trim().isEmpty() || active.equalsIgnoreCase(NONE) || !exists(active)
                ? null : active;
    }

    @Nonnull
    public static List<String> getNames() {
        ConfigurationSection section = file().getConfigurationSection(PRESETS);
        return section == null ? Collections.<String>emptyList() : new ArrayList<>(section.getKeys(false));
    }

    public static boolean exists(@Nonnull String name) {
        return file().isConfigurationSection(PRESETS + "." + name);
    }

    /**
     * @return the preset's games in file order; unknown names are skipped with a warning
     */
    @Nonnull
    public static List<GameType> getGames(@Nonnull String name, @Nullable Logger logger) {
        List<GameType> games = new ArrayList<>();
        for (String key : file().getStringList(PRESETS + "." + name + ".games")) {
            GameType type = GameType.fromName(key);
            if (type == null) {
                if (logger != null) logger.warning("Preset '" + name + "' lists an unknown game: " + key);
            } else if (!games.contains(type)) {
                games.add(type);
            }
        }
        return games;
    }

    /**
     * Makes a preset active, or none with null.
     */
    public static void setActive(@Nullable String name) {
        file().set(ACTIVE, name == null ? NONE : name);
        file().save();
    }

    /**
     * Saves a set of games under a name, replacing a preset of the same name.
     */
    public static void save(@Nonnull String name, @Nonnull List<GameType> games) {
        List<String> keys = new ArrayList<>();
        for (GameType type : games) keys.add(type.getConfigKey());
        file().set(PRESETS + "." + name + ".games", keys);
        file().save();
    }

    /**
     * Preset names are YAML keys: letters, digits, '-' and '_' only.
     */
    public static boolean isValidName(@Nonnull String name) {
        return name.matches("[A-Za-z0-9_-]{1,32}") && !name.toLowerCase(Locale.ROOT).equals(NONE);
    }

    private static YamlConfig file() {
        return ConfigManager.getPresets();
    }
}
