package org.nsider.chatgames.other;

import org.nsider.chatgames.ChatGames;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;

/**
 * Utility class for checking plugin updates on SpigotMC.
 * Fetches the latest version information for a specified resource.
 */
public final class UpdateChecker {

    /** How long to wait for SpigotMC before giving up, in milliseconds. */
    private static final int TIMEOUT_MILLIS = 10_000;

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
     * Asks SpigotMC for the latest version. Blocks for up to the timeout, so call it off the main thread.
     *
     * @return the latest version, or null if SpigotMC couldn't be reached
     */
    @Nullable
    public String fetchVersion() {
        try {
            URLConnection connection = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).openConnection();
            // Without timeouts an unresponsive site would hold an async thread forever
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            try (InputStream inputStream = connection.getInputStream();
                 Scanner scanner = new Scanner(inputStream, "UTF-8")) {
                return scanner.hasNext() ? scanner.next() : null;
            }
        } catch (IOException exception) {
            plugin.getLogger().log(Level.INFO, "Unable to check for updates: {0}", exception.getMessage());
            return null;
        }
    }

    /**
     * The page players download the plugin from.
     */
    @Nonnull
    public String getResourceUrl() {
        return "https://www.spigotmc.org/resources/" + resourceId + "/";
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

    /**
     * Packs the first three numbers of a version into one long ("2.0.3" is 2_000000_000003), so the
     * version an admin was last told about fits in a player stat.
     */
    public static long versionKey(@Nonnull String version) {
        int[] parts = numbers(version);
        long key = 0;
        for (int i = 0; i < 3; i++) {
            key = key * 1_000_000L + (i < parts.length ? Math.min(parts[i], 999_999) : 0);
        }
        return key;
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
        return Arrays.copyOf(result, count);
    }
}
