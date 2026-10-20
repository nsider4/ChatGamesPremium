package org.nsider.chatgames.races;

import com.google.gson.JsonElement;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.utils.gameplay.VersionUtils;
import org.nsider.chatgames.utils.gameplay.XMaterial;

import java.util.List;

public class RaceUtils {

    private final ChatGames plugin;

    public RaceUtils(ChatGames plugin) {
        this.plugin = plugin;
    }

    public String getItemName(Material material) {
        String name = material.toString();
        if (name.contains("_")) {
            String[] parts = name.split("_");
            StringBuilder result = new StringBuilder();
            for (String part : parts) {
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase()).append(" ");
            }
            return result.toString().trim();
        }
        return name;
    }

    public String getEntityName(EntityType type) {
        String name = type.toString();
        if (name.contains("_")) {
            String[] parts = name.split("_");
            StringBuilder result = new StringBuilder();
            for (String part : parts) {
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase()).append(" ");
            }
            return result.toString().trim();
        }
        return name;
    }

    public String getMaterialName(Material material) {
        String materialKey = this.convertTo113(material).toLowerCase();

        JsonElement element = plugin.getLanguageManager().getLanguageData().get("block.minecraft." + materialKey);
        if (element != null) {
            return element.getAsString();
        } else {
            JsonElement element1 = plugin.getLanguageManager().getLanguageData().get("item.minecraft." + materialKey);
            if (element1 != null) {
                return element1.getAsString();
            }
        }

        return this.getItemName(material);
    }

    public String getEntityTypeName(ValidatedEntity validatedEntity) {
        /*if (plugin.isMythicMobsEnabled() && validatedEntity.isMythicMob()) {
            MythicMob mythicMob = validatedEntity.getMythicMob();
            if (mythicMob != null) {
                return mythicMob.getDisplayName().get();
            }
        } else {*/
        EntityType entityType = validatedEntity.getEntityType();
        String entityName = plugin.getLanguageManager().getLanguageData()
                .getAsJsonPrimitive("entity.minecraft." + entityType.name().toLowerCase())
                .getAsString();

        if (entityName == null || entityName.isEmpty()) {
            return this.getEntityName(entityType);
        }

        return entityName;
    }

    private String convertTo113(Material material) {
        if (VersionUtils.getMCVersion() < 13) {
            XMaterial xMaterial = XMaterial.matchXMaterial(material);
            if (xMaterial == null) {
                return null;
            }
            return xMaterial.name();
        } else {
            return material.name();
        }
    }

    public int getMaxCraftAmount(CraftingInventory inv) {
        if (inv.getResult() == null)
            return 0;

        int resultCount = inv.getResult().getAmount();
        int materialCount = Integer.MAX_VALUE;

        for (ItemStack is : inv.getMatrix())
            if (is != null && is.getAmount() < materialCount)
                materialCount = is.getAmount();

        return resultCount * materialCount;
    }

    public int fits(ItemStack stack, Inventory inv) {
        ItemStack[] contents = inv.getContents();
        int result = 0;

        for (ItemStack item : contents)
            if (item == null)
                result += stack.getMaxStackSize();
            else if (item.isSimilar(stack))
                result += Math.max(stack.getMaxStackSize() - item.getAmount(), 0);

        return result;
    }

    public String getValueName() {
        String var;
        if(plugin.getRaceBuilder().getCurrentRace().equalsIgnoreCase("hunt")) {
            var = this.getEntityTypeName(plugin.getRaceBuilder().getRace().getEntityType());
        } else {
            var = this.getMaterialName(plugin.getRaceBuilder().getRace().getMaterial());
        }
        return var;
    }

    public List<String> getRewardsForGame(String game, String key, int amount) {
        return ConfigManager.getRaces().getStringList(game+".data."+key+"."+amount+".rewards");
    }
}
