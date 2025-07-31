package org.nsider.chatgames.games.builder.algorithms;

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