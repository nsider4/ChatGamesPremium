package org.nsider.chatgames.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.nsider.chatgames.api.GameType;

/**
 * Fired when a player gives a correct answer to the running chat game, before they get a
 * place. Cancelling it ignores the answer, as if it were wrong (useful for anti-cheat).
 *
 * <p>Usually fired from the chat thread, so {@link #isAsynchronous()} is often true: don't
 * touch the world from a listener without scheduling it.</p>
 */
public class ChatGamesAnswerEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final GameType type;
    private final String answer;
    private final long millis;
    private boolean cancelled;

    /**
     * @param millis how long after the round opened the answer came
     */
    public ChatGamesAnswerEvent(Player player, GameType type, String answer, long millis) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.type = type;
        this.answer = answer;
        this.millis = millis;
    }

    public Player getPlayer() {
        return player;
    }

    public GameType getType() {
        return type;
    }

    /** @return what the player typed */
    public String getAnswer() {
        return answer;
    }

    /** @return milliseconds from the round opening to the answer */
    public long getMillis() {
        return millis;
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
