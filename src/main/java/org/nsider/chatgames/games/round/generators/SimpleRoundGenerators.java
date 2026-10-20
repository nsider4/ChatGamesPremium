package org.nsider.chatgames.games.round.generators;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.round.Hints;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.games.round.RoundGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The small games that need only a few lines each: Random, Clickable, Guess the Number and Trivia.
 */
public final class SimpleRoundGenerators {

    private SimpleRoundGenerators() {}

    /** Type a random string of characters. */
    public static final RoundGenerator RANDOM = (context, args) -> {
        ConfigurationSection settings = context.getSettings();
        int[] range = parseRange(settings.getString("lengthRange", "6-8"), 6, 8);
        if (args.length > 0) {
            Integer length = parseInt(args[0]);
            if (length != null && length > 0) range = new int[]{length, length};
        }
        String characters = characters(settings.getString("character_type", "ALPHANUMERIC"),
                settings.getString("custom_characters", ""));
        String text = randomString(characters, context.between(range[0], range[1]), context.getRandom());
        return Round.builder(GameType.RANDOM, text).display(text).build();
    };

    /** First to click the chat text wins; the click runs /chatgames with a one-off key. */
    public static final RoundGenerator CLICKABLE = (context, args) -> {
        String key = randomString(characters("ALPHANUMERIC", ""), 12, context.getRandom());
        String text = context.getMessages().getString("clickableText", "&aClick Here!");
        String hover = context.getMessages().getString("hoverText", "&eClick Here!");
        return Round.builder(GameType.CLICKABLE, key)
                .component(new Round.ComponentSpec("%clickableText%", text, hover, "/chatgames " + key))
                .build();
    };

    /**
     * Guess a number in a range. Hints narrow the range, always keeping the answer inside.
     * Args: the number, then how far either side of it the shown range reaches.
     */
    public static final RoundGenerator GUESS_THE_NUMBER = (context, args) -> {
        int lower = context.getSettings().getInt("lower-limit", 1);
        int upper = context.getSettings().getInt("upper-limit", 20);
        int answer = context.between(lower, upper);
        int low = Math.min(lower, upper);
        int high = Math.max(lower, upper);
        if (args.length > 1) {
            Integer number = parseInt(args[0]);
            Integer spread = parseInt(args[1]);
            if (number != null && spread != null) {
                answer = number;
                low = number - Math.abs(spread);
                high = number + Math.abs(spread);
            }
        }
        return Round.builder(GameType.GUESS_THE_NUMBER, String.valueOf(answer))
                .display(low + "-" + high)
                .hints(narrowingRanges(answer, low, high, context.getRandom()))
                .build();
    };

    /** A question from words.yml with any number of accepted answers. Arg: the question id. */
    public static final RoundGenerator TRIVIA = (context, args) -> {
        ConfigurationSection data = context.getWords().getConfigurationSection("trivia.data");
        if (data == null || data.getKeys(false).isEmpty()) {
            throw new IllegalStateException("words.yml has no trivia questions under 'trivia.data'");
        }
        List<String> ids = new ArrayList<>(data.getKeys(false));
        String id = args.length > 0 && data.isConfigurationSection(args[0])
                ? args[0]
                : ids.get(context.getRandom().nextInt(ids.size()));
        List<String> answers = data.getStringList(id + ".answers");
        if (answers.isEmpty()) {
            throw new IllegalStateException("Trivia question '" + id + "' has no answers");
        }
        return Round.builder(GameType.TRIVIA, answers.get(0))
                .answers(answers)
                .display(data.getString(id + ".question", ""))
                .hints(Hints.revealFromStart(answers.get(0)))
                .build();
    };

    static List<String> narrowingRanges(int answer, int low, int high, Random random) {
        List<String> hints = new ArrayList<>();
        int width = high - low;
        for (int i = 0; i < Hints.PREPARED; i++) {
            width /= 2;
            if (width < 2) break;
            // A window of the new width that still contains the answer, placed at random
            int start = answer - random.nextInt(width + 1);
            start = Math.max(low, Math.min(start, high - width));
            low = start;
            high = start + width;
            hints.add(low + "-" + high);
        }
        return hints;
    }

    static String characters(String type, String custom) {
        switch (type.toUpperCase(Locale.ROOT)) {
            case "ALPHA":
                return "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
            case "NUMERIC":
                return "0123456789";
            case "SYMBOLIC":
                return "~,.:?;[]{}`&*()^!@#$%-_+=></";
            case "LOWERCASE_ALPHA":
                return "abcdefghijklmnopqrstuvwxyz";
            case "UPPERCASE_ALPHA":
                return "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            case "HEXADECIMAL":
                return "0123456789ABCDEF";
            case "CUSTOM":
                if (custom != null && !custom.isEmpty()) return custom;
                // fall through: an empty custom set would make an empty string
            default:
                return "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        }
    }

    static String randomString(String characters, int length, Random random) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(characters.charAt(random.nextInt(characters.length())));
        }
        return builder.toString();
    }

    /** Parses "min-max"; a single number means exactly that. */
    static int[] parseRange(String range, int defaultMin, int defaultMax) {
        if (range == null) return new int[]{defaultMin, defaultMax};
        String[] parts = range.trim().split("-");
        Integer min = parseInt(parts[0]);
        Integer max = parts.length > 1 ? parseInt(parts[1]) : min;
        if (min == null || max == null || min < 1 || max < 1) return new int[]{defaultMin, defaultMax};
        return new int[]{Math.min(min, max), Math.max(min, max)};
    }

    static Integer parseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
