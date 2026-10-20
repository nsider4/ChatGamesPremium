package org.nsider.chatgames.games.round;

import org.nsider.inventorymanager.extras.Nonnull;

/**
 * Builds the rounds of one game. Implementations have no side effects.
 */
public interface RoundGenerator {

    /**
     * @param args optional values from an admin's start command, e.g. the word to use
     * @throws IllegalStateException if the game's settings or word lists can't produce a round
     */
    @Nonnull
    Round generate(@Nonnull RoundContext context, @Nonnull String... args);
}
