package org.nsider.chatgames.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.nsider.chatgames.api.GameType;

/**
 * Fired for each winner of a chat game or race, before they get points and rewards. The
 * points can be changed and the rewards skipped; the win itself always counts.
 *
 * <p>Chat game wins are usually fired from the chat thread ({@link #isAsynchronous()} is then
 * true); race wins from the main thread.</p>
 */
public class ChatGamesWinEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final GameType type;
    private final int place;
    private final double seconds;
    private int points;
    private boolean giveRewards = true;

    public ChatGamesWinEvent(Player player, GameType type, int place, double seconds, int points) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.type = type;
        this.place = place;
        this.seconds = seconds;
        this.points = points;
    }

    public Player getPlayer() {
        return player;
    }

    public GameType getType() {
        return type;
    }

    /** @return 1 for the first winner; races have only one */
    public int getPlace() {
        return place;
    }

    /** @return seconds from the start of the game to the win */
    public double getSeconds() {
        return seconds;
    }

    /** @return the points the win pays */
    public int getPoints() {
        return points;
    }

    /** @param points the points the win pays instead; 0 for none */
    public void setPoints(int points) {
        this.points = Math.max(0, points);
    }

    public boolean isGiveRewards() {
        return giveRewards;
    }

    /** @param giveRewards false to skip the rewards from rewards.yml (or races.yml) for this win */
    public void setGiveRewards(boolean giveRewards) {
        this.giveRewards = giveRewards;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
