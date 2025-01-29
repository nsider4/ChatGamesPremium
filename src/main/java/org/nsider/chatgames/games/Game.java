package org.nsider.chatgames.games;

import lombok.Getter;
import lombok.Setter;
import org.nsider.chatgames.enums.GameType;

/**
 * Represents a game instance with a specific type and answer.
 *
 * <p>This class provides details about the game type and allows the answer to be
 * set or retrieved. It also provides a mapping of the game type to its configuration name.</p>
 */
@Getter
public class Game {

    /**
     * The type of the game, defined by {@link GameType}.
     */
    private final GameType gameType;

    /**
     * The current answer for the game.
     * This value can be updated dynamically during gameplay.
     */
    @Setter
    private String answer;

    /**
     * Constructs a new {@code Game} instance with the specified type and answer.
     *
     * @param gameType The type of the game.
     * @param answer   The initial answer for the game.
     */
    public Game(GameType gameType, String answer) {
        this.gameType = gameType;
        this.answer = answer;
    }

    /**
     * Retrieves the configuration name corresponding to the game type.
     *
     * @return A {@link String} representing the configuration name of the game type.
     *         Returns an empty string if the game type is not recognized.
     */
    public String getGameConfigName() {
        switch (gameType) {
            case UNSCRAMBLE:
                return "unscramble";
            case UNREVERSE:
                return "unreverse";
            case REACTION:
                return "reaction";
            case RANDOM:
                return "random";
            case FILLOUT:
                return "fillout";
            case MATH:
                return "math";
            case VARIABLE:
                return "variable";
            case GUESS_THE_NUMBER:
                return "guess_the_number";
            case SHOPPINGLIST:
                return "shoppinglist";
            case TRIVIA:
                return "trivia";
            case HOVERABLE:
                return "hoverable";
            case CLICKABLE:
                return "clickable";
            default:
                return "";
        }
    }
}
