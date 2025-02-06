package org.nsider.chatgames.races.race;

import org.bukkit.Material;
import org.nsider.chatgames.races.Race;

import java.util.List;

/**
 *
 * @author Nicolas
 */
public class CraftRace extends Race {
    private final int amount;
    private final List<String> rewards;

    public CraftRace(Material material, int amount, List<String> rewards) {
        super(material);
        this.amount = amount;
        this.rewards = rewards;
    }

    @Override
    public Integer getAmount() {
        return amount;
    }

    @Override
    public List<String> getRewards() {
        return rewards;
    }
}

