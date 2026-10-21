package org.nsider.chatgames.games.round;

import org.nsider.inventorymanager.extras.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds progressive hints. Each hint gives away a little more than the one before.
 */
public final class Hints {

    /** How many hints a generator prepares; games.yml picks how many are shown. */
    public static final int PREPARED = 3;

    private static final char BLANK = '_';

    private Hints() {}

    /**
     * Reveals the answer from the left: "C______", "Cr_____", "Cre____".
     * Spaces stay visible so the word count shows. Never reveals the whole answer.
     */
    @Nonnull
    public static List<String> revealFromStart(@Nonnull String answer) {
        List<String> hints = new ArrayList<>();
        int letters = countLetters(answer);
        for (int shown = 1; shown <= PREPARED && shown < letters; shown++) {
            String hint = mask(answer, shown);
            // An answer ending in '_' (a name like "Jeb_") would otherwise be given away
            if (hint.equals(answer)) break;
            hints.add(hint);
        }
        return hints;
    }

    /**
     * Fills in one more hidden letter of a fill-out puzzle per hint, leaving at least one blank.
     */
    @Nonnull
    public static List<String> fillBlanks(@Nonnull String answer, @Nonnull String puzzle, @Nonnull Random random) {
        List<String> hints = new ArrayList<>();
        char[] current = puzzle.toCharArray();
        List<Integer> blanks = new ArrayList<>();
        for (int i = 0; i < current.length && i < answer.length(); i++) {
            if (current[i] == BLANK && answer.charAt(i) != BLANK) blanks.add(i);
        }
        while (hints.size() < PREPARED && blanks.size() > 1) {
            int index = blanks.remove(random.nextInt(blanks.size()));
            current[index] = answer.charAt(index);
            hints.add(new String(current));
        }
        return hints;
    }

    private static String mask(String answer, int shown) {
        StringBuilder masked = new StringBuilder();
        int revealed = 0;
        for (char c : answer.toCharArray()) {
            if (c == ' ') {
                masked.append(' ');
            } else if (revealed < shown) {
                masked.append(c);
                revealed++;
            } else {
                masked.append(BLANK);
            }
        }
        return masked.toString();
    }

    private static int countLetters(String answer) {
        int count = 0;
        for (char c : answer.toCharArray()) {
            if (c != ' ') count++;
        }
        return count;
    }
}
