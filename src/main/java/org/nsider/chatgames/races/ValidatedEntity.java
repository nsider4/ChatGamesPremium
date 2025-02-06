package org.nsider.chatgames.races;

import lombok.Getter;
import org.bukkit.entity.EntityType;

@Getter
public class ValidatedEntity {
    private final EntityType entityType;

    public ValidatedEntity(EntityType entityType) {
        this.entityType = entityType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ValidatedEntity)) return false;
        ValidatedEntity other = (ValidatedEntity) obj;
        return this.entityType == other.entityType;
    }

    @Override
    public int hashCode() {
        return entityType.hashCode();
    }
}
