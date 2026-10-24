package org.nsider.chatgames.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when one of a player's streaks (streaks.yml) grows by a day.
 */
public class ChatGamesStreakEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String streak;
    private final int days;

    public ChatGamesStreakEvent(Player player, String streak, int days) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.streak = streak;
        this.days = days;
    }

    public Player getPlayer() {
        return player;
    }

    /** @return the streak's type in streaks.yml: "challenge", "daily" or "global" */
    public String getStreak() {
        return streak;
    }

    /** @return how many days in a row it is now */
    public int getDays() {
        return days;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
