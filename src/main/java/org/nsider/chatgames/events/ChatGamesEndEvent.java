package org.nsider.chatgames.events;

import java.util.Collections;
import java.util.List;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.utils.PlayerResult;
import org.nsider.inventorymanager.extras.Nonnull;

public class ChatGamesEndEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    @Getter private final GameType type;
    private final String answer;
    private boolean cancelled;
    @Getter private final List<PlayerResult> winners;

    /**
     * @param type    the game or race that ended
     * @param answer  the answer, or null for races
     * @param winners the winners ordered by place
     */
    public ChatGamesEndEvent(GameType type, String answer, List<PlayerResult> winners) {
        this.type = type;
        this.answer = answer;
        this.cancelled = false;
        this.winners = Collections.unmodifiableList(winners);
    }

    public boolean hasAnswer() {
        return !type.isRace();
    }

    public String getAnswer() {
        return answer;
    }

    public boolean hasWinner() {
        return !this.winners.isEmpty();
    }

    public Double getTime(Player p) {
        for (PlayerResult result : winners) {
            if (result.getUuid().equals(p.getUniqueId())) {
                return result.getTime();
            }
        }
        return 0.0;
    }

    @Override
    public @Nonnull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @Nonnull String getEventName() {
        return "ChatGamesEndEvent";
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
