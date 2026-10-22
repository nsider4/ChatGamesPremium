package org.nsider.chatgames.api;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Random;

/**
 * What a {@link QuestionGenerator} can read while making a round.
 */
public interface QuestionContext {

    GameType getGame();

    /**
     * @return the game's section of games.yml. A game event may override some values
     *         (a difficulty, say), and those overrides are already applied here.
     */
    ConfigurationSection getSettings();

    /** @return the game's section of messages.yml */
    ConfigurationSection getMessages();

    /** @return the whole of words.yml */
    ConfigurationSection getWords();

    /** @return the values after the game's name in {@code /chatgames <game> ...}; empty for normal rounds */
    String[] getArgs();

    Random getRandom();

    /**
     * @param list the words to pick from, e.g. a list from the game's settings
     * @return a random word that isn't in words.yml's blacklist
     * @throws IllegalStateException if no word is left
     */
    String randomWord(List<String> list);
}
