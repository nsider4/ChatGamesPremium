package org.nsider.chatgames.events;

import java.util.List;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.GameController;
import org.nsider.chatgames.games.manager.GameUtility;
import org.nsider.chatgames.games.utils.PlayerResult;
import org.nsider.inventorymanager.extras.Nonnull;

public class ChatGamesEndEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ChatGames plugin;
    @Getter private final GameType type;
    private boolean cancelled;
    @Getter private final List<PlayerResult> winners;

    public ChatGamesEndEvent(ChatGames plugin, GameType type, List<PlayerResult> winners) {
        this.plugin = plugin;
        this.type = type;
        this.cancelled = false;
        this.winners = winners;
    }

    public boolean hasAnswer() {
        return !GameUtility.isRace(type);
    }

    public String getAnswer() {
        return this.plugin.getGameManager().getGame().getAnswer();
    }

    public boolean hasWinner() {
        return !this.winners.isEmpty();
    }

    public Double getTime(Player p) {
        PlayerResult result = GameController.getPlayerResult(p.getUniqueId());
        if (result != null) {
            return result.getTime();
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
