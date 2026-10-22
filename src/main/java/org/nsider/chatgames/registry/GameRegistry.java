package org.nsider.chatgames.registry;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.api.ChatGame;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.api.RaceGame;
import org.nsider.chatgames.api.RewardAction;
import org.nsider.chatgames.games.round.RoundGenerators;
import org.nsider.chatgames.progression.ChallengeType;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * What other plugins added through the API: games, races, reward actions and challenge types.
 *
 * <p>Presets, events, challenges and global games name games, and ChatGames reads them while it
 * enables, before the plugins that add games have. So names that aren't games yet aren't warned
 * about until the server has started ({@link #markStarted}), when everything is read again.
 * Registering or removing a game later reads them again on the next tick.</p>
 */
public final class GameRegistry implements Listener {

    private static final Pattern REWARD_NAME = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final Pattern CHALLENGE_ID = Pattern.compile("[a-z][a-z0-9_]{1,31}");
    private static final Set<String> BUILT_IN_REWARDS = new HashSet<>(Arrays.asList(
            "playercmd", "consolecmd", "playermsg", "broadcast", "player_as_op"));

    @Nullable private static volatile GameRegistry instance;
    private static volatile boolean started;
    private static volatile boolean suppressedWarnings;

    private final ChatGames plugin;
    private final Map<GameType, ChatGame> games = new ConcurrentHashMap<>();
    private final Map<GameType, RaceGame> races = new ConcurrentHashMap<>();
    private final Map<GameType, Plugin> owners = new ConcurrentHashMap<>();
    private final Map<String, Owned<RewardAction>> rewardActions = new ConcurrentHashMap<>();
    private final Map<String, Plugin> challengeTypes = new ConcurrentHashMap<>();
    private boolean refreshPending;

    public GameRegistry(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
        instance = this;
        started = false;
        suppressedWarnings = false;
    }

    // Games

    @Nonnull
    public GameType registerGame(@Nonnull Plugin owner, @Nonnull ChatGame game) {
        GameType type = register(owner, game.getId(), GameType.Kind.CHAT);
        games.put(type, game);
        RoundGenerators.register(type, new ApiRoundGenerator(type, game.getGenerator()));
        GameDefaults.addChatGame(plugin, type, game, owner);
        added(type);
        return type;
    }

    @Nonnull
    public GameType registerRace(@Nonnull Plugin owner, @Nonnull RaceGame race) {
        GameType type = register(owner, race.getId(), GameType.Kind.RACE);
        races.put(type, race);
        GameDefaults.addRace(plugin, type, race, owner);
        added(type);
        return type;
    }

    private GameType register(Plugin owner, String id, GameType.Kind kind) {
        if (plugin.getChatGamesCommand() != null && plugin.getChatGamesCommand().isTaken(id)) {
            throw new IllegalArgumentException("'" + id + "' is a /chatgames command; pick another game id");
        }
        GameType type = GameType.register(id, kind, owner.getName());
        owners.put(type, owner);
        return type;
    }

    private void added(GameType type) {
        if (plugin.getChatGamesCommand() != null) plugin.getChatGamesCommand().addGame(type);
        plugin.getLogger().info(owners.get(type).getName() + " added the " + (type.isRace() ? "race " : "game ") + type.getId() + ".");
        refreshLater();
    }

    /**
     * Removes a game another plugin added; stops it first if it is running. Main/global thread.
     */
    public boolean unregister(@Nonnull GameType type) {
        if (type.isBuiltIn() || !owners.containsKey(type)) return false;
        if (plugin.getGameManager() != null && plugin.getGameManager().getCurrentGame() == type) {
            plugin.getGameManager().stopCurrent();
        }
        GameType.unregister(type);
        games.remove(type);
        races.remove(type);
        owners.remove(type);
        RoundGenerators.unregister(type);
        if (plugin.getChatGamesCommand() != null) plugin.getChatGamesCommand().removeGame(type);
        refreshLater();
        return true;
    }

    @Nullable
    public static ChatGame chatGame(@Nonnull GameType type) {
        GameRegistry registry = instance;
        return registry == null ? null : registry.games.get(type);
    }

    @Nullable
    public static RaceGame race(@Nonnull GameType type) {
        GameRegistry registry = instance;
        return registry == null ? null : registry.races.get(type);
    }

    /** @return the display name a plugin gave the game, or null for built-in games */
    @Nullable
    public static String displayName(@Nonnull GameType type) {
        ChatGame game = chatGame(type);
        if (game != null) return game.getDisplayName();
        RaceGame race = race(type);
        return race != null ? race.getDisplayName() : null;
    }

    /** @return the menu icon a plugin gave the game, or null for built-in games */
    @Nullable
    public static String icon(@Nonnull GameType type) {
        ChatGame game = chatGame(type);
        if (game != null) return game.getIcon();
        RaceGame race = race(type);
        return race != null ? race.getIcon() : null;
    }

    // Reward actions

    public void registerRewardAction(@Nonnull Plugin owner, @Nonnull String name, @Nonnull RewardAction action) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (!REWARD_NAME.matcher(key).matches()) {
            throw new IllegalArgumentException("Reward action '" + name + "' must be 1-32 lower-case letters, digits, _ or -");
        }
        if (BUILT_IN_REWARDS.contains(key) || rewardActions.containsKey(key)) {
            throw new IllegalArgumentException("A reward action called [" + key + "] already exists");
        }
        rewardActions.put(key, new Owned<>(owner, action));
    }

    /**
     * Runs a reward line if it starts with an action another plugin added.
     *
     * @param line the reward line, with %player% already filled in
     * @return false if the line isn't one of those actions
     */
    public static boolean runRewardAction(@Nonnull ChatGames plugin, @Nonnull Player player, @Nonnull String line) {
        GameRegistry registry = instance;
        if (registry == null || !line.startsWith("[")) return false;
        int end = line.indexOf(']');
        if (end < 0) return false;
        Owned<RewardAction> action = registry.rewardActions.get(line.substring(1, end).trim().toLowerCase(Locale.ROOT));
        if (action == null) return false;
        String argument = line.substring(end + 1).trim();
        plugin.getScheduler().onGlobal(() -> {
            try {
                action.value.give(player, argument);
            } catch (RuntimeException | LinkageError e) {
                plugin.getLogger().log(Level.WARNING, action.owner.getName() + "'s reward action failed: " + line, e);
            }
        });
        return true;
    }

    // Challenge types

    public void registerChallengeType(@Nonnull Plugin owner, @Nonnull String id) {
        String key = id.trim().toLowerCase(Locale.ROOT);
        if (!CHALLENGE_ID.matcher(key).matches()) {
            throw new IllegalArgumentException("Challenge type '" + id + "' must be 2-32 lower-case letters, digits or _");
        }
        if (isBuiltInChallengeType(key) || challengeTypes.containsKey(key)) {
            throw new IllegalArgumentException("A challenge type called " + key + " already exists");
        }
        challengeTypes.put(key, owner);
        refreshLater();
    }

    /** @return true if another plugin added this challenge type */
    public static boolean isChallengeType(@Nullable String id) {
        GameRegistry registry = instance;
        return registry != null && id != null && registry.challengeTypes.containsKey(id.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean isBuiltInChallengeType(String key) {
        for (ChallengeType type : ChallengeType.values()) {
            if (type.name().equalsIgnoreCase(key)) return true;
        }
        return false;
    }

    // Startup and refreshing

    /**
     * Sends a warning about a name that isn't a game (or challenge type) yet. While the server
     * starts it is held back: a plugin that adds that game may not have enabled yet.
     */
    public static void unknownName(@Nonnull Consumer<String> warn, @Nonnull String message) {
        if (started || instance == null) {
            warn.accept(message);
        } else {
            suppressedWarnings = true;
        }
    }

    /**
     * Called on the first tick, once every plugin has enabled: reads the files that name games
     * again if a plugin added one or a name wasn't known yet.
     */
    public void markStarted() {
        started = true;
        if (suppressedWarnings || !owners.isEmpty() || !challengeTypes.isEmpty()) refresh();
    }

    private void refreshLater() {
        if (!started || refreshPending) return;
        refreshPending = true;
        plugin.getScheduler().global(() -> {
            refreshPending = false;
            refresh();
        });
    }

    /** Re-reads everything that names games, so added or removed ones are picked up. */
    private void refresh() {
        plugin.getRaceManager().load();
        plugin.getProgressionManager().load();
        plugin.getGlobalGameManager().load();
        plugin.getEventManager().load();
        plugin.getMainTask().reloadEnabledGames();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        Plugin disabled = event.getPlugin();
        if (disabled == plugin) return;
        for (Map.Entry<GameType, Plugin> entry : new ArrayList<>(owners.entrySet())) {
            if (entry.getValue() == disabled) unregister(entry.getKey());
        }
        rewardActions.values().removeIf(action -> action.owner == disabled);
        if (challengeTypes.values().removeIf(owner -> owner == disabled)) refreshLater();
    }

    /** @return the games other plugins added, for /chatgames debug and tests */
    @Nonnull
    public List<GameType> getAdded() {
        return new ArrayList<>(owners.keySet());
    }

    private static final class Owned<T> {
        final Plugin owner;
        final T value;

        Owned(Plugin owner, T value) {
            this.owner = owner;
            this.value = value;
        }
    }
}
