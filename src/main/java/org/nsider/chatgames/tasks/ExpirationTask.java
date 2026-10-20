package org.nsider.chatgames.tasks;

import lombok.Getter;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.races.RaceHandler;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;

import org.nsider.inventorymanager.extras.Nonnull;
import java.util.Locale;

/**
 * Ends a race when its time limit runs out. Chat games schedule their own time
 * limit (see {@link org.nsider.chatgames.games.builder.GameBuilder}).
 */
public class ExpirationTask {

    @Nonnull
    private final ChatGames plugin;
    @Getter
    private volatile ChatGamesScheduler.Task task;

    public ExpirationTask(@Nonnull ChatGames instance) {
        this.plugin = instance;
    }

    /**
     * @param race the race's config key, e.g. "mine"
     */
    public void scheduleExpirationTask(@Nonnull String race) {
        long seconds;
        try {
            seconds = ConfigValue.getInt(ConfigPaths.valueOf("RACE_" + race.toUpperCase(Locale.ROOT) + "_TIME_LIMIT"));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("No time limit configured for race: " + race);
            return;
        }
        task = plugin.getScheduler().globalLater(() -> {
            if (!plugin.isInGame()) return;
            RaceHandler.removePlayersOnline();
            plugin.getRaceBuilder().getRaceMessageSender().sendTimeExpiredToWorlds(race);
            plugin.getRaceBuilder().endRace();
        }, seconds * 20L);
    }

    public void cancel() {
        ChatGamesScheduler.cancel(task);
    }
}
