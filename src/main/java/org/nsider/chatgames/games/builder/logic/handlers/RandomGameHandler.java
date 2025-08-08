package org.nsider.chatgames.games.builder.logic.handlers;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.games.builder.algorithms.RandomCharacterGenerator;
import org.nsider.chatgames.games.builder.logic.GameHandler;

public class RandomGameHandler implements GameHandler<String[]> {
    private final ChatGames plugin;

    public RandomGameHandler(ChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public String[] handle(String... args) {
        String lengthRange = ConfigValue.getString(ConfigPaths.RANDOM_LENGTH_RANGE);

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
                RandomCharacterGenerator.Type.valueOf(ConfigValue.getString(ConfigPaths.RANDOM_CHARACTER_TYPE))
        );

        return new String[]{selected, selected};
    }
}
