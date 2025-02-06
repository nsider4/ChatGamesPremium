package org.nsider.chatgames.races.validator;

import org.bukkit.Material;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.utils.gameplay.XMaterial;

import java.util.logging.Level;

public class MaterialValidator {
    private final ChatGames plugin;

    public MaterialValidator(ChatGames plugin) {
        this.plugin = plugin;
    }

    public Material validateMaterial(String mat) {
        try {
            Material material = XMaterial.valueOf(mat.toUpperCase()).parseMaterial();
            if (material == null) {
                plugin.getLogger().log(Level.INFO, "Error while trying to get Material for {0}", mat);
                return null;
            }
            return material;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.INFO, "Error while trying to get Material for {0}", mat);
            return null;
        }
    }
}
