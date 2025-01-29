package org.nsider.chatgames.games.builder.logic;


import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.enums.GameType;

public class GameHelper {
    private final ChatGames plugin;

    public GameHelper(ChatGames instance) {
        this.plugin = instance;
    }

    public Object handleGame(GameType type, String... args) {
        GameHandler<?> handler = GameHandlerFactory.getHandler(type, plugin);
        return handler != null ? handler.handle(args) : null;
    }
}
