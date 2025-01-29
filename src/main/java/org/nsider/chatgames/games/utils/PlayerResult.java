package org.nsider.chatgames.games.utils;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class PlayerResult {
    private final UUID uuid;
    private final double time;
    private final int place;

    public PlayerResult(UUID uuid, double time, int place) {
        this.uuid = uuid;
        this.time = time;
        this.place = place;
    }

    public OfflinePlayer getPlayer() { return Bukkit.getOfflinePlayer(uuid); }
}
