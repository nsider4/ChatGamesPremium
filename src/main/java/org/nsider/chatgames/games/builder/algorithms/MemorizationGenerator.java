package org.nsider.chatgames.games.builder.algorithms;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Utility class for generating and validating shopping list memory games.
 */
public class MemorizationGenerator {
    private final ChatGames plugin;

    public MemorizationGenerator(ChatGames plugin) {
        this.plugin = plugin;
    }

    /**
     * Generates a randomized shopping list based on configurable settings.
     *
     * @param listLength The number of items to include in the list.
     * @return A list of items to memorize.
     */
    public static List<String> generateMemorizationList(int listLength) {
        List<String> items = new ArrayList<>(ConfigManager.getWords().getStringList("shoppinglist.items"));

        if (listLength > items.size()) {
            listLength = items.size();
        }

        Collections.shuffle(items, new Random());
        return items.subList(0, listLength);
    }

    /**
     * Validates the player's response against the shopping list.
     *
     * @param shoppingList   The original shopping list.
     * @param playerResponse The player's submitted response as a comma-separated string.
     * @return True if the player's response matches the shopping list in order, otherwise false.
     */
    public static boolean validateResponse(List<String> shoppingList, String playerResponse) {
        String[] items = playerResponse.split(",\\s*");
        List<String> playerList = new ArrayList<>();
        Collections.addAll(playerList, items);

        return playerList.equals(shoppingList);
    }
}
