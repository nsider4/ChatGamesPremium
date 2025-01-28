package org.nsider.chatgames.utils.messages;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.utils.gameplay.Game;
import org.nsider.chatgames.utils.gameplay.GameType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TextMatchUtils {

    /**
     * Cleans and normalizes the chat input.
     *
     * @param chatTextRaw the raw input
     * @return the normalized input
     */
    private static String cleanChatInput(String chatTextRaw) {
        String chatText = chatTextRaw.trim();
        return chatText.endsWith(".") ? chatText.substring(0, chatText.length() - 1) : chatText;
    }

    /**
     * Splits answers if they are delimited by a specific character.
     *
     * @param selected the answer string
     * @return a list of individual answers
     */
    private static List<String> extractAnswers(String selected) {
        return selected.contains("•")
                ? Arrays.asList(selected.split("•"))
                : Collections.singletonList(selected);
    }

    /**
     * Compares the player's input with the game's answers.
     *
     * @param player      the player
     * @param chatTextRaw the raw input from the player
     * @param game        the current game instance
     * @param plugin      the plugin instance
     * @return true if a match is found, false otherwise
     */
    protected static boolean textEqualsWord(Player player, String chatTextRaw, Game game, ChatGames plugin) {
        String chatText = cleanChatInput(chatTextRaw);
        List<String> answers = extractAnswers(game.getAnswer());

        for (String answer : answers) {
            if (!chatText.isEmpty() && !answer.isEmpty()) {
                boolean caseSensitive = plugin.getConfig().getBoolean(game.getGameConfigName() + ".case-sensitive");
                String normalizedChat = chatText.replace(" ", "");
                String normalizedAnswer = answer.replace(" ", "");

                boolean matches = caseSensitive
                        ? normalizedAnswer.equals(normalizedChat)
                        : normalizedAnswer.equalsIgnoreCase(normalizedChat);

                if (matches) {
                    game.setAnswer(answer);
                    return true;
                }
            }
        }
        return false;
    }
}