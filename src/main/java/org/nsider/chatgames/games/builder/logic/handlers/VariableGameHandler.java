package org.nsider.chatgames.games.builder.logic.handlers;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.games.builder.logic.GameHandler;
import org.nsider.chatgames.games.utils.EquationResult;

public class VariableGameHandler implements GameHandler<String> {
    private final ChatGames plugin;

    public VariableGameHandler(ChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public String handle(String... args) {
        EquationResult result = plugin.getGameAlgorithms().createSymbolEquation();
        String selected = String.valueOf(result.getAnswer());

        plugin.getGameManager().sendVariableMessages(
                result.getMessageLines(),
                ConfigManager.getMessagesConfig().getStringList("variable.gameStartAnnouncement"),
                "%timeToGuess%", String.valueOf(ConfigValue.getInt(ConfigPaths.VARIABLE_TIME_TO_GUESS)),
                "%symbol%", ConfigValue.getString(ConfigPaths.VARIABLE_TO_GET_SYMBOL)
        );

        return selected;
    }
}
