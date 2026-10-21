package org.nsider.chatgames.events;

import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.nsider.chatgames.enums.GameType;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

/**
 * Fired just before a chat game or race is announced. Cancelling it stops the
 * game from starting; nothing has been shown to players yet.
 */
public class ChatGamesStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    @Getter private final GameType type;
    @Nullable private final String answer;
    private boolean cancelled;

    /**
     * @param answer the answer, or null for races
     */
    public ChatGamesStartEvent(@Nonnull GameType type, @Nullable String answer) {
        this.type = type;
        this.answer = answer;
    }

    public boolean hasAnswer() {
        return answer != null;
    }

    @Nullable
    public String getAnswer() {
        return answer;
    }

    @Nonnull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Nonnull
    @Override
    public String getEventName() {
        return "ChatGamesStartEvent";
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
