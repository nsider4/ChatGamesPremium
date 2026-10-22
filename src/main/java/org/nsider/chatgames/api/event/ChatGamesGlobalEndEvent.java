package org.nsider.chatgames.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.nsider.chatgames.api.GameType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Fired when a global game ends, after {@link ChatGamesEndEvent}: whether the server reached
 * the goal, how far it got, and what each player contributed.
 */
public class ChatGamesGlobalEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    /** How a global game ended. */
    public enum Outcome {
        /** The server reached the goal. */
        COMPLETED,
        /** Time ran out, or a count was broken too often. */
        FAILED,
        /** An admin stopped it. */
        STOPPED
    }

    private final GameType type;
    private final Outcome outcome;
    private final int progress;
    private final int goal;
    private final int minContribution;
    private final List<Contributor> contributors;

    public ChatGamesGlobalEndEvent(GameType type, Outcome outcome, int progress, int goal, int minContribution,
                                   List<Contributor> contributors) {
        this.type = type;
        this.outcome = outcome;
        this.progress = progress;
        this.goal = goal;
        this.minContribution = minContribution;
        this.contributors = Collections.unmodifiableList(new ArrayList<>(contributors));
    }

    public GameType getType() {
        return type;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public boolean isCompleted() {
        return outcome == Outcome.COMPLETED;
    }

    public int getProgress() {
        return progress;
    }

    public int getGoal() {
        return goal;
    }

    /** @return the least a player had to contribute to be rewarded */
    public int getMinContribution() {
        return minContribution;
    }

    /** @return everyone who contributed, most first */
    public List<Contributor> getContributors() {
        return contributors;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /** What one player added to a global game. */
    public static final class Contributor {
        private final UUID uuid;
        private final int amount;
        private final int place;

        public Contributor(UUID uuid, int amount, int place) {
            this.uuid = uuid;
            this.amount = amount;
            this.place = place;
        }

        public UUID getUuid() {
            return uuid;
        }

        public int getAmount() {
            return amount;
        }

        /** @return 1 for whoever contributed most */
        public int getPlace() {
            return place;
        }
    }
}
