package org.nsider.chatgames.games.builder.algorithms;


import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.games.utils.EquationResult;

import java.util.ArrayList;
import java.util.List;

public class SymbolEquationCreator {

    public static EquationResult createSymbolEquation(ChatGames plugin) {
        List<Integer> numbers = new ArrayList<>();
        List<String> symbols = new ArrayList<>();
        List<String> message = new ArrayList<>();
        List<String> variables = plugin.getConfig().getStringList("variable.data");

        populateSymbolsAndNumbers(numbers, symbols, message, variables);

        int result = generateResult(plugin);
        int value1 = numbers.get(0);
        int value2 = numbers.get(1);

        message.add(formatFinalMessage(plugin, symbols, result));

        return new EquationResult(message, result - value1 - value2);
    }

    private static void populateSymbolsAndNumbers(List<Integer> numbers, List<String> symbols, List<String> message, List<String> variables) {
        while (numbers.size() < 2) {
            String[] variableInfo = getRandomVariableInfo(variables);
            String symbol = variableInfo[0];
            int value = Integer.parseInt(variableInfo[1]);

            if (!numbers.contains(value)) {
                symbols.add(symbol);
                numbers.add(value);
                String format = formatSymbolMessage(symbol, value);
                message.add(format);
            }
        }
    }

    private static String[] getRandomVariableInfo(List<String> variables) {
        int randomIndex = (int) (Math.random() * variables.size());
        return variables.get(randomIndex).split(";");
    }

    private static String formatSymbolMessage(String symbol, int value) {
        String format = getTimesReplaced(ConfigManager.getMessagesConfig().getString("variable.line1and2-format"), symbol);
        int additionResult = value * Integer.parseInt(format.split(";")[1]);
        return format.split(";")[0] + additionResult;
    }

    private static int generateResult(ChatGames plugin) {
        int min = plugin.getConfig().getInt("variable.result-min");
        int max = plugin.getConfig().getInt("variable.result-max");
        return (int) (Math.random() * (max - min + 1)) + min;
    }

    private static String formatFinalMessage(ChatGames plugin, List<String> symbols, int result) {
        return ConfigManager.getMessagesConfig().getString("variable.line3-format")
                .replaceAll("%symbol_1%", symbols.get(0))
                .replaceAll("%symbol_2%", symbols.get(1))
                .replaceAll("%result%", "" + result)
                .replaceAll("%toGet%", plugin.getConfig().getString("variable.toGet"));
    }

    private static String getTimesReplaced(String message, String symbol) {
        int totalReplaced = 0;

        while (!message.replaceFirst("%symbol%", symbol).equals(message)) {
            message = message.replaceFirst("%symbol%", symbol);
            totalReplaced++;
        }

        return message + ";" + totalReplaced;
    }
}

