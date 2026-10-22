package org.nsider.chatgames.api;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * ChatGames' API. Get it with {@link #get()} once ChatGames has enabled: add
 * {@code depend: [ChatGames-Premium]} (or softdepend) to your plugin.yml and call it from your
 * {@code onEnable}.
 *
 * <p>Games, reward actions and challenge types you register are removed when your plugin
 * disables. Unless a method says otherwise, call it on the main thread (the global region on Folia).</p>
 *
 * <p>ChatGames also fires events for everything that happens in a game; see the
 * {@code org.nsider.chatgames.api.event} package.</p>
 */
public interface ChatGamesAPI {

    /**
     * @throws IllegalStateException if ChatGames isn't enabled (yet)
     */
    static ChatGamesAPI get() {
        ChatGamesAPI api = Bukkit.getServicesManager().load(ChatGamesAPI.class);
        if (api == null) throw new IllegalStateException("ChatGames isn't enabled; add it to depend or softdepend in plugin.yml");
        return api;
    }

    // Games

    /**
     * Adds a chat game. Its sections are added to games.yml, messages.yml and rewards.yml if they
     * aren't there yet, and it joins the rotation if it is enabled in games.yml.
     *
     * @throws IllegalArgumentException if a game with that id exists or the id is a ChatGames command
     */
    GameType registerGame(Plugin owner, ChatGame game);

    /**
     * Adds a race. Its sections are added to races.yml and messages.yml if they aren't there yet.
     *
     * @throws IllegalArgumentException if a game with that id exists or the id is a ChatGames command
     */
    GameType registerRace(Plugin owner, RaceGame race);

    /**
     * Removes a game a plugin added, stopping it if it is running. Its config sections and
     * players' stats are kept.
     *
     * @return false for built-in games and games that aren't registered
     */
    boolean unregister(GameType game);

    /** @return the game with that id (built-in or added), or null */
    GameType getGame(String id);

    /** @return every game, built-in and added */
    List<GameType> getGames();

    // Running games

    /**
     * Starts a game or race now, as {@code /chatgames <game> [args]} would.
     *
     * @param args optional values, e.g. a race's target and amount
     * @return false if a game is already running, the game can't start or a plugin cancelled it
     */
    boolean startGame(GameType game, String... args);

    /**
     * Ends the running game or race with no winner, as {@code /chatgames stop} does.
     *
     * @return false if nothing was running
     */
    boolean stopGame();

    /** @return the game or race running now, or null. Any thread. */
    GameType getCurrentGame();

    /**
     * Counts something a player did towards a running custom race. Does nothing unless that race
     * is running, the target matches its target and the player can play. Any thread.
     *
     * @param target the target it counts for, without case; null counts for any target
     */
    void addRaceProgress(Player player, GameType race, String target, int amount);

    // Rewards and challenges

    /**
     * Adds a reward action, used in reward lines as {@code [name] argument}.
     *
     * @param name lower-case letters, digits, _ and -, e.g. "myplugin_crate"; not one of ChatGames' own
     * @throws IllegalArgumentException if the name is malformed or taken
     */
    void registerRewardAction(Plugin owner, String name, RewardAction action);

    /**
     * Adds a challenge type that challenges.yml can use as {@code type: <id>}. Progress comes
     * from {@link #addChallengeProgress}.
     *
     * @param id lower-case letters, digits and _, e.g. "boss_kills"; not one of ChatGames' own types
     * @throws IllegalArgumentException if the id is malformed or taken
     */
    void registerChallengeType(Plugin owner, String id);

    /**
     * Adds progress to the player's active challenges of a type you registered. Any thread.
     */
    void addChallengeProgress(Player player, String type, int amount);

    // Player data

    /** @return the online player's numbers, or null while their data is still loading. Any thread. */
    PlayerStats getStats(Player player);

    /**
     * Gives points (negative to take them away). Milestones run as for points won in games.
     * Works for offline players too; their data is loaded, changed and saved. Any thread.
     */
    void addPoints(OfflinePlayer player, int amount);

    /**
     * @return the top players by points, at most {@code limit}, from the cached leaderboard (rebuilt
     *         every refresh-seconds in config.yml and after games). Any thread.
     */
    List<TopEntry> getTopPoints(int limit);

    /**
     * @param game a game, or null for the fastest wins over every game
     * @return the fastest winners, at most {@code limit}, from the cached leaderboard. Any thread.
     */
    List<TopEntry> getTopTimes(GameType game, int limit);
}
