package org.nsider.chatgames.games.builder.logic.handlers;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.games.builder.algorithms.MathQuestionGenerator;
import org.nsider.chatgames.games.builder.logic.GameHandler;

/**
 * Game handler for math equations where the player must guess the missing value (x).
 */
public class SequenceGameHandler implements GameHandler<String[]> {
    private final ChatGames plugin;

    public SequenceGameHandler(ChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public String[] handle(String... args) {
        try {
            int operations = 2;
            if (args.length > 0) {
                try {
                    operations = Integer.parseInt(args[0]);
                } catch (NumberFormatException ignored) {}
            }

            String[] question = MathQuestionGenerator.generateMissingValueEquation(operations);
            return new String[]{question[1], question[0]};
        } catch (Exception e) {
            return null;
        }
    }
}
