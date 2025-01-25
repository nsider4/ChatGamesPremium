package org.nsider.chatgames;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ChatGames extends JavaPlugin {

    @Override
    public void onEnable() {
        File config = new File(getDataFolder()+File.separator+"config.yml");
        if (!config.exists()) {
            saveDefaultConfig();
        }


        if (Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceHolderHook(this).register();
            getLogger().info("PlaceholderAPI Hooked!");
            this.isPlaceHolderAPIEnabled = true;
        }

        versionUtils.correctConfigs(getConfig(), getConfigManager().getChatRaces());
        checkForUpdate();
        if(getConfig().getInt("time_minutes") != -1) { //DO IN THE OTHER CLASS
            mainTask.enable();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
