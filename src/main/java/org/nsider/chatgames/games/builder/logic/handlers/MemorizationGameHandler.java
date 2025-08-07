package org.nsider.chatgames.games.builder.logic.handlers;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.games.builder.algorithms.MemorizationGenerator;
import org.nsider.chatgames.games.builder.logic.GameHandler;

import java.util.List;

/**
 * Game handler for the shopping list memory game.
 */
public class MemorizationGameHandler implements GameHandler<String[]> {
    private final ChatGames plugin;
    private final MemorizationGenerator generator;

    public MemorizationGameHandler(ChatGames plugin) {
        this.plugin = plugin;
        this.generator = new MemorizationGenerator(plugin);
    }

    @Override
    public String[] handle(String... args) {
        try {
            int listLength = plugin.getConfig().getInt("shoppinglist.listLength", 3);
            List<String> shoppingList = generator.generateShoppingList(listLength);

            String answer = String.join(", ", shoppingList);
            return new String[]{answer, answer};
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
