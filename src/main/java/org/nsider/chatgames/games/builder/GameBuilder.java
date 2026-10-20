package org.nsider.chatgames.games.builder;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.events.ChatGamesStartEvent;
import org.nsider.chatgames.games.Game;

import java.util.Random;

public class GameBuilder {
    private final ChatGames plugin;
    private final Object gameLock = new Object();
    private final Random random = new Random();

    public GameBuilder(ChatGames instance) {
        plugin = instance;
    }

    public void startGame(GameType type, String... args) {
        String selected = "";
        String toReplace = "";

        synchronized (gameLock) {
            Object result = BuilderUtils.handleGameType(plugin, type, args);

            if (result instanceof String[]) {
                String[] results = (String[]) result;
                if (results.length > 0) {
                    selected = results[0];
                    if (results.length > 1) {
                        toReplace = results[1];
                    }
                }
            } else if (result instanceof String) {
                selected = (String) result;
            }
        }

        if (type != GameType.MEMORIZE) {
            initGame(type, selected);
            plugin.getExpirationTask().scheduleExpirationTask(plugin.getGameManager().getGame().getGameConfigName());
        }

        if (!BuilderUtils.isSpecialGame(type) && !toReplace.isEmpty()) {
            plugin.getGameManager().sendGameStartedMessage(plugin.getGameManager().getGame().getGameConfigName(), "%selection%", toReplace);
        }
    }

    public void initGame(GameType type, String selected) {
        int maxWinners = plugin.getGameConfigManager().getGameWinners(type.getConfigKey());
        plugin.getGameManager().setGame(new Game(type, selected, maxWinners));
        plugin.setInGame(true);
        ChatGamesStartEvent startEvent = new ChatGamesStartEvent(plugin, type);
        plugin.getScheduler().global(() -> plugin.getServer().getPluginManager().callEvent(startEvent));
    }
}
