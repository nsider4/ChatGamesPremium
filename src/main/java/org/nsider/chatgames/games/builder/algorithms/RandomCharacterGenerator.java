package org.nsider.chatgames.games.builder.algorithms;


import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;

public class RandomCharacterGenerator {

    public enum Type {
        ALPHA, ALPHANUMERIC, NUMERIC, SYMBOLIC, LOWERCASE_ALPHA, UPPERCASE_ALPHA, HEXADECIMAL, CUSTOM
    }

    public static String generateRandomCharacters(ChatGames plugin, int length, Type type) {
        String characters = getCharactersByType(plugin, type);
        return buildRandomString(length, characters);
    }

    private static String getCharactersByType(ChatGames plugin, Type type) {
        switch (type) {
            case ALPHA:
                return "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
            case ALPHANUMERIC:
                return "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            case NUMERIC:
                return "0123456789";
            case SYMBOLIC:
                return "~,.:?;[]{}´`&*()^!@#$%¨-_+=></ ";
            case LOWERCASE_ALPHA:
                return "abcdefghijklmnopqrstuvwxyz";
            case UPPERCASE_ALPHA:
                return "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            case HEXADECIMAL:
                return "0123456789ABCDEF";
            case CUSTOM:
                return ConfigValue.getString(ConfigPaths.RANDOM_CUSTOM_CHARACTERS);
            default:
                throw new IllegalArgumentException("Unknown Type: " + type);
        }
    }

    private static String buildRandomString(int length, String characters) {
        StringBuilder builder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * characters.length());
            builder.append(characters.charAt(index));
        }

        return builder.toString();
    }
}
