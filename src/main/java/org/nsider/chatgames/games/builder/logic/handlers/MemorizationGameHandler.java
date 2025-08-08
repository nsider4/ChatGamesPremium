package org.nsider.chatgames.games.builder.logic.handlers;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.games.builder.logic.GameHandler;

import java.util.List;

public class MemorizationGameHandler implements GameHandler<String> {
    private final ChatGames plugin;

    public MemorizationGameHandler(ChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public String handle(String... args) {
        int listLength;
        try {
            listLength = args.length > 0
                    ? Integer.parseInt(args[0])
                    : ConfigValue.getInt(ConfigPaths.MEMORIZE_LIST_LENGTH);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid list length provided. Using default value from configuration.");
            listLength = ConfigValue.getInt(ConfigPaths.MEMORIZE_LIST_LENGTH);
        }

        List<String> shoppingList = plugin.getGameAlgorithms().generateMemorizationList(listLength);
        String selected = String.join(", ", shoppingList);

        plugin.getGameManager().startMemorizationGame(selected, selected);

        return selected;
    }
}