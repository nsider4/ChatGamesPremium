package org.nsider.chatgames.gameevents;

import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * One event from events.yml. Unchanging.
 */
public final class EventDefinition {

    private final String id;
    private final String name;
    private final List<EventSchedule> schedule;
    private final int minimumPlayers;
    private final int pauseSeconds;
    /** Event points for 1st, 2nd... in each step. */
    private final int[] placePoints;
    /** Event points for anyone else who made it in a step (only global steps have more). */
    private final int participationPoints;
    private final List<EventStep> steps;
    /** Reward lines by "1", "2"... and "participation". */
    private final Map<String, List<String>> rewards;

    EventDefinition(String id, String name, List<EventSchedule> schedule, int minimumPlayers, int pauseSeconds,
                    int[] placePoints, int participationPoints, List<EventStep> steps, Map<String, List<String>> rewards) {
        this.id = id;
        this.name = name;
        this.schedule = Collections.unmodifiableList(schedule);
        this.minimumPlayers = minimumPlayers;
        this.pauseSeconds = pauseSeconds;
        this.placePoints = placePoints.clone();
        this.participationPoints = participationPoints;
        this.steps = Collections.unmodifiableList(steps);
        this.rewards = Collections.unmodifiableMap(rewards);
    }

    @Nonnull
    public String getId() {
        return id;
    }

    /** @return the name shown to players, with colour codes */
    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public List<EventSchedule> getSchedule() {
        return schedule;
    }

    /** @return the players needed online for a scheduled start (the command ignores it) */
    public int getMinimumPlayers() {
        return minimumPlayers;
    }

    /** @return seconds between one step ending and the next starting */
    public int getPauseSeconds() {
        return pauseSeconds;
    }

    @Nonnull
    public int[] getPlacePoints() {
        return placePoints.clone();
    }

    public int getParticipationPoints() {
        return participationPoints;
    }

    @Nonnull
    public List<EventStep> getSteps() {
        return steps;
    }

    /**
     * @return the reward lines for a final place: that place's, or {@code participation}'s
     *         for everyone without one of their own
     */
    @Nonnull
    public List<String> rewardsFor(int place) {
        List<String> lines = rewards.get(String.valueOf(place));
        if (lines == null) lines = rewards.get("participation");
        return lines == null ? Collections.<String>emptyList() : lines;
    }
}
