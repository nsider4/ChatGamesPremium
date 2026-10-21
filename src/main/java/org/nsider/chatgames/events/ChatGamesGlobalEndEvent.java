package org.nsider.chatgames.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.globalgames.CoopProgress;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fired when a global game ends, after {@link ChatGamesEndEvent}: whether the server
 * reached the goal, how far it got, and what each player contributed.
 */
public class ChatGamesGlobalEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameType type;
    private final CoopProgress.Outcome outcome;
    private final int progress;
    private final int goal;
    private final int minContribution;
    private final List<CoopProgress.Contributor> contributors;

    public ChatGamesGlobalEndEvent(@Nonnull GameType type, @Nonnull CoopProgress.Outcome outcome, int progress, int goal,
                                   int minContribution, @Nonnull List<CoopProgress.Contributor> contributors) {
        this.type = type;
        this.outcome = outcome;
        this.progress = progress;
        this.goal = goal;
        this.minContribution = minContribution;
        this.contributors = Collections.unmodifiableList(new ArrayList<>(contributors));
    }

    @Nonnull
    public GameType getType() {
        return type;
    }

    /** @return COMPLETED (goal reached), FAILED (time up, a broken count) or STOPPED (by an admin) */
    @Nonnull
    public CoopProgress.Outcome getOutcome() {
        return outcome;
    }

    public boolean isCompleted() {
        return outcome == CoopProgress.Outcome.COMPLETED;
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
    @Nonnull
    public List<CoopProgress.Contributor> getContributors() {
        return contributors;
    }

    @Nonnull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
