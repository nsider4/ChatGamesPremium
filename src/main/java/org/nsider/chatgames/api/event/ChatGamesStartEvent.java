package org.nsider.chatgames.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.nsider.chatgames.api.GameType;

/**
 * Fired just before a chat game, race or global game is announced. Cancelling it stops the
 * game from starting; nothing has been shown to players yet.
 */
public class ChatGamesStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameType type;
    private final String answer;
    private boolean cancelled;

    /**
     * @param answer the answer, or null for races
     */
    public ChatGamesStartEvent(GameType type, String answer) {
        this.type = type;
        this.answer = answer;
    }

    public GameType getType() {
        return type;
    }

    public boolean hasAnswer() {
        return answer != null;
    }

    /** @return the round's answer, null for races; for global games, a description of the goal */
    public String getAnswer() {
        return answer;
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
