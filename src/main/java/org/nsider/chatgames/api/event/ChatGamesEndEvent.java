package org.nsider.chatgames.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.api.PlayerResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Fired when a chat game, race or global game ends: with its winners, when time runs out
 * (no winners), or when an admin stops it (no winners). Rewards have been given by then.
 */
public class ChatGamesEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameType type;
    private final String answer;
    private final List<PlayerResult> winners;

    /**
     * @param answer  the answer, or null for races
     * @param winners the winners ordered by place
     */
    public ChatGamesEndEvent(GameType type, String answer, List<PlayerResult> winners) {
        this.type = type;
        this.answer = answer;
        this.winners = Collections.unmodifiableList(new ArrayList<>(winners));
    }

    public GameType getType() {
        return type;
    }

    public boolean hasAnswer() {
        return answer != null;
    }

    /** @return the round's answer, null for races */
    public String getAnswer() {
        return answer;
    }

    /** @return the winners ordered by place; for global games, every contributor */
    public List<PlayerResult> getWinners() {
        return winners;
    }

    public boolean hasWinner() {
        return !winners.isEmpty();
    }

    /** @return the player's result, or null if they didn't place */
    public PlayerResult getResult(UUID player) {
        for (PlayerResult result : winners) {
            if (result.getUuid().equals(player)) return result;
        }
        return null;
    }

    /** @return the player's time in seconds, or null if they didn't place */
    public Double getTime(Player player) {
        PlayerResult result = getResult(player.getUniqueId());
        return result == null ? null : result.getTime();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
