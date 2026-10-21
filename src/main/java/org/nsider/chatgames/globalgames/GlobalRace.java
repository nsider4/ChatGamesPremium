package org.nsider.chatgames.globalgames;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.races.Race;
import org.nsider.chatgames.utils.messages.Msg;
import org.nsider.inventorymanager.extras.Nonnull;

/**
 * A race played globally: every block mined, mob killed, item crafted... by anyone is +1
 * towards one shared amount, e.g. 500 Stone. The race listeners report progress as for
 * a normal race; {@link org.nsider.chatgames.races.RaceManager} hands it here.
 */
final class GlobalRace extends GlobalSession {

    private final Race race;

    GlobalRace(@Nonnull ChatGames plugin, @Nonnull GlobalGameManager manager, @Nonnull GlobalGameConfig.Entry entry,
               @Nonnull Race race) {
        super(plugin, manager, entry, entry.getGoal());
        this.race = race;
    }

    @Nonnull
    @Override
    Msg startMessage() {
        return fill(Msg.of(GlobalGameManager.messageList("race_start"))).with("%value%", race.getDisplayName());
    }

    @Override
    void begin() {
        plugin.getRaceManager().attachGlobal(race);
    }

    /** One player's progress in the race. Any thread. */
    void progress(@Nonnull Player player, int amount) {
        manager.contribute(this, player, amount);
    }

    @Override
    void end() {
        plugin.getRaceManager().detachGlobal(race);
    }

    @Nonnull
    Race getRace() {
        return race;
    }

    @Nonnull
    @Override
    public String getGoalText() {
        return progress.getGoal() + " " + race.getDisplayName();
    }
}
