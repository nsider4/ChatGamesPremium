package org.nsider.chatgames.games.round.generators;

import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.games.round.RoundContext;
import org.nsider.chatgames.games.round.RoundGenerator;

import org.nsider.inventorymanager.extras.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Symbol equations. Two lines give away the value of two symbols, the third asks for a third:
 * <pre>
 *   ✯ + ✯ + ✯ = 30
 *   ♛ + ♛ + ♛ = 45
 *   ✯ + ♛ + ✗ = 40   → ✗ = 15
 * </pre>
 */
public final class VariableRoundGenerator implements RoundGenerator {

    @Nonnull
    @Override
    public Round generate(@Nonnull RoundContext context, @Nonnull String... args) {
        List<String[]> symbols = new ArrayList<>();
        for (String entry : context.getSettings().getStringList("data")) {
            String[] parts = entry.split(";");
            Integer value = parts.length == 2 ? SimpleRoundGenerators.parseInt(parts[1]) : null;
            if (value == null) continue;
            boolean duplicate = false;
            for (String[] known : symbols) {
                if (known[1].equals(String.valueOf(value))) duplicate = true;
            }
            if (!duplicate) symbols.add(new String[]{parts[0], String.valueOf(value)});
        }
        if (symbols.size() < 2) {
            throw new IllegalStateException("variable.data in games.yml needs at least two symbols with different values");
        }

        String[] first = symbols.remove(context.getRandom().nextInt(symbols.size()));
        String[] second = symbols.get(context.getRandom().nextInt(symbols.size()));
        int value1 = Integer.parseInt(first[1]);
        int value2 = Integer.parseInt(second[1]);

        // The unknown is the difference, so keep the result above the two known values
        int min = context.getSettings().getInt("result-min", 20);
        int max = context.getSettings().getInt("result-max", 100);
        int result = context.between(Math.max(min, value1 + value2 + 1), Math.max(max, value1 + value2 + 1));
        int answer = result - value1 - value2;

        String toGet = context.getSettings().getString("toGet", "✗");
        String repeated = context.getMessages().getString("line1and2-format", "&c%symbol%&f + &c%symbol%&f + &c%symbol%&f = &e");
        String last = context.getMessages().getString("line3-format", "&c%symbol_1%&f + &c%symbol_2%&f + &b%toGet% &f= &e%result%");

        return Round.builder(GameType.VARIABLE, String.valueOf(answer))
                .placeholder("%line_1%", repeatedLine(repeated, first[0], value1))
                .placeholder("%line_2%", repeatedLine(repeated, second[0], value2))
                .placeholder("%line_3%", last.replace("%symbol_1%", first[0]).replace("%symbol_2%", second[0])
                        .replace("%toGet%", toGet).replace("%result%", String.valueOf(result)))
                .placeholder("%symbol%", toGet)
                .build();
    }

    /** Fills every %symbol% and appends the total: the symbol's value times the times it appears. */
    private static String repeatedLine(String format, String symbol, int value) {
        int count = 0;
        int index = 0;
        while ((index = format.indexOf("%symbol%", index)) != -1) {
            count++;
            index += "%symbol%".length();
        }
        return format.replace("%symbol%", symbol) + (value * Math.max(1, count));
    }
}
