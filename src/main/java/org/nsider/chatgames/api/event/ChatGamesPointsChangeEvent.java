package org.nsider.chatgames.api.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired after a player's points change: from a game, an admin command or a plugin. The
 * player may be offline (their data was loaded to change it). May be fired from any thread.
 */
public class ChatGamesPointsChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID uuid;
    private final int before;
    private final int after;

    public ChatGamesPointsChangeEvent(UUID uuid, int before, int after) {
        super(!Bukkit.isPrimaryThread());
        this.uuid = uuid;
        this.before = before;
        this.after = after;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getBefore() {
        return before;
    }

    public int getAfter() {
        return after;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
