package org.nsider.chatgames.other;

import org.nsider.chatgames.ChatGames;
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
    private final ChatGames plugin;

    /** The resource ID on SpigotMC. */
    private final int resourceId;

    /**
     * Constructs a new UpdateChecker instance.
     *
     * @param plugin     The plugin instance.
     * @param resourceId The resource ID of the plugin on SpigotMC.
     */
    public UpdateChecker(@Nonnull ChatGames plugin, int resourceId) {
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
        plugin.getScheduler().async(() -> {
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

    /**
     * Compares dotted version numbers, e.g. "2.0.10" is newer than "2.0.9".
     * Anything after the numbers (like "-SNAPSHOT") is ignored.
     *
     * @return true if {@code remote} is a higher version than {@code local}
     */
    public static boolean isNewer(@Nonnull String remote, @Nonnull String local) {
        int[] a = numbers(remote);
        int[] b = numbers(local);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int[] numbers(String version) {
        String[] parts = version.trim().split("[^0-9]+");
        int count = 0;
        int[] result = new int[parts.length];
        for (String part : parts) {
            if (!part.isEmpty()) {
                // Long version segments are capped rather than overflowing
                result[count++] = part.length() > 9 ? Integer.MAX_VALUE : Integer.parseInt(part);
            }
        }
        return java.util.Arrays.copyOf(result, count);
    }
}
