package org.nsider.chatgames.games.builder.logic.handlers;


import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.games.builder.algorithms.RandomCharacterGenerator;
import org.nsider.chatgames.games.builder.logic.GameHandler;

public class RandomGameHandler implements GameHandler<String[]> {
    private final ChatGames plugin;

    public RandomGameHandler(ChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public String[] handle(String... args) {
        String lengthRange = plugin.getConfig().getString("random.lengthRange", "6-8");

        if (args.length > 0) {
            try {
                int specifiedLength = Integer.parseInt(args[0]);
                lengthRange = specifiedLength + "-" + specifiedLength;
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid length parameter provided for random game. Using default length range.");
            }
        }

        String selected = plugin.getGameAlgorithms().generateRandomCharacters(
                plugin.getGameAlgorithms().getRandomLength(lengthRange),
                RandomCharacterGenerator.Type.valueOf(plugin.getConfig().getString("random.character_type"))
        );

        return new String[]{selected, selected};
    }
}
