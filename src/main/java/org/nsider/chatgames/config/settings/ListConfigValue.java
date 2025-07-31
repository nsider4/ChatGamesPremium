package org.nsider.chatgames.config.settings;


import java.util.List;

public class ListConfigValue {

    public static List<String> getStringList(ConfigPaths path) {
        return path.getFile().getStringList(path.path());
    }

    public static List<Integer> getIntegerList(ConfigPaths path) {
        return path.getFile().getIntegerList(path.path());
    }
}
