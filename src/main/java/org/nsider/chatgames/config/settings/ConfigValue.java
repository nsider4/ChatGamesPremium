package org.nsider.chatgames.config.settings;

/**
 * Reads one setting from the file its {@link ConfigPaths} entry belongs to.
 */
public final class ConfigValue {

    private ConfigValue() {}

    public static String getString(ConfigPaths path) {
        return path.getFile().getString(path.path());
    }

    public static boolean getBoolean(ConfigPaths path) {
        return path.getFile().getBoolean(path.path());
    }

    public static int getInt(ConfigPaths path) {
        return path.getFile().getInt(path.path());
    }

    public static double getDouble(ConfigPaths path) {
        return path.getFile().getDouble(path.path());
    }
}

