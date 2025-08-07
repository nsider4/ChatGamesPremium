package org.nsider.chatgames.games.builder.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class MathQuestionGenerator {
    private static final Random random = new Random();

    public static int evaluateMathExpression(String expression) {
        Stack<Double> numbers = new Stack<>();
        Stack<Character> operations = new Stack<>();

        for (int i = 0; i < expression.length(); i++) {
            char currentChar = expression.charAt(i);

            if (Character.isWhitespace(currentChar)) {
                continue;
            }

            if (Character.isDigit(currentChar) || currentChar == '.') {
                StringBuilder numberBuilder = new StringBuilder();
                while (i < expression.length() && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    numberBuilder.append(expression.charAt(i++));
                }
                numbers.push(Double.parseDouble(numberBuilder.toString()));
                i--; //Step back after the inner loop
            } else if (isOperator(currentChar)) {
                while (!operations.isEmpty() && precedence(currentChar) <= precedence(operations.peek())) {
                    numbers.push(applyOperation(operations.pop(), numbers.pop(), numbers.pop()));
                }
                operations.push(currentChar);
            }
        }

        while (!operations.isEmpty()) {
            numbers.push(applyOperation(operations.pop(), numbers.pop(), numbers.pop()));
        }

        return (int) Math.round(numbers.pop());
    }

    // Generates a random math question based on difficulty and configuration.
    public static String generateRandomMathQuestion(List<String> operators, int difficulty) {
        if (operators == null || operators.isEmpty()) {
            throw new IllegalArgumentException("Operators list cannot be null or empty.");
        }
        if (difficulty < 1) {
            difficulty = 1;
        }

        String operator = operators.get(random.nextInt(operators.size()));
        int[] numbers = generateNumbersForDifficulty(difficulty);

        int answer = calculateAnswer(numbers[0], numbers[1], operator);
        return numbers[0] + " " + operator + " " + numbers[1] + ";" + answer;
    }

    /**
     * Generates an equation with one missing value (represented by 'x') and an integer result.
     *
     * @param numberOfOperations The number of operations to include (e.g., 3 -> a + b - x = result).
     * @return A string array where [0] is the expression with 'x' (e.g., "3 + 4 - x = 10"),
     *         and [1] is the correct value of 'x'.
     */
    public static String[] generateMissingValueEquation(int numberOfOperations) {
        if (numberOfOperations < 1) numberOfOperations = 1;

        String[] operators = {"+", "-", "x", "/"};
        List<String> values = new ArrayList<>();
        List<String> usedOperators = new ArrayList<>();

        Random rand = new Random();
        int xIndex = rand.nextInt(numberOfOperations + 1);
        int result;

        // Fill values with either random integers or 'x'
        for (int i = 0; i <= numberOfOperations; i++) {
            if (i == xIndex) {
                values.add("x");
            } else {
                values.add(String.valueOf(rand.nextInt(9) + 1)); // Values 1-9
            }
        }

        // Fill operators randomly
        for (int i = 0; i < numberOfOperations; i++) {
            usedOperators.add(operators[rand.nextInt(operators.length)]);
        }

        // Try to compute result, replacing 'x' temporarily with 0
        StringBuilder expressionBuilder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            expressionBuilder.append(values.get(i).equals("x") ? "0" : values.get(i));
            if (i < usedOperators.size()) {
                expressionBuilder.append(" ").append(usedOperators.get(i)).append(" ");
            }
        }

        int target = evaluateMathExpression(expressionBuilder.toString());

        // Now compute the real value of x that would make the expression == target
        int xValue = computeMissingValue(values, usedOperators, target, xIndex);

        // Reconstruct final expression with 'x'
        StringBuilder finalExpression = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            finalExpression.append(values.get(i));
            if (i < usedOperators.size()) {
                finalExpression.append(" ").append(usedOperators.get(i)).append(" ");
            }
        }

        return new String[]{finalExpression.toString() + " = " + target, String.valueOf(xValue)};
    }

    /**
     * Computes the correct integer value of x so the expression equals the target.
     */
    private static int computeMissingValue(List<String> values, List<String> operators, int target, int xIndex) {
        for (int trial = -100; trial <= 100; trial++) {
            StringBuilder expr = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                if (i == xIndex) {
                    expr.append(trial);
                } else {
                    expr.append(values.get(i));
                }
                if (i < operators.size()) {
                    expr.append(" ").append(operators.get(i)).append(" ");
                }
            }

            try {
                if (evaluateMathExpression(expr.toString()) == target) {
                    return trial;
                }
            } catch (Exception ignored) {}
        }
        return 0;
    }


    // Helper method to generate numbers based on difficulty level.
    private static int[] generateNumbersForDifficulty(int difficulty) {
        int range = (int) Math.pow(10, difficulty);
        int num1 = random.nextInt(range);
        int num2 = random.nextInt(range);
        return new int[]{num1, num2};
    }

    // Helper method to calculate the result of an operation.
    private static int calculateAnswer(int num1, int num2, String operator) {
        switch (operator) {
            case "+":
                return num1 + num2;
            case "-":
                return num1 - num2;
            case "x":
                return num1 * num2;
            case "/":
                if (num2 == 0) {
                    num2 = 1; // Avoid division by zero
                }
                return num1 / num2;
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }

    // Applies a mathematical operation on two operands.
    private static double applyOperation(char operation, double b, double a) {
        switch (operation) {
            case '+':
                return a + b;
            case '-':
                return a - b;
            case 'x':
                return a * b;
            case '/':
                if (b == 0) {
                    throw new UnsupportedOperationException("Cannot divide by zero.");
                }
                return a / b;
            default:
                throw new IllegalArgumentException("Unknown operator: " + operation);
        }
    }

    // Determines precedence of operators.
    private static int precedence(char operation) {
        switch (operation) {
            case '+':
            case '-':
                return 1;
            case 'x':
            case '/':
                return 2;
            default:
                return -1;
        }
    }

    // Checks if a character is a valid operator.
    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == 'x' || c == '/';
    }
}