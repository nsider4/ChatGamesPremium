package org.nsider.chatgames.games.builder;

import org.bukkit.Bukkit;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.events.ChatGamesStartEvent;
import org.nsider.chatgames.games.Game;
import org.nsider.chatgames.utils.management.TimeUtils;

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

        if (type != GameType.SHOPPINGLIST) {
            initGame(type, selected);
            plugin.getExpirationTask().timeExpiredTask(plugin.getGameManager().getGame().getGameConfigName());
        }

        if (!BuilderUtils.isSpecialGame(type) && !toReplace.isEmpty()) {
            plugin.getGameManager().sendGameStartedMessage(plugin.getGameManager().getGame().getGameConfigName(), "%selection%", toReplace);
        }
    }

    public void initGame(GameType type, String selected) {
        plugin.getGameManager().setGame(new Game(type, selected));
        plugin.getGameManager().setCurrentGame(plugin.getGameManager().getGame().getGameConfigName());
        plugin.setInGame(true);
        ChatGamesStartEvent startEvent = new ChatGamesStartEvent(plugin, type);
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getServer().getPluginManager().callEvent(startEvent));
        TimeUtils.startTimer();
    }
}
