package org.nsider.chatgames.tasks;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.events.ChatGamesEndEvent;
import org.nsider.chatgames.games.Game;
import org.nsider.chatgames.utils.management.TaskUtils;
import org.nsider.inventorymanager.extras.Nonnull;

/**
 * Handles the expiration logic for games and races.
 */
public class ExpirationTask {

    @Nonnull
    private final ChatGames plugin;
    @Nonnull
    private final TaskUtils taskUtils;

    @Getter
    private BukkitTask task;

    /**
     * Constructs a new {@code ExpirationTask}.
     *
     * @param instance The plugin instance. Must not be null.
     */
    public ExpirationTask(@Nonnull ChatGames instance) {
        this.plugin = instance;
        this.taskUtils = new TaskUtils(plugin);
    }

    /**
     * Schedules a task to handle game or race expiration after a specified time.
     *
     * @param game The name of the game to monitor for expiration.
     */
    public void scheduleExpirationTask(@Nonnull String game) {
        long timeToCompleteTicks = getTimeToCompleteTicks(game);

        task = taskUtils.runAsyncTask(() -> {
            if (plugin.isInGame()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (plugin.getGameManager().isRace(GameType.valueOf(game.toUpperCase()))) {
                        handleRaceExpiration(game);
                    } else {
                        handleGameExpiration(plugin.getGameManager().getGame());
                    }
                });
            }
        }, timeToCompleteTicks);
    }

    /**
     * Cancels the currently running expiration task.
     */
    public void cancel() {
        taskUtils.cancelTask(task);
    }

    /**
     * Retrieves the time to complete the game or race in ticks.
     *
     * @param game The name of the game.
     * @return The time to complete in ticks.
     */
    private long getTimeToCompleteTicks(@Nonnull String game) {
        int timeInSeconds = plugin.getConfig().isSet(game + ".timeToGuess_seconds")
                ? plugin.getConfig().getInt(game + ".timeToGuess_seconds")
                : plugin.getConfigManager().getChatRaces().getInt(game + ".timeToComplete_seconds");

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
        ChatGamesEndEvent endEvent = new ChatGamesEndEvent(plugin, game.getGameType(), GameHandler.getResults());
        Bukkit.getServer().getPluginManager().callEvent(endEvent);

        // Handle results or expiration message
        if (!GameHandler.getResults().isEmpty()) {
            plugin.getGameManager().sendMessageForWinners();
            GameHandler.clearResults();
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
        plugin.getRaceHandler().removePlayersOnline();
        plugin.getRaceBuilder().sendTimeExpiredToWorlds(game);
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
