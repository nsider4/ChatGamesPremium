package org.nsider.chatgames.games.round;

import org.nsider.inventorymanager.extras.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Decides whether a chat message answers a round.
 *
 * <p>Spaces are ignored (so "nether portal" matches "netherportal") and one
 * trailing full stop is allowed. List answers compare item by item, in order
 * or not depending on the round.</p>
 */
public final class AnswerChecker {

    public enum Result {
        CORRECT,
        /** Right except for capitalisation, in a case-sensitive game. */
        WRONG_CASE,
        WRONG
    }

    private AnswerChecker() {}

    @Nonnull
    public static Result check(@Nonnull Round round, @Nonnull String input, boolean caseSensitive) {
        String cleaned = clean(input);
        if (cleaned.isEmpty()) return Result.WRONG;

        boolean wrongCase = false;
        for (String answer : round.getAnswers()) {
            if (round.isListAnswer()) {
                if (listsMatch(answer, cleaned, round.isOrderMatters(), caseSensitive)) return Result.CORRECT;
                if (caseSensitive && listsMatch(answer, cleaned, round.isOrderMatters(), false)) wrongCase = true;
            } else {
                String expected = normalize(answer);
                if (caseSensitive ? expected.equals(normalize(cleaned)) : expected.equalsIgnoreCase(normalize(cleaned))) {
                    return Result.CORRECT;
                }
                if (caseSensitive && expected.equalsIgnoreCase(normalize(cleaned))) wrongCase = true;
            }
        }
        return wrongCase ? Result.WRONG_CASE : Result.WRONG;
    }

    private static String clean(String input) {
        String text = input.trim();
        return text.endsWith(".") ? text.substring(0, text.length() - 1).trim() : text;
    }

    private static String normalize(String text) {
        return text.replace(" ", "");
    }

    private static boolean listsMatch(String answer, String input, boolean orderMatters, boolean caseSensitive) {
        List<String> expected = items(answer, caseSensitive);
        List<String> given = items(input, caseSensitive);
        if (expected.size() != given.size()) return false;
        if (!orderMatters) {
            Collections.sort(expected);
            Collections.sort(given);
        }
        return expected.equals(given);
    }

    private static List<String> items(String list, boolean caseSensitive) {
        List<String> items = new ArrayList<>();
        for (String item : list.split(",")) {
            String normalized = normalize(item.trim());
            if (normalized.isEmpty()) continue;
            items.add(caseSensitive ? normalized : normalized.toLowerCase(Locale.ROOT));
        }
        return items;
    }
}
