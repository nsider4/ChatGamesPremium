package org.nsider.chatgames.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a game event (events.yml) is about to start, by command or by its schedule.
 * Cancelling it keeps the event from starting; nothing has been shown yet.
 */
public class ChatGamesEventStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String eventId;
    private final String eventName;
    private final boolean scheduled;
    private boolean cancelled;

    public ChatGamesEventStartEvent(String eventId, String eventName, boolean scheduled) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.scheduled = scheduled;
    }

    /** @return the event's id in events.yml */
    public String getEventId() {
        return eventId;
    }

    /** @return the event's display name */
    public String getEventName() {
        return eventName;
    }

    /** @return true if its schedule started it, false for a command */
    public boolean isScheduled() {
        return scheduled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
