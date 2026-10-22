package org.nsider.chatgames.api;

/**
 * Makes the rounds of a custom chat game. Called on the main thread (the global region on Folia)
 * each time the game starts, so it should be quick and must not block.
 */
@FunctionalInterface
public interface QuestionGenerator {

    /**
     * @throws IllegalStateException if no question can be made (the game then doesn't start,
     *                               and the message is logged)
     */
    Question generate(QuestionContext context);
}
