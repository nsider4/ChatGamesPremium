package org.nsider.chatgames.games.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.config.settings.ListConfigValue;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.games.round.GameSettings;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;
import java.util.List;
import java.util.Random;

public class GameConfigManager {

    private final ChatGames plugin;
    private final Random random = new Random();

    public GameConfigManager(ChatGames plugin) {
        this.plugin = plugin;
    }

    /**
     * The game's current settings from games.yml.
     */
    @Nonnull
    public GameSettings settings(@Nonnull GameType type) {
        return new GameSettings(type, section(ConfigManager.getGames(), type));
    }

    /**
     * The game's section in a config file. Memorize also accepts the 1.9.9 name
     * "shoppinglist", so configs carried over from 1.9.9 keep working.
     */
    @Nullable
    public static ConfigurationSection section(@Nonnull ConfigurationSection root, @Nonnull GameType type) {
        ConfigurationSection section = root.getConfigurationSection(type.getConfigKey());
        if (section == null && type == GameType.MEMORIZE) {
            section = root.getConfigurationSection("shoppinglist");
        }
        return section;
    }

    /**
     * @return ticks until the next rotation game: fixed, or rolled between the random-delay bounds each time
     */
    public long getNextGameDelay() {
        if (ConfigValue.getBoolean(ConfigPaths.RANDOM_DELAY_ENABLED)) {
            int min = ConfigValue.getInt(ConfigPaths.RANDOM_DELAY_MIN);
            int max = ConfigValue.getInt(ConfigPaths.RANDOM_DELAY_LIMIT);
            String unit = ConfigValue.getString(ConfigPaths.RANDOM_DELAY_UNIT);
            long delay = Math.min(min, max) + random.nextInt(Math.abs(max - min) + 1);
            return "seconds".equalsIgnoreCase(unit) ? delay * 20L : delay * 20L * 60L;
        }
        return ConfigValue.getInt(ConfigPaths.TIME_MINUTES) * 20L * 60L;
    }

    public List<String> getDisabledWorlds() {
        return ListConfigValue.getStringList(ConfigPaths.DISABLED_WORLDS);
    }

    public boolean isPermissionRequiredToPlay() {
        return ConfigValue.getBoolean(ConfigPaths.REQUIRE_PERMISSION_TO_PLAY);
    }

    public boolean countAllPlayers() {
        return ConfigValue.getBoolean(ConfigPaths.COUNT_ALL_PLAYERS);
    }

    public float getGameSoundVolume() {
        return (float) ConfigValue.getDouble(ConfigPaths.GAME_SOUND_VOLUME);
    }

    public float getGameSoundPitch() {
        return (float) ConfigValue.getDouble(ConfigPaths.GAME_SOUND_PITCH);
    }

    /**
     * @param type the key under GameSounds, e.g. "win"
     */
    public String getGameSound(String type) {
        return ConfigManager.getConfig().getString("GameSounds." + type, "");
    }
}
