package org.nsider.chatgames.placeholders;

import org.nsider.chatgames.enums.GameType;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Every leaderboard at one moment, read by placeholders from any thread. Never
 * changed after it is built; {@link LeaderboardCache} swaps in a new one.
 *
 * <p>Ranks are "competition" ranks: players with equal points share a rank and the
 * next rank is skipped (1, 2, 2, 4), like 1.9.9's "players with more points + 1".</p>
 */
public final class LeaderboardSnapshot {

    /** Only this many names per leaderboard are looked up; ranks cover everyone. */
    static final int NAMED_ENTRIES = 100;

    public static final LeaderboardSnapshot EMPTY = new LeaderboardSnapshot(
            Collections.<Entry>emptyList(), Collections.<UUID, Integer>emptyMap(),
            Collections.<Entry>emptyList(), Collections.<UUID, Integer>emptyMap(),
            Collections.<GameType, List<Entry>>emptyMap(), 0);

    private final List<Entry> points;
    private final Map<UUID, Integer> pointsRank;
    private final List<Entry> fastest;
    private final Map<UUID, Integer> fastestRank;
    private final Map<GameType, List<Entry>> fastestByGame;
    private final long totalPoints;

    private LeaderboardSnapshot(List<Entry> points, Map<UUID, Integer> pointsRank, List<Entry> fastest,
                                Map<UUID, Integer> fastestRank, Map<GameType, List<Entry>> fastestByGame, long totalPoints) {
        this.points = points;
        this.pointsRank = pointsRank;
        this.fastest = fastest;
        this.fastestRank = fastestRank;
        this.fastestByGame = fastestByGame;
        this.totalPoints = totalPoints;
    }

    /**
     * @param pointRows   every player with points, in any order
     * @param fastestRows every recorded fastest time (one per player and game), in any order
     * @param names       looks up a player's name; only called for the top {@value #NAMED_ENTRIES} of each list
     */
    @Nonnull
    public static LeaderboardSnapshot build(@Nonnull List<Row> pointRows, @Nonnull List<TimeRow> fastestRows,
                                            @Nonnull Function<UUID, String> names) {
        Map<UUID, String> nameCache = new HashMap<>();
        Function<UUID, String> cachedNames = uuid -> nameCache.computeIfAbsent(uuid, id -> {
            String name = names.apply(id);
            return name == null ? "?" : name;
        });

        List<Row> byPoints = new ArrayList<>();
        long total = 0;
        for (Row row : pointRows) {
            total += row.value > 0 ? (long) row.value : 0;
            if (row.value > 0) byPoints.add(row);
        }
        byPoints.sort(Comparator.comparingDouble((Row r) -> r.value).reversed());

        // Overall best time = the lowest of a player's chat-game times; races only have per-race lists
        Map<UUID, Double> best = new HashMap<>();
        Map<GameType, List<Row>> byGame = new EnumMap<>(GameType.class);
        for (TimeRow row : fastestRows) {
            if (row.seconds <= 0) continue;
            if (row.game == null || !row.game.isRace()) best.merge(row.uuid, row.seconds, Math::min);
            if (row.game != null) byGame.computeIfAbsent(row.game, g -> new ArrayList<>()).add(new Row(row.uuid, row.seconds));
        }
        List<Row> byTime = new ArrayList<>();
        for (Map.Entry<UUID, Double> entry : best.entrySet()) byTime.add(new Row(entry.getKey(), entry.getValue()));
        byTime.sort(Comparator.comparingDouble(r -> r.value));

        Map<UUID, Integer> pointsRank = new HashMap<>();
        List<Entry> points = rank(byPoints, pointsRank, cachedNames);
        Map<UUID, Integer> fastestRank = new HashMap<>();
        List<Entry> fastest = rank(byTime, fastestRank, cachedNames);

        Map<GameType, List<Entry>> fastestByGame = new EnumMap<>(GameType.class);
        for (Map.Entry<GameType, List<Row>> entry : byGame.entrySet()) {
            List<Row> rows = entry.getValue();
            rows.sort(Comparator.comparingDouble(r -> r.value));
            fastestByGame.put(entry.getKey(), rank(rows, new HashMap<>(), cachedNames));
        }

        return new LeaderboardSnapshot(points, pointsRank, fastest, fastestRank, fastestByGame, total);
    }

    /** Ranks sorted rows into {@code ranks}, and returns the named top entries. */
    private static List<Entry> rank(List<Row> sorted, Map<UUID, Integer> ranks, Function<UUID, String> names) {
        List<Entry> named = new ArrayList<>();
        int rank = 0;
        Double previous = null;
        for (int i = 0; i < sorted.size(); i++) {
            Row row = sorted.get(i);
            if (previous == null || row.value != previous) rank = i + 1;
            previous = row.value;
            ranks.put(row.uuid, rank);
            if (i < NAMED_ENTRIES) named.add(new Entry(row.uuid, names.apply(row.uuid), row.value, rank));
        }
        return Collections.unmodifiableList(named);
    }

    /**
     * @param position 1-based place in the list (not the shared rank)
     * @return null if the list is shorter
     */
    @Nullable
    public Entry points(int position) {
        return at(points, position);
    }

    @Nullable
    public Entry fastest(int position) {
        return at(fastest, position);
    }

    @Nullable
    public Entry fastest(@Nonnull GameType game, int position) {
        List<Entry> list = fastestByGame.get(game);
        return list == null ? null : at(list, position);
    }

    /**
     * @return the player's rank by points, or 0 if they have none
     */
    public int pointsRank(@Nonnull UUID uuid) {
        Integer rank = pointsRank.get(uuid);
        return rank == null ? 0 : rank;
    }

    /**
     * @return the player's rank by best overall time, or 0 if they never won
     */
    public int fastestRank(@Nonnull UUID uuid) {
        Integer rank = fastestRank.get(uuid);
        return rank == null ? 0 : rank;
    }

    /**
     * @return the points of every player together (what 1.9.9 called "total games won")
     */
    public long getTotalPoints() {
        return totalPoints;
    }

    private static Entry at(List<Entry> list, int position) {
        return position >= 1 && position <= list.size() ? list.get(position - 1) : null;
    }

    /** A player's value in a leaderboard, as read from the database. */
    public static final class Row {
        final UUID uuid;
        final double value;

        public Row(@Nonnull UUID uuid, double value) {
            this.uuid = uuid;
            this.value = value;
        }
    }

    /** One fastest-time record; a null game is a 1.9.9 overall best, which only counts overall. */
    public static final class TimeRow {
        final UUID uuid;
        final GameType game;
        final double seconds;

        public TimeRow(@Nonnull UUID uuid, @Nullable GameType game, double seconds) {
            this.uuid = uuid;
            this.game = game;
            this.seconds = seconds;
        }
    }

    /** One named place in a leaderboard. */
    public static final class Entry {
        private final UUID uuid;
        private final String name;
        private final double value;
        private final int rank;

        Entry(UUID uuid, String name, double value, int rank) {
            this.uuid = uuid;
            this.name = name;
            this.value = value;
            this.rank = rank;
        }

        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public double getValue() { return value; }
        public int getRank() { return rank; }
    }
}
