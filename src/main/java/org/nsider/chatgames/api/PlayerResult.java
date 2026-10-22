package org.nsider.chatgames.api;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * A player's place in a finished chat game or race.
 */
public final class PlayerResult {

    private final UUID uuid;
    private final double time;
    private final int place;

    public PlayerResult(UUID uuid, double time, int place) {
        this.uuid = uuid;
        this.time = time;
        this.place = place;
    }

    public UUID getUuid() {
        return uuid;
    }

    /** @return seconds from the start of the game to the player's answer (or finishing the race) */
    public double getTime() {
        return time;
    }

    /** @return 1 for the first winner, 2 for the second... */
    public int getPlace() {
        return place;
    }

    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(uuid);
    }
}
