package org.nsider.chatgames.events;

import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.manager.GameUtility;

public class ChatGamesStartEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ChatGames plugin;
    @Getter private final GameType type;
    private boolean cancelled;

    public ChatGamesStartEvent(ChatGames plugin, GameType type) {
        this.plugin = plugin;
        this.type = type;
        this.cancelled = false;
    }

    public boolean hasAnswer() {
        return !GameUtility.isRace(type);
    }

    public String getAnswer() {
        return this.plugin.getGameManager().getGame().getAnswer();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

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
