package org.nsider.chatgames.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.nsider.chatgames.gameevents.EventDefinition;
import org.nsider.chatgames.gameevents.EventScores;

import java.util.Collections;
import java.util.List;

import org.nsider.inventorymanager.extras.Nonnull;

/**
 * Fired when a game event ends: after its last game (with the final standings), or
 * when an admin stops it (no standings, no rewards).
 */
public class ChatGamesEventEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EventDefinition definition;
    private final List<EventScores.Standing> standings;
    private final boolean stopped;

    /**
     * @param standings the final standings, best first; empty when stopped
     * @param stopped   true if an admin stopped it before its last game
     */
    public ChatGamesEventEndEvent(@Nonnull EventDefinition definition, @Nonnull List<EventScores.Standing> standings,
                                  boolean stopped) {
        this.definition = definition;
        this.standings = Collections.unmodifiableList(standings);
        this.stopped = stopped;
    }

    @Nonnull
    public EventDefinition getDefinition() {
        return definition;
    }

    /** @return the event's id in events.yml */
    @Nonnull
    public String getEventId() {
        return definition.getId();
    }

    /** @return the final standings, best first; on equal points whoever scored first is ahead */
    @Nonnull
    public List<EventScores.Standing> getStandings() {
        return standings;
    }

    public boolean wasStopped() {
        return stopped;
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
