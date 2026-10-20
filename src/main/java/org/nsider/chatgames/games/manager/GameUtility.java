package org.nsider.chatgames.games.manager;

import org.nsider.chatgames.enums.GameType;

public class GameUtility {

    public static boolean isRace(GameType type) {
        return type.isRace();
    }

    public static boolean isMaterialRace(GameType type) {
        return type.isMaterialRace();
    }
}
