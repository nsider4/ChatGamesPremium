package org.nsider.chatgames.other;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.nsider.inventorymanager.extras.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Utility class for checking plugin updates on SpigotMC.
 * Fetches the latest version information for a specified resource.
 */
public class UpdateChecker {

    /** The plugin instance using this update checker. */
    private final JavaPlugin plugin;

    /** The resource ID on SpigotMC. */
    private final int resourceId;

    /**
     * Constructs a new UpdateChecker instance.
     *
     * @param plugin     The plugin instance.
     * @param resourceId The resource ID of the plugin on SpigotMC.
     */
    public UpdateChecker(@Nonnull JavaPlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    /**
     * Retrieves the latest version of the plugin from SpigotMC asynchronously.
     * If successful, the version is passed to the provided consumer.
     *
     * @param consumer A consumer to handle the retrieved version string.
     */
    public void getVersion(@Nonnull final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try (InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).openStream();
                 Scanner scanner = new Scanner(inputStream)) {

                if (scanner.hasNext()) {
                    consumer.accept(scanner.next());
                }
            } catch (IOException exception) {
                plugin.getLogger().log(Level.INFO, "Unable to check for updates: {0}", exception.getMessage());
            }
        });
    }
}
