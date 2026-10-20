package org.nsider.chatgames.races;

import com.cryptomorin.xseries.XEntityType;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.managers.LanguageManager;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * What a race asks for: a mob for Hunt, a material for every other race.
 *
 * <p>Names in races.yml may be modern or legacy (MOOSHROOM or MUSHROOM_COW, COD or
 * RAW_FISH); XSeries maps them to this server's version. Items are compared with their
 * legacy data value on 1.8-1.12 (a salmon is not a cod there either). Blocks are
 * compared by material only, so on those versions any wool colour counts for WHITE_WOOL.</p>
 */
public final class RaceTarget {

    private final String name;
    @Nullable private final XMaterial material;
    @Nullable private final Material bukkitMaterial;
    @Nullable private final EntityType entity;

    private RaceTarget(String name, @Nullable XMaterial material, @Nullable EntityType entity) {
        this.name = name;
        this.material = material;
        this.bukkitMaterial = material == null ? null : material.get();
        this.entity = entity;
    }

    /**
     * @param name a material or mob name as written in races.yml or typed in a command
     * @return null if it doesn't exist on this server version
     */
    @Nullable
    public static RaceTarget resolve(@Nonnull GameType race, @Nonnull String name) {
        String key = name.trim().toUpperCase(Locale.ROOT);
        try {
            if (race == GameType.HUNT) {
                Optional<XEntityType> type = XEntityType.of(key);
                EntityType entity = type.isPresent() ? type.get().get() : null;
                return entity == null || !entity.isAlive() ? null : new RaceTarget(key, null, entity);
            }
            Optional<XMaterial> type = XMaterial.matchXMaterial(key);
            return type.isPresent() && type.get().get() != null ? new RaceTarget(key, type.get(), null) : null;
        } catch (RuntimeException e) {
            // Names XSeries can't parse at all
            return null;
        }
    }

    /** @return the name as configured, upper-case (used to look up rewards) */
    @Nonnull
    public String getName() {
        return name;
    }

    /** @return the Bukkit name on this server, for logs */
    @Nonnull
    public String getServerName() {
        if (entity != null) return entity.name();
        return bukkitMaterial != null ? bukkitMaterial.name() : name;
    }

    public boolean matches(@Nullable EntityType type) {
        return entity != null && entity == type;
    }

    public boolean matchesBlock(@Nullable Material type) {
        return bukkitMaterial != null && bukkitMaterial == type;
    }

    public boolean matches(@Nullable ItemStack item) {
        if (material == null || item == null || item.getType() != bukkitMaterial) return false;
        try {
            return material.isSimilar(item);
        } catch (RuntimeException e) {
            // Items newer than this XSeries: the material already matched
            return true;
        }
    }

    /**
     * @return the target's name in the configured language, e.g. "Zombie" or "Oak Log"
     */
    @Nonnull
    public String displayName(@Nullable LanguageManager language) {
        String modern = entity != null ? XEntityType.of(entity).name() : material != null ? material.name() : name;
        String lower = modern.toLowerCase(Locale.ROOT);
        if (language != null) {
            String translated = entity != null ? language.translate("entity.minecraft." + lower) : null;
            if (translated == null && entity == null) translated = language.translate("block.minecraft." + lower);
            if (translated == null && entity == null) translated = language.translate("item.minecraft." + lower);
            if (translated != null && !translated.isEmpty()) return translated;
        }
        return titleCase(modern);
    }

    static String titleCase(String name) {
        StringBuilder result = new StringBuilder();
        for (String part : name.toLowerCase(Locale.ROOT).split("_")) {
            if (part.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
}
