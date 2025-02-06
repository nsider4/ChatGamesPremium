package org.nsider.chatgames.races.validator;

import org.bukkit.entity.EntityType;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.races.ValidatedEntity;

import java.util.logging.Level;

public class EntityValidator {
    private final ChatGames plugin;

    public EntityValidator(ChatGames plugin) {
        this.plugin = plugin;
    }

    public ValidatedEntity validateEntityType(String entity) {
        try {
            EntityType bukkitEntityType = EntityType.valueOf(entity.toUpperCase());
            return new ValidatedEntity(bukkitEntityType);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.INFO, "Error while trying to get Entity Type for {0}", entity);
            return null;
        }
    }
}
