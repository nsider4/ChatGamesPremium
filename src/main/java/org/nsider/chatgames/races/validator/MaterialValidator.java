package org.nsider.chatgames.races.validator;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;
import org.nsider.chatgames.ChatGames;

import java.util.logging.Level;

public class MaterialValidator {
    private final ChatGames plugin;

    public MaterialValidator(ChatGames plugin) {
        this.plugin = plugin;
    }

    /**
     * Resolves a material name (modern or legacy) to the Material of the running server version.
     *
     * @return the Material, or null if it doesn't exist on this version
     */
    public Material validateMaterial(String mat) {
        Material material = XMaterial.matchXMaterial(mat)
                .map(xMaterial -> xMaterial.get())
                .orElse(null);

        if (material == null) {
            plugin.getLogger().log(Level.INFO, "Error while trying to get Material for {0}", mat);
        }
        return material;
    }
}
