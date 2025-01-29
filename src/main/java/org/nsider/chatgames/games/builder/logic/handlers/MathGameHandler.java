package org.nsider.chatgames.games.builder.logic.handlers;


import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.games.builder.algorithms.MathQuestionGenerator;
import org.nsider.chatgames.games.builder.logic.GameHandler;

public class MathGameHandler implements GameHandler<String[]> {
    private final ChatGames plugin;

    public MathGameHandler(ChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public String[] handle(String... args) {
        String selected = "", toReplace = "";
        try {
            if (args.length > 0) {
                selected = String.valueOf(MathQuestionGenerator.evaluateMathExpression(args[0]));
                toReplace = args[0];
            } else {
                String[] question = plugin.getGameAlgorithms().randomMathQuestion().split(";");
                selected = question[1];
                toReplace = question[0];
            }
        } catch (Exception e) {
            return null;
        }

        return new String[]{selected, toReplace};
    }
}
