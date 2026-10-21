package org.nsider.chatgames.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.nsider.chatgames.gameevents.EventDefinition;

import org.nsider.inventorymanager.extras.Nonnull;

/**
 * Fired when a game event (events.yml) is about to start, by command or by its
 * schedule. Cancelling it keeps the event from starting; nothing has been shown yet.
 */
public class ChatGamesEventStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EventDefinition definition;
    private final boolean scheduled;
    private boolean cancelled;

    public ChatGamesEventStartEvent(@Nonnull EventDefinition definition, boolean scheduled) {
        this.definition = definition;
        this.scheduled = scheduled;
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

    /** @return true if its schedule started it, false for a command */
    public boolean isScheduled() {
        return scheduled;
    }

    @Nonnull
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
