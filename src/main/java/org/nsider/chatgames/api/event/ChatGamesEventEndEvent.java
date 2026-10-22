package org.nsider.chatgames.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Fired when a game event ends: after its last game (with the final standings), or when an
 * admin stops it (no standings, no rewards).
 */
public class ChatGamesEventEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String eventId;
    private final String eventName;
    private final List<Standing> standings;
    private final boolean stopped;

    /**
     * @param standings the final standings, best first; empty when stopped
     * @param stopped   true if an admin stopped it before its last game
     */
    public ChatGamesEventEndEvent(String eventId, String eventName, List<Standing> standings, boolean stopped) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.standings = Collections.unmodifiableList(new ArrayList<>(standings));
        this.stopped = stopped;
    }

    /** @return the event's id in events.yml */
    public String getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    /** @return the final standings, best first; on equal points whoever scored first is ahead */
    public List<Standing> getStandings() {
        return standings;
    }

    public boolean wasStopped() {
        return stopped;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /** A player's final place in the event. */
    public static final class Standing {
        private final UUID uuid;
        private final int points;
        private final int place;

        public Standing(UUID uuid, int points, int place) {
            this.uuid = uuid;
            this.points = points;
            this.place = place;
        }

        public UUID getUuid() {
            return uuid;
        }

        /** @return the event points the player scored */
        public int getPoints() {
            return points;
        }

        public int getPlace() {
            return place;
        }
    }
}
