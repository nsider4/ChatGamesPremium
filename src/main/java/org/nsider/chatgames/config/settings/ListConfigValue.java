package org.nsider.chatgames.config.settings;

import java.util.List;

/**
 * Reads one list setting from the file its {@link ConfigPaths} entry belongs to.
 */
public final class ListConfigValue {

    private ListConfigValue() {}

    public static List<String> getStringList(ConfigPaths path) {
        return path.getFile().getStringList(path.path());
    }

    public static List<Integer> getIntegerList(ConfigPaths path) {
        return path.getFile().getIntegerList(path.path());
    }
}
