package org.nsider.chatgames.games.builder.logic.handlers;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.games.builder.logic.GameHandler;

import java.util.List;

public class ShoppingListGameHandler implements GameHandler<String> {
    private final ChatGames plugin;

    public ShoppingListGameHandler(ChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public String handle(String... args) {
        int listLength;
        try {
            listLength = args.length > 0 ? Integer.parseInt(args[0]) : plugin.getConfig().getInt("shoppinglist.listLength");
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid list length provided. Using default value from configuration.");
            listLength = plugin.getConfig().getInt("shoppinglist.listLength");
        }

        List<String> shoppingList = plugin.getGameAlgorithms().generateShoppingList(listLength);
        String selected = String.join(", ", shoppingList);

        plugin.getGameManager().startMemorizationGame(selected, selected);

        return selected;
    }
}