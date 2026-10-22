package org.nsider.chatgames.games.round.generators;

import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.games.round.Hints;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.games.round.RoundContext;
import org.nsider.chatgames.games.round.RoundGenerator;

import org.nsider.inventorymanager.extras.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Complete the number sequence: {@code 3, 6, 12, 24, ?} → 48.
 *
 * <p>Difficulty (1–3) decides which of the configured patterns may appear and
 * how large the numbers get. If none of the configured patterns suit the
 * difficulty, the configured ones are used anyway.</p>
 */
public final class SequenceRoundGenerator implements RoundGenerator {

    enum Pattern {
        /** a, a+d, a+2d */
        ADD(1),
        /** a, a·r, a·r² */
        MULTIPLY(1),
        /** n², (n+1)², (n+2)² */
        SQUARES(2),
        /** steps alternate between two values: +5, -2, +5, -2 */
        ALTERNATING(2),
        /** each term is the sum of the two before it */
        FIBONACCI(3);

        final int minDifficulty;

        Pattern(int minDifficulty) {
            this.minDifficulty = minDifficulty;
        }

        static Pattern from(String name) {
            try {
                return valueOf(name.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    @Nonnull
    @Override
    public Round generate(@Nonnull RoundContext context, @Nonnull String... args) {
        int difficulty = Math.max(1, Math.min(3, context.getSettings().getInt("difficulty", 2)));
        int shown = Math.max(4, Math.min(8, context.getSettings().getInt("shown-terms", 5)));

        List<Pattern> configured = new ArrayList<>();
        for (String name : context.getSettings().getStringList("patterns")) {
            Pattern pattern = Pattern.from(name);
            if (pattern != null && !configured.contains(pattern)) configured.add(pattern);
        }
        if (configured.isEmpty()) {
            for (Pattern pattern : Pattern.values()) configured.add(pattern);
        }
        List<Pattern> allowed = new ArrayList<>();
        for (Pattern pattern : configured) {
            if (pattern.minDifficulty <= difficulty) allowed.add(pattern);
        }
        if (allowed.isEmpty()) allowed = configured;

        Pattern pattern = allowed.get(context.getRandom().nextInt(allowed.size()));
        long[] terms = terms(pattern, shown + 1, difficulty, context.getRandom());

        StringBuilder display = new StringBuilder();
        for (int i = 0; i < shown; i++) display.append(terms[i]).append(", ");
        display.append('?');
        String answer = String.valueOf(terms[shown]);
        return Round.builder(GameType.SEQUENCE, answer)
                .display(display.toString())
                .placeholder("%pattern%", pattern.name().toLowerCase(Locale.ROOT))
                .hints(Hints.revealFromStart(answer))
                .build();
    }

    static long[] terms(Pattern pattern, int count, int difficulty, Random random) {
        long[] terms = new long[count];
        switch (pattern) {
            case ADD: {
                long start = 1 + random.nextInt(10 * difficulty);
                long step = 1 + random.nextInt(3 * difficulty);
                // Hard sequences may count down
                if (difficulty >= 3 && random.nextBoolean()) step = -step;
                for (int i = 0; i < count; i++) terms[i] = start + step * i;
                break;
            }
            case MULTIPLY: {
                long ratio = 2 + random.nextInt(difficulty);
                long start = 1 + random.nextInt(2 + difficulty);
                for (int i = 0; i < count; i++) terms[i] = i == 0 ? start : terms[i - 1] * ratio;
                break;
            }
            case SQUARES: {
                long first = 1 + random.nextInt(5 * difficulty);
                for (int i = 0; i < count; i++) terms[i] = (first + i) * (first + i);
                break;
            }
            case ALTERNATING: {
                long up = 1 + random.nextInt(5 * difficulty);
                long other;
                do {
                    other = random.nextInt(10 * difficulty + 1) - 5L * difficulty;
                } while (other == 0 || other == up);
                terms[0] = 1 + random.nextInt(10 * difficulty);
                for (int i = 1; i < count; i++) terms[i] = terms[i - 1] + (i % 2 == 1 ? up : other);
                break;
            }
            case FIBONACCI:
            default: {
                terms[0] = 1 + random.nextInt(3 * difficulty);
                terms[1] = terms[0] + random.nextInt(3 * difficulty);
                for (int i = 2; i < count; i++) terms[i] = terms[i - 1] + terms[i - 2];
                break;
            }
        }
        return terms;
    }
}
