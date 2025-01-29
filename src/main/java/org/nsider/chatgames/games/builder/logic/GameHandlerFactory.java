package org.nsider.chatgames.games.builder.logic;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.builder.logic.handlers.*;

public class GameHandlerFactory {
    public static GameHandler<?> getHandler(GameType type, ChatGames plugin) {
        switch (type) {
            case UNSCRAMBLE:
            case UNREVERSE:
            case REACTION:
            case FILLOUT:
                return new WordBasedGameHandler(plugin, type);
            case RANDOM:
                return new RandomGameHandler(plugin);
            case MATH:
                return new MathGameHandler(plugin);
            case TRIVIA:
                return new TriviaGameHandler(plugin);
            case GUESS_THE_NUMBER:
                return new GuessTheNumberHandler(plugin);
            case VARIABLE:
                return new VariableGameHandler(plugin);
            case SHOPPINGLIST:
                return new ShoppingListGameHandler(plugin);
            case HOVERABLE:
                return new HoverableGameHandler(plugin);
            case CLICKABLE:
                return new ClickableGameHandler(plugin);
            default:
                return null;
        }
    }
}