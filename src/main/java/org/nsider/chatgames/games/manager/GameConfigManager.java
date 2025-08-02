package org.nsider.chatgames.games.manager;

import org.nsider.chatgames.ChatGames;

import java.util.List;
import java.util.Random;

public class GameConfigManager {
    private final ChatGames plugin;

    public GameConfigManager(ChatGames plugin) {
        this.plugin = plugin;
    }

    public int getGameWinners(String game) {
        return plugin.getConfig().getInt(game + ".winners", 1);
    }

    public long getNextGameDelay() {
        if (plugin.getConfig().getBoolean("randomDelay.enable")) {
            Random random = new Random();
            int min = plugin.getConfig().getInt("randomDelay.minimum");
            int max = plugin.getConfig().getInt("randomDelay.limit");
            String unit = plugin.getConfig().getString("randomDelay.timeUnit", "seconds").toLowerCase();

            long delay = random.nextInt(max - min + 1) + min;
            return unit.equals("seconds") ? delay * 20L : delay * 20L * 60L;
        }

        return plugin.getConfig().getInt("time_minutes") * 20L * 60L;
    }

    public List<String> getDisabledWorlds() {
        return plugin.getConfig().getStringList("disabled-worlds");
    }

    public boolean isPermissionRequiredToPlay() {
        return plugin.getConfig().getBoolean("use-permission-toplay");
    }

    public boolean countAllPlayers() {
        return plugin.getConfig().getBoolean("count-all-players");
    }

    public float getGameSoundVolume() {
        return (float) plugin.getConfig().getDouble("GameSounds.volume", 0.5);
    }

    public float getGameSoundPitch() {
        return (float) plugin.getConfig().getDouble("GameSounds.pitch", 1.0);
    }

    public String getGameSound(String type) {
        return plugin.getConfig().getString("GameSounds." + type, "");
    }
}
