package org.nsider.chatgames.games.manager;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.config.settings.ListConfigValue;

import java.util.List;
import java.util.Random;

public class GameConfigManager {
    private final ChatGames plugin;

    public GameConfigManager(ChatGames plugin) {
        this.plugin = plugin;
    }

    public int getGameWinners(String game) {
        return ConfigValue.getInt(ConfigPaths.valueOf(game.toUpperCase() + "_WINNERS"));
    }

    public long getNextGameDelay() {
        if (ConfigValue.getBoolean(ConfigPaths.RANDOM_DELAY_ENABLED)) {
            Random random = new Random();
            int min = ConfigValue.getInt(ConfigPaths.RANDOM_DELAY_MIN);
            int max = ConfigValue.getInt(ConfigPaths.RANDOM_DELAY_LIMIT);
            String unit = ConfigValue.getString(ConfigPaths.RANDOM_DELAY_UNIT).toLowerCase();

            long delay = random.nextInt(max - min + 1) + min;
            return unit.equals("seconds") ? delay * 20L : delay * 20L * 60L;
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

    public String getGameSound(String type) {
        return ConfigValue.getString(ConfigPaths.valueOf("SOUND_" + type.toUpperCase()));
    }
}