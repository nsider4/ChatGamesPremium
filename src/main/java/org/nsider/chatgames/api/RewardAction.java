package org.nsider.chatgames.api;

import org.bukkit.entity.Player;

/**
 * A reward line of your own, e.g. {@code [myplugin_crate] legendary 2} in rewards.yml, races.yml
 * rewards, challenges, streaks, milestones or events.
 */
@FunctionalInterface
public interface RewardAction {

    /**
     * Gives the reward. Called on the main thread (the global region on Folia); on Folia, use the
     * player's scheduler for anything that touches the player or the world.
     *
     * @param argument the rest of the line after {@code [name]}, with {@code %player%} filled in
     */
    void give(Player player, String argument);
}
