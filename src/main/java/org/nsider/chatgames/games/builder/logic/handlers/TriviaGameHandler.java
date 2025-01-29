package org.nsider.chatgames.games.builder.logic.handlers;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.games.builder.BuilderUtils;
import org.nsider.chatgames.games.builder.logic.GameHandler;

import java.util.Collection;

public class TriviaGameHandler implements GameHandler<String[]> {
    private final ChatGames plugin;

    public TriviaGameHandler(ChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public String[] handle(String... args) {
        Collection<String> questions = ConfigManager.getWords().getConfigurationSection("trivia.data").getKeys(false);

        String selected = "";
        String toReplace = "";

        if (args.length > 0 && questions.contains(args[0])) {
            selected = BuilderUtils.buildStringFromList(ConfigManager.getWords().getStringList("trivia.data." + args[0] + ".answers"));
            toReplace = ConfigManager.getWords().getString("trivia.data." + args[0] + ".question");
        } else if (!questions.isEmpty()) {
            String questionId = BuilderUtils.getRandomValue(plugin, questions);
            selected = BuilderUtils.buildStringFromList(ConfigManager.getWords().getStringList("trivia.data." + questionId + ".answers"));
            toReplace = ConfigManager.getWords().getString("trivia.data." + questionId + ".question");
        }

        return new String[]{selected, toReplace};
    }
}
