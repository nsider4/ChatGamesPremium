package org.nsider.chatgames.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player completes a daily or weekly challenge (challenges.yml). Its reward is
 * claimed separately, with {@code /chatgames claim}.
 */
public class ChatGamesChallengeCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String challengeId;
    private final String challengeName;
    private final String period;

    public ChatGamesChallengeCompleteEvent(Player player, String challengeId, String challengeName, String period) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.challengeId = challengeId;
        this.challengeName = challengeName;
        this.period = period;
    }

    public Player getPlayer() {
        return player;
    }

    /** @return the challenge's id in challenges.yml */
    public String getChallengeId() {
        return challengeId;
    }

    public String getChallengeName() {
        return challengeName;
    }

    /** @return "daily" or "weekly" */
    public String getPeriod() {
        return period;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
