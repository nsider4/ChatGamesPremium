package org.nsider.chatgames.registry;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.api.ChatGame;
import org.nsider.chatgames.api.ChatGamesAPI;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.api.PlayerStats;
import org.nsider.chatgames.api.RaceGame;
import org.nsider.chatgames.api.RewardAction;
import org.nsider.chatgames.api.TopEntry;
import org.nsider.chatgames.managers.TopManager;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.chatgames.placeholders.LeaderboardSnapshot;
import org.nsider.chatgames.progression.Activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * {@link ChatGamesAPI} for other plugins, registered with Bukkit's services manager on enable.
 */
public final class ChatGamesApiImpl implements ChatGamesAPI {

    private final ChatGames plugin;
    private final GameRegistry registry;

    public ChatGamesApiImpl(ChatGames plugin, GameRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public GameType registerGame(Plugin owner, ChatGame game) {
        return registry.registerGame(Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(game, "game"));
    }

    @Override
    public GameType registerRace(Plugin owner, RaceGame race) {
        return registry.registerRace(Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(race, "race"));
    }

    @Override
    public boolean unregister(GameType game) {
        return game != null && registry.unregister(game);
    }

    @Override
    public GameType getGame(String id) {
        return GameType.fromName(id);
    }

    @Override
    public List<GameType> getGames() {
        return Collections.unmodifiableList(Arrays.asList(GameType.values()));
    }

    @Override
    public boolean startGame(GameType game, String... args) {
        return plugin.getGameManager().startGame(Objects.requireNonNull(game, "game"), args == null ? new String[0] : args);
    }

    @Override
    public boolean stopGame() {
        return plugin.getGameManager().stopCurrent();
    }

    @Override
    public GameType getCurrentGame() {
        return plugin.getGameManager().getCurrentGame();
    }

    @Override
    public void addRaceProgress(Player player, GameType race, String target, int amount) {
        if (player == null || race == null || amount <= 0) return;
        plugin.getRaceManager().progress(race, player, target, amount);
    }

    @Override
    public void registerRewardAction(Plugin owner, String name, RewardAction action) {
        registry.registerRewardAction(Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(name, "name"),
                Objects.requireNonNull(action, "action"));
    }

    @Override
    public void registerChallengeType(Plugin owner, String id) {
        registry.registerChallengeType(Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(id, "id"));
    }

    @Override
    public void addChallengeProgress(Player player, String type, int amount) {
        if (player == null || type == null || amount <= 0) return;
        plugin.getProgressionManager().record(player, Activity.custom(type, amount));
    }

    @Override
    public PlayerStats getStats(Player player) {
        ChatGamesPlayer data = player == null ? null : ChatGamesPlayerManager.get(player.getUniqueId());
        return data == null ? null : new Stats(data);
    }

    @Override
    public void addPoints(OfflinePlayer player, int amount) {
        if (player != null && amount != 0) TopManager.addPoints(player, amount);
    }

    @Override
    public List<TopEntry> getTopPoints(int limit) {
        LeaderboardSnapshot snapshot = plugin.getLeaderboardCache().getSnapshot();
        List<TopEntry> top = new ArrayList<>();
        for (int position = 1; position <= limit; position++) {
            LeaderboardSnapshot.Entry entry = snapshot.points(position);
            if (entry == null) break;
            top.add(new TopEntry(entry.getUuid(), entry.getName(), entry.getValue(), entry.getRank()));
        }
        return top;
    }

    @Override
    public List<TopEntry> getTopTimes(GameType game, int limit) {
        LeaderboardSnapshot snapshot = plugin.getLeaderboardCache().getSnapshot();
        List<TopEntry> top = new ArrayList<>();
        for (int position = 1; position <= limit; position++) {
            LeaderboardSnapshot.Entry entry = game == null ? snapshot.fastest(position) : snapshot.fastest(game, position);
            if (entry == null) break;
            top.add(new TopEntry(entry.getUuid(), entry.getName(), entry.getValue(), entry.getRank()));
        }
        return top;
    }

    private static final class Stats implements PlayerStats {
        private final ChatGamesPlayer data;

        Stats(ChatGamesPlayer data) {
            this.data = data;
        }

        @Override
        public UUID getUuid() {
            return data.getUuid();
        }

        @Override
        public int getPoints() {
            return data.getPoints();
        }

        @Override
        public long getWins() {
            return data.getStat(Stat.WINS);
        }

        @Override
        public long getWins(GameType game) {
            return data.getStat(Stat.wins(game));
        }

        @Override
        public double getFastest(GameType game) {
            return data.getFastest(game);
        }

        @Override
        public boolean isPlaying() {
            return data.isToggled();
        }
    }
}
