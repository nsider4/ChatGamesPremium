package org.nsider.chatgames.games.round.generators;

import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.games.round.Hints;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.games.round.RoundContext;
import org.nsider.chatgames.games.round.RoundGenerator;

import org.nsider.inventorymanager.extras.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Math, in two modes:
 * <ul>
 *   <li><b>solve</b>: {@code 7 x 3 - 4} → 17</li>
 *   <li><b>missing-value</b>: {@code 3 + ? - 4 = 10} → 11</li>
 * </ul>
 * Every result is a whole number: divisions are only generated when they divide exactly.
 * An admin may pass an expression to solve, e.g. {@code 12 x 3 + 1}.
 */
public final class MathRoundGenerator implements RoundGenerator {

    private static final int ATTEMPTS = 200;

    @Nonnull
    @Override
    public Round generate(@Nonnull RoundContext context, @Nonnull String... args) {
        if (args.length > 0) {
            String expression = String.join(" ", args);
            Expression parsed = Expression.parse(expression);
            return Round.builder(GameType.MATH, String.valueOf(parsed.evaluate()))
                    .display(expression).build();
        }

        List<Character> operators = new ArrayList<>();
        for (String op : context.getSettings().getStringList("operators")) {
            Character symbol = Expression.operator(op);
            if (symbol != null && !operators.contains(symbol)) operators.add(symbol);
        }
        if (operators.isEmpty()) operators.add('+');

        int difficulty = Math.max(1, Math.min(5, context.getSettings().getInt("difficulty", 2)));
        int operations = Math.max(1, Math.min(5, context.getSettings().getInt("operations", 1)));
        Expression expression = Expression.random(context.getRandom(), operators, operations, difficulty);

        List<String> modes = context.getSettings().getStringList("modes");
        String mode = modes.isEmpty() ? "solve" : modes.get(context.getRandom().nextInt(modes.size()));

        if (mode.equalsIgnoreCase("missing-value")) {
            int hidden = context.getRandom().nextInt(expression.numbers.length);
            String answer = String.valueOf(expression.numbers[hidden]);
            return Round.builder(GameType.MATH, answer)
                    .display(expression.format(hidden) + " = " + expression.evaluate())
                    .hints(Hints.revealFromStart(answer))
                    .build();
        }
        String answer = String.valueOf(expression.evaluate());
        return Round.builder(GameType.MATH, answer)
                .display(expression.format(-1))
                .hints(Hints.revealFromStart(answer))
                .build();
    }

    /**
     * Whole numbers joined by + - x /, evaluated with the usual precedence.
     */
    static final class Expression {
        final long[] numbers;
        final char[] operators;

        Expression(long[] numbers, char[] operators) {
            this.numbers = numbers;
            this.operators = operators;
        }

        /** Maps the accepted spellings of an operator to its canonical symbol. */
        static Character operator(String text) {
            switch (text.trim()) {
                case "+": return '+';
                case "-": return '-';
                case "x": case "X": case "*": case "×": return 'x';
                case "/": case "÷": return '/';
                default: return null;
            }
        }

        static Expression random(Random random, List<Character> allowed, int operations, int difficulty) {
            long max = (long) Math.pow(10, difficulty) - 1;
            for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                long[] numbers = new long[operations + 1];
                char[] operators = new char[operations];
                for (int i = 0; i < operations; i++) {
                    operators[i] = allowed.get(random.nextInt(allowed.size()));
                }
                for (int i = 0; i < numbers.length; i++) {
                    numbers[i] = 1 + (long) (random.nextDouble() * max);
                }
                // Make divisions likely to come out exact: dividend = divisor x something
                for (int i = 0; i < operations; i++) {
                    if (operators[i] == '/') {
                        long divisor = 2 + random.nextInt((int) Math.min(11, Math.max(1, max - 1)));
                        numbers[i + 1] = divisor;
                        numbers[i] = divisor * (1 + (long) (random.nextDouble() * Math.max(1, max / divisor)));
                    }
                }
                Expression expression = new Expression(numbers, operators);
                if (expression.tryEvaluate() != null) return expression;
            }
            // Very unlikely: give up on division rather than show a fraction
            long[] numbers = new long[operations + 1];
            char[] operators = new char[operations];
            for (int i = 0; i < numbers.length; i++) numbers[i] = 1 + random.nextInt((int) Math.min(max, 99));
            for (int i = 0; i < operations; i++) operators[i] = '+';
            return new Expression(numbers, operators);
        }

        /**
         * @throws IllegalArgumentException if the text isn't numbers separated by operators,
         *                                  or a division isn't exact
         */
        static Expression parse(String text) {
            List<Long> numbers = new ArrayList<>();
            List<Character> operators = new ArrayList<>();
            StringBuilder digits = new StringBuilder();
            for (char c : (text + " ").toCharArray()) {
                if (Character.isDigit(c)) {
                    digits.append(c);
                    continue;
                }
                if (digits.length() > 0) {
                    numbers.add(Long.parseLong(digits.toString()));
                    digits.setLength(0);
                }
                if (Character.isWhitespace(c)) continue;
                Character op = operator(String.valueOf(c));
                if (op == null) throw new IllegalArgumentException("Unexpected '" + c + "' in " + text);
                operators.add(op);
            }
            if (numbers.isEmpty() || numbers.size() != operators.size() + 1) {
                throw new IllegalArgumentException("Not an expression: " + text);
            }
            long[] n = new long[numbers.size()];
            for (int i = 0; i < n.length; i++) n[i] = numbers.get(i);
            char[] o = new char[operators.size()];
            for (int i = 0; i < o.length; i++) o[i] = operators.get(i);
            Expression expression = new Expression(n, o);
            if (expression.tryEvaluate() == null) {
                throw new IllegalArgumentException("The result of " + text + " isn't a whole number");
            }
            return expression;
        }

        long evaluate() {
            Long value = tryEvaluate();
            if (value == null) throw new IllegalStateException("inexact division");
            return value;
        }

        /**
         * @return the value, or null if a division doesn't come out exact (or divides by zero)
         */
        Long tryEvaluate() {
            List<Long> terms = new ArrayList<>();
            List<Character> signs = new ArrayList<>();
            long term = numbers[0];
            for (int i = 0; i < operators.length; i++) {
                long next = numbers[i + 1];
                char op = operators[i];
                if (op == 'x') {
                    term *= next;
                } else if (op == '/') {
                    if (next == 0 || term % next != 0) return null;
                    term /= next;
                } else {
                    terms.add(term);
                    signs.add(op);
                    term = next;
                }
            }
            terms.add(term);
            long result = terms.get(0);
            for (int i = 0; i < signs.size(); i++) {
                result = signs.get(i) == '+' ? result + terms.get(i + 1) : result - terms.get(i + 1);
            }
            return result;
        }

        /** @param hidden index of the number to show as "?", or -1 */
        String format(int hidden) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < numbers.length; i++) {
                if (i > 0) text.append(' ').append(operators[i - 1]).append(' ');
                text.append(i == hidden ? "?" : String.valueOf(numbers[i]));
            }
            return text.toString();
        }
    }
}
