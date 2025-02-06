package org.nsider.chatgames.races;

import java.util.List;

import lombok.Getter;
import org.bukkit.Material;

/**
 *
 * @author Nicolas
 */
public abstract class Race {
    @Getter protected Material material;
    protected ValidatedEntity validatedEntity;

    public Race(Material material) {
        this.material = material;
    }

    public Race(ValidatedEntity entityType) {
        this.validatedEntity = entityType;
    }

    public ValidatedEntity getEntityType() {
        return validatedEntity;
    }

    public abstract Integer getAmount();

    public abstract List<String> getRewards();
}

