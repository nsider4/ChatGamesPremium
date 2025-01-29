package org.nsider.chatgames.games.manager;

import org.nsider.chatgames.enums.GameType;

public class GameUtility {

    public static boolean isRace(GameType type) {
        return type == GameType.CRAFT ||
                type == GameType.FURNACE ||
                type == GameType.EAT ||
                type == GameType.FISH ||
                type == GameType.HUNT ||
                type == GameType.MINE ||
                type == GameType.PLACE;
    }

    public static boolean isMaterialRace(GameType type) {
        return type == GameType.CRAFT ||
                type == GameType.FURNACE ||
                type == GameType.MINE ||
                type == GameType.PLACE;
    }
}
