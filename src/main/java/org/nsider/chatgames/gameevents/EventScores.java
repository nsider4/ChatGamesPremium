package org.nsider.chatgames.gameevents;

import org.nsider.inventorymanager.extras.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The points players earned in one run of an event. Steps end on the main/global
 * thread, placeholders read from any, so every method is synchronized.
 */
public final class EventScores {

    /** By when each player first took part, which breaks ties. */
    private final Map<UUID, Integer> points = new LinkedHashMap<>();

    /**
     * Adds one step's results.
     *
     * @param byPlace     everyone who made it, first place first
     * @param placePoints points for 1st, 2nd...; anyone further down gets {@code participation}
     * @return the points each player earned in this step
     */
    @Nonnull
    public synchronized Map<UUID, Integer> addStep(@Nonnull List<UUID> byPlace, @Nonnull int[] placePoints, int participation) {
        Map<UUID, Integer> earned = new LinkedHashMap<>();
        for (int i = 0; i < byPlace.size(); i++) {
            UUID uuid = byPlace.get(i);
            int amount = i < placePoints.length ? placePoints[i] : participation;
            earned.put(uuid, amount);
            Integer total = points.get(uuid);
            points.put(uuid, (total == null ? 0 : total) + amount);
        }
        return earned;
    }

    public synchronized int points(@Nonnull UUID uuid) {
        Integer total = points.get(uuid);
        return total == null ? 0 : total;
    }

    /** @return the player's place in the ranking, or 0 if they haven't taken part */
    public synchronized int place(@Nonnull UUID uuid) {
        List<Standing> ranking = ranking();
        for (Standing standing : ranking) {
            if (standing.getUuid().equals(uuid)) return standing.getPlace();
        }
        return 0;
    }

    public synchronized int participants() {
        return points.size();
    }

    /**
     * @return everyone who took part, most points first; equal points keep whoever
     *         scored first ahead
     */
    @Nonnull
    public synchronized List<Standing> ranking() {
        List<Map.Entry<UUID, Integer>> entries = new ArrayList<>(points.entrySet());
        // A stable sort, so ties stay in the order players first took part
        Collections.sort(entries, (a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<Standing> ranking = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            ranking.add(new Standing(entries.get(i).getKey(), entries.get(i).getValue(), i + 1));
        }
        return ranking;
    }

    /** One player's final result. */
    public static final class Standing {
        private final UUID uuid;
        private final int points;
        private final int place;

        Standing(UUID uuid, int points, int place) {
            this.uuid = uuid;
            this.points = points;
            this.place = place;
        }

        @Nonnull
        public UUID getUuid() {
            return uuid;
        }

        public int getPoints() {
            return points;
        }

        public int getPlace() {
            return place;
        }
    }
}
