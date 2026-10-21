package org.nsider.chatgames.games.round.generators;

import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.round.Hints;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.games.round.RoundContext;
import org.nsider.chatgames.games.round.RoundGenerator;

import org.nsider.inventorymanager.extras.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The games built from a words.yml list: Unscramble, Unreverse, Reaction, Fill-out and Hoverable.
 */
public final class WordRoundGenerator implements RoundGenerator {

    private final GameType type;

    public WordRoundGenerator(@Nonnull GameType type) {
        this.type = type;
    }

    @Nonnull
    @Override
    public Round generate(@Nonnull RoundContext context, @Nonnull String... args) {
        String word = args.length > 0 && !args[0].trim().isEmpty()
                ? String.join(" ", args)
                : context.randomWord(type.getConfigKey() + ".Words");
        Round.Builder round = Round.builder(type, word);

        switch (type) {
            case UNSCRAMBLE:
                return round.display(scramble(word, context.getRandom()))
                        .hints(Hints.revealFromStart(word)).build();
            case UNREVERSE:
                return round.display(new StringBuilder(word).reverse().toString())
                        .hints(Hints.revealFromStart(word)).build();
            case FILLOUT:
                String puzzle = hideLetters(word, context.getSettings().getInt("difficulty", 30), context.getRandom());
                return round.display(puzzle)
                        .hints(Hints.fillBlanks(word, puzzle, context.getRandom())).build();
            case HOVERABLE:
                // The word is only in the hover text; the title must not give it away
                String text = context.getMessages().getString("hoverableText", "&eHover Here!");
                String hover = context.getMessages().getString("hoverText", "&b%selection%").replace("%selection%", word);
                return round.component(new Round.ComponentSpec("%hoverableText%", text, hover, null)).build();
            default:
                return round.display(word).build();
        }
    }

    /**
     * Shuffles the letters of each word separately, so the word count stays visible.
     * Tries a few times to avoid handing back the word unchanged.
     */
    static String scramble(String text, Random random) {
        for (int attempt = 0; attempt < 10; attempt++) {
            List<String> words = new ArrayList<>();
            for (String word : text.split(" ")) {
                List<Character> letters = new ArrayList<>();
                for (char c : word.toCharArray()) letters.add(c);
                Collections.shuffle(letters, random);
                StringBuilder shuffled = new StringBuilder();
                for (char c : letters) shuffled.append(c);
                words.add(shuffled.toString());
            }
            String result = String.join(" ", words);
            if (!result.equals(text)) return result;
        }
        return new StringBuilder(text).reverse().toString();
    }

    /**
     * Replaces hidePercent% of each word's letters with '_', at distinct positions.
     * At least one letter is hidden and, for words longer than one letter, at least one stays.
     */
    static String hideLetters(String text, int hidePercent, Random random) {
        int percent = Math.max(1, Math.min(100, hidePercent));
        List<String> words = new ArrayList<>();
        for (String word : text.split(" ")) {
            int toHide = (int) Math.ceil(word.length() * percent / 100.0);
            toHide = Math.max(1, Math.min(toHide, Math.max(1, word.length() - 1)));
            List<Integer> positions = new ArrayList<>();
            for (int i = 0; i < word.length(); i++) positions.add(i);
            Collections.shuffle(positions, random);
            char[] chars = word.toCharArray();
            for (int i = 0; i < toHide && i < positions.size(); i++) {
                chars[positions.get(i)] = '_';
            }
            words.add(new String(chars));
        }
        return String.join(" ", words);
    }
}
