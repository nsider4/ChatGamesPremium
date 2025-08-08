package org.nsider.chatgames.games.builder.logic.handlers;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.games.builder.logic.GameHandler;

import java.util.Random;

public class GuessTheNumberHandler implements GameHandler<String[]> {
    private final ChatGames plugin;
    private final Random random;

    public GuessTheNumberHandler(ChatGames plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @Override
    public String[] handle(String... args) {
        int lowerLimit = ConfigValue.getInt(ConfigPaths.GUESS_NUMBER_LOWER_LIMIT);
        int upperLimit = ConfigValue.getInt(ConfigPaths.GUESS_NUMBER_UPPER_LIMIT);
        String selected;
        String toReplace;

        if (args.length > 1) {
            try {
                int number = Integer.parseInt(args[0]);
                int rangeToShow = Integer.parseInt(args[1]);
                selected = String.valueOf(number);
                toReplace = (number - rangeToShow) + "-" + (number + rangeToShow);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            int randomGuess = random.nextInt((upperLimit - lowerLimit) + 1) + lowerLimit;
            selected = String.valueOf(randomGuess);
            toReplace = lowerLimit + "-" + upperLimit;
        }

        return new String[]{selected, toReplace};
    }
}