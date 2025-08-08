package org.nsider.chatgames.games.builder.logic.handlers;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.builder.BuilderUtils;
import org.nsider.chatgames.games.builder.logic.GameHandler;

import java.util.Collection;

public class WordBasedGameHandler implements GameHandler<String[]> {
    private final ChatGames plugin;
    private final GameType type;

    public WordBasedGameHandler(ChatGames plugin, GameType type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public String[] handle(String... args) {
        Collection<String> words = BuilderUtils.getWordsForGame(plugin, type);
        String selected = args.length > 0 ? args[0] : BuilderUtils.getRandomValue(plugin, words);

        String toReplace = selected;
        switch (type) {
            case UNREVERSE:
                toReplace = plugin.getGameAlgorithms().reverse(selected);
                break;
            case FILLOUT:
                int difficulty = ConfigValue.getInt(ConfigPaths.FILLOUT_DIFFICULTY);
                toReplace = plugin.getGameAlgorithms().hideLetters(selected, difficulty);
                break;
            case UNSCRAMBLE:
                toReplace = plugin.getGameAlgorithms().scrambleWord(selected);
                break;
        }

        return new String[]{selected, toReplace};
    }
}