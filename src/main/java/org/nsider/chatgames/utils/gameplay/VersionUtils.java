package org.nsider.chatgames.utils.gameplay;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects the running Minecraft version.
 *
 * <p>Mojang dropped the "1." prefix after 1.21.11, so versions are compared as
 * (major, minor, patch): 1.8.8 is (1, 8, 8) and 26.1.2 is (26, 1, 2), which
 * makes every 26.x release newer than any 1.x release.</p>
 */
public final class VersionUtils {

    // Spigot reports "1.21.11-R0.2-SNAPSHOT" or "26.1.2-R0.1-SNAPSHOT", Paper reports "26.3.build.207-stable"
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private static int[] version;

    private VersionUtils() {}

    /**
     * Checks whether the server runs the given version or newer.
     *
     * @param major major version, e.g. 1 for 1.16 or 26 for 26.1
     * @param minor minor version, e.g. 16 for 1.16 or 1 for 26.1
     * @return true if the server version is at least major.minor
     */
    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }

    /**
     * Checks whether the server runs the given version or newer.
     *
     * @return true if the server version is at least major.minor.patch
     */
    public static boolean isAtLeast(int major, int minor, int patch) {
        return isAtLeast(getVersion(), major, minor, patch);
    }

    static boolean isAtLeast(int[] current, int major, int minor, int patch) {
        if (current[0] != major) return current[0] > major;
        if (current[1] != minor) return current[1] > minor;
        return current[2] >= patch;
    }

    /**
     * @return the server version as {major, minor, patch}
     */
    public static int[] getVersion() {
        if (version == null) {
            version = parse(Bukkit.getBukkitVersion());
        }
        return version.clone();
    }

    /**
     * Parses a Bukkit version string into {major, minor, patch}.
     * Unparseable input is treated as 1.8.0, the oldest supported version.
     */
    static int[] parse(String bukkitVersion) {
        Matcher matcher = VERSION_PATTERN.matcher(bukkitVersion == null ? "" : bukkitVersion);
        if (!matcher.find()) {
            return new int[]{1, 8, 0};
        }
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), patch};
    }
}
