package org.nsider.chatgames.tasks;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.events.ChatGamesEndEvent;
import org.nsider.chatgames.games.Game;
import org.nsider.chatgames.games.manager.GameUtility;
import org.nsider.chatgames.races.RaceHandler;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.inventorymanager.extras.Nonnull;

/**
 * Handles the expiration logic for games and races.
 */
public class ExpirationTask {

    @Nonnull
    private final ChatGames plugin;

    @Getter
    private volatile ChatGamesScheduler.Task task;

    /**
     * Constructs a new {@code ExpirationTask}.
     *
     * @param instance The plugin instance. Must not be null.
     */
    public ExpirationTask(@Nonnull ChatGames instance) {
        this.plugin = instance;
    }

    /**
     * Schedules a task to handle game or race expiration after a specified time.
     *
     * @param game The name of the game to monitor for expiration.
     */
    public void scheduleExpirationTask(@Nonnull String game) {
        long timeToCompleteTicks = getTimeToCompleteTicks(game);

        GameType type = GameType.fromName(game);
        task = plugin.getScheduler().globalLater(() -> {
            if (!plugin.isInGame()) return;
            if (type != null && GameUtility.isRace(type)) {
                handleRaceExpiration(game);
                return;
            }
            Game current = plugin.getGameManager().getGame();
            // A last winner may have ended the game in the same tick
            if (current != null && current.finish()) {
                handleGameExpiration(current);
            }
        }, timeToCompleteTicks);
    }

    /**
     * Cancels the currently running expiration task.
     */
    public void cancel() {
        ChatGamesScheduler.cancel(task);
    }

    /**
     * Retrieves the time to complete the game or race in ticks.
     *
     * @param game The name of the game.
     * @return The time to complete in ticks.
     */
    private long getTimeToCompleteTicks(@Nonnull String game) {
        String gameKey = game.toUpperCase();
        long timeInSeconds = 0;

        try {
            ConfigPaths timePath = ConfigPaths.valueOf(gameKey + "_TIME_TO_GUESS");
            timeInSeconds = ConfigValue.getInt(timePath);
        } catch (IllegalArgumentException e) {
            try {
                ConfigPaths racePath = ConfigPaths.valueOf("RACE_" + gameKey + "_TIME_LIMIT");
                timeInSeconds = ConfigValue.getInt(racePath);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("No time configuration found for game: " + game);
            }
        }

        return timeInSeconds * 20L;
    }

    /**
     * Handles expiration logic for a non-race game.
     *
     * @param game The {@link Game} instance representing the active game.
     */
    private void handleGameExpiration(@Nonnull Game game) {
        String selectedAnswer = extractSelectedAnswer(game.getAnswer());

        // Trigger end event
        ChatGamesEndEvent endEvent = new ChatGamesEndEvent(game.getGameType(), game.getAnswer(), game.getResultsByPlace());
        Bukkit.getServer().getPluginManager().callEvent(endEvent);

        // Handle results or expiration message
        if (game.hasWinners()) {
            plugin.getGameManager().sendWinnerMessages();
        } else {
            plugin.getGameManager().sendTimeExpiredToWorlds(selectedAnswer, game.getGameConfigName());
        }

        plugin.getGameManager().endGame();
    }

    /**
     * Handles expiration logic for a race.
     *
     * @param game The name of the race.
     */
    private void handleRaceExpiration(@Nonnull String game) {
        RaceHandler.removePlayersOnline();
        plugin.getRaceBuilder().getRaceMessageSender().sendTimeExpiredToWorlds(game);
        plugin.getRaceBuilder().endRace();
    }

    /**
     * Extracts the selected answer from the game's answer string, if formatted with "•".
     *
     * @param answer The raw answer string.
     * @return The extracted answer, or the original answer if no special format is present.
     */
    private String extractSelectedAnswer(@Nonnull String answer) {
        return answer.contains("•") ? answer.split("•")[1] : answer;
    }
}