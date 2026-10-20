package org.nsider.chatgames.races.validator;

import com.cryptomorin.xseries.XEntityType;
import org.bukkit.entity.EntityType;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.races.ValidatedEntity;

import java.util.logging.Level;

public class EntityValidator {
    private final ChatGames plugin;

    public EntityValidator(ChatGames plugin) {
        this.plugin = plugin;
    }

    /**
     * Resolves an entity name (modern or legacy, e.g. MOOSHROOM or MUSHROOM_COW) for the running server version.
     *
     * @return the validated entity, or null if it doesn't exist on this version
     */
    public ValidatedEntity validateEntityType(String entity) {
        EntityType entityType = XEntityType.of(entity)
                .map(xEntityType -> xEntityType.get())
                .orElse(null);

        if (entityType == null) {
            plugin.getLogger().log(Level.INFO, "Error while trying to get Entity Type for {0}", entity);
            return null;
        }
        return new ValidatedEntity(entityType);
    }
}
