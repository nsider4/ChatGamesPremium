package org.nsider.chatgames.inventory;

import lombok.Getter;
import org.nsider.chatgames.config.YamlConfig;

/**
 * Enum representing the menus in the plugin.
 */
@Getter
public enum Menu {;//TODO REMOVE ;
    //MAIN(ConfigManager.getMainMenu(), "MainMenu"),

    private final String section;
    private final YamlConfig config;

    /**
     * Constructor for the enum.
     *
     * @param config the config for the menu
     * @param section the section of the config for the menu
     */
    Menu(YamlConfig config, String section) {
        this.config = config;
        this.section = section;
    }
}


