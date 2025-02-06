package org.nsider.chatgames.races.race;

import org.nsider.chatgames.races.Race;
import org.nsider.chatgames.races.ValidatedEntity;

import java.util.List;

/**
 *
 * @author Nicolas
 */
public class HuntRace extends Race {
    private final int amount;
    private final List<String> rewards;

    public HuntRace(ValidatedEntity validatedEntity, int amount, List<String> rewards) {
        super(validatedEntity);
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
