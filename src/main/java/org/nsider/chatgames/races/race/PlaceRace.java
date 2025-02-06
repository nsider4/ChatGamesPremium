package org.nsider.chatgames.races.race;

import org.bukkit.Material;
import org.nsider.chatgames.races.Race;

import java.util.List;

/**
 *
 * @author Nicolas
 */
public class PlaceRace extends Race {
    private final int amount;
    private final List<String> rewards;

    public PlaceRace(Material material, int amount, List<String> rewards) {
        super(material);
        this.amount = amount;
        this.rewards = rewards;
    }


    @Override
    public Material getMaterial() {
        return material;
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

