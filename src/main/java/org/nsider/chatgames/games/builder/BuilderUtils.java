package org.nsider.chatgames.games.builder;

import org.bukkit.Bukkit;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.builder.logic.GameHandler;
import org.nsider.chatgames.games.builder.logic.GameHandlerFactory;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.*;

public class BuilderUtils {
    protected static Object handleGameType(ChatGames plugin, GameType type, String... args) {
        GameHandler<?> handler = GameHandlerFactory.getHandler(type, plugin);
        if (handler != null) {
            return handler.handle(args);
        }
        return null;
    }

    protected static boolean isSpecialGame(GameType type) {
        return type == GameType.VARIABLE || type == GameType.CLICKABLE || type == GameType.HOVERABLE || type == GameType.SHOPPINGLIST;
    }

    public static Collection<String> getWordsForGame(ChatGames plugin, GameType type) {
        switch (type) {
            case UNSCRAMBLE:
                return ConfigManager.getWords().getStringList("unscramble.Words");
            case UNREVERSE:
                return ConfigManager.getWords().getStringList("unreverse.Words");
            case REACTION:
                return ConfigManager.getWords().getStringList("reaction.Words");
            case FILLOUT:
                return ConfigManager.getWords().getStringList("fillout.Words");
            default:
                return new ArrayList<>();
        }
    }

    public static String getRandomValue(ChatGames plugin, Collection<String> words) {
        boolean validWord = checkBlacklistedWords(plugin, words);
        Random random = new Random();
        synchronized (random) {
            return words.stream()
                    .filter(word -> !validWord)
                    .skip(random.nextInt(words.size()))
                    .findFirst()
                    .orElse("");
        }
    }

    protected static boolean checkBlacklistedWords(ChatGames plugin, Collection<String> words) {
        if (ConfigManager.getWords().isSet("blacklistedWords")) {
            List<String> blacklistedWords = ConfigManager.getWords().getStringList("blacklistedWords");
            for (String word : words) {
                if (blacklistedWords.contains(word)) {
                    plugin.getLogger().warning("A BlackListed word was found: " + word);
                    return true;
                }
            }
        }
        return false;
    }

    public static String buildStringFromList(Collection<String> answers) {
        StringBuilder correctAnswerBuilder = new StringBuilder();
        for (String answer : answers) {
            correctAnswerBuilder.append("•").append(answer);
        }
        return correctAnswerBuilder.toString();
    }

    private static String color(ChatGames plugin, String message) {
        return MessageUtils.color(message);
    }
}
