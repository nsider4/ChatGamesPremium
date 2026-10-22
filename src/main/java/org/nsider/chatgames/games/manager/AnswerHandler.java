package org.nsider.chatgames.games.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.games.Game;
import org.nsider.chatgames.games.round.AnswerChecker;
import org.nsider.chatgames.games.round.GameSettings;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.api.PlayerResult;
import org.nsider.chatgames.api.event.ChatGamesAnswerEvent;
import org.nsider.chatgames.api.event.ChatGamesWinEvent;
import org.nsider.chatgames.managers.TopManager;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.chatgames.progression.Activity;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;

/**
 * Checks answers and pays out wins. Chat arrives on async threads (and on Folia,
 * clicks arrive on region threads), so everything here is thread-safe: places are
 * claimed atomically, player data is synchronized, and the end of the game is
 * handed to the main/global thread.
 */
public final class AnswerHandler {

    private final ChatGames plugin;

    AnswerHandler(ChatGames plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles a chat message.
     *
     * @return true if it won a place (the caller may then hide the message)
     */
    public boolean onChat(@Nonnull Player player, @Nonnull String message) {
        Game game = plugin.getGameManager().getGame();
        // The clickable key is only reachable by clicking, never by typing
        if (game == null || game.getGameType() == GameType.CLICKABLE) return false;
        return submit(player, game, message);
    }

    /**
     * Handles {@code /chatgames <key>} sent by clicking the Clickable game's text.
     *
     * @return true if the argument was the current key (whether or not it still won a place)
     */
    public boolean onClick(@Nonnull Player player, @Nonnull String key) {
        if (plugin.getGlobalGameManager().isRunning()) return plugin.getGlobalGameManager().onClick(player, key);
        Game game = plugin.getGameManager().getGame();
        if (game == null || game.getGameType() != GameType.CLICKABLE || !game.getAnswer().equals(key)) return false;
        submit(player, game, key);
        return true;
    }

    private boolean submit(Player player, Game game, String message) {
        Round round = game.getRound();
        GameSettings settings = game.getSettings();
        if (round == null || settings == null || game.isFinished()) return false;
        if (game.hasResult(player.getUniqueId())) return false;
        GameManager manager = plugin.getGameManager();
        if (!manager.gamesIsToggled(player) || !manager.worldEnabled(player)) return false;

        AnswerChecker.Result result = AnswerChecker.check(round, message, settings.isCaseSensitive());
        if (result == AnswerChecker.Result.WRONG_CASE) {
            MessageUtils.sendMessage(player, ConfigManager.getMessages().get(MessageKey.WRONG_CASE));
            GameSoundManager.playGameSound(player, "wrong-case");
            return false;
        }
        if (result != AnswerChecker.Result.CORRECT) return false;

        long millis = game.getElapsedMillis();
        if (millis < settings.getMinAnswerMillis()) {
            // Faster than a person can read and type: most likely a macro or chat bot
            plugin.getLogger().warning(player.getName() + " answered " + game.getGameConfigName() + " in " + millis
                    + " ms (minimum " + settings.getMinAnswerMillis() + " ms); the answer was ignored.");
            return false;
        }

        ChatGamesAnswerEvent answered = new ChatGamesAnswerEvent(player, game.getGameType(), message, millis);
        Bukkit.getPluginManager().callEvent(answered);
        if (answered.isCancelled()) return false;

        PlayerResult placed = game.claimPlace(player.getUniqueId());
        if (placed == null) return false; // every place was taken a moment earlier

        award(player, game, settings, placed);
        if (game.isFull()) {
            plugin.getScheduler().global(() -> manager.conclude(game));
        } else {
            String waiting = ConfigManager.getMessagesConfig().getString("waiting_for_other_winners", "");
            if (!waiting.isEmpty()) MessageUtils.sendMessage(player, waiting);
        }
        return true;
    }

    private void award(Player player, Game game, GameSettings settings, PlayerResult placed) {
        GameType type = game.getGameType();
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data != null) {
            data.incrementStat(Stat.WINS, 1);
            data.incrementStat(Stat.wins(type), 1);
        }
        ChatGamesWinEvent win = new ChatGamesWinEvent(player, type, placed.getPlace(), placed.getTime(), settings.getPoints());
        Bukkit.getPluginManager().callEvent(win);
        if (win.getPoints() > 0) {
            TopManager.addPoints(player, win.getPoints());
        }
        if (ConfigValue.getBoolean(ConfigPaths.FASTEST_TYPERS_TOP)) {
            TopManager.recordTime(player, type, placed.getTime());
        }
        if (win.isGiveRewards()) plugin.getGameUtils().giveRewards(type.getConfigKey(), player, placed.getPlace());
        plugin.getProgressionManager().record(player, Activity.win(type, placed.getTime(), win.getPoints()));
    }
}
