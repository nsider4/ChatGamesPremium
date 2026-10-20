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
     * @return the section name of this game in the config files
     */
    public String getGameConfigName() {
        return gameType.getConfigKey();
    }
}
