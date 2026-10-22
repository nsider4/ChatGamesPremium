package org.nsider.chatgames.hooks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reads MythicMobs' mobs, for Hunt races that target {@code mythic:<MobType>}.
 *
 * <p>Uses reflection, so ChatGames has no compile dependency on MythicMobs and works the same
 * without it. Both MythicMobs 5 ({@code io.lumine.mythic}) and 4 ({@code io.lumine.xikage.mythicmobs})
 * are supported: their mob managers have the same three methods used here. Any call that fails
 * (a MythicMobs version that renamed them) is logged once and then treated as "no MythicMobs".</p>
 */
public final class MythicHook {

    /** How races.yml and commands mark a MythicMobs mob type, e.g. {@code mythic:SkeletonKing}. */
    public static final String PREFIX = "mythic:";

    private static final String[] MAIN_CLASSES = {
            "io.lumine.mythic.bukkit.MythicBukkit",
            "io.lumine.xikage.mythicmobs.MythicMobs"
    };

    @Nullable private static volatile Object mobManager;
    private static volatile boolean failed;

    private MythicHook() {}

    /**
     * Finds MythicMobs' mob manager. Called on enable; MythicMobs loads first (softdepend).
     *
     * @return true if MythicMobs is installed and usable
     */
    public static boolean initialize(@Nonnull Logger logger) {
        mobManager = null;
        failed = false;
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) return false;
        for (String name : MAIN_CLASSES) {
            try {
                Class<?> main = Class.forName(name);
                Object instance = main.getMethod("inst").invoke(null);
                mobManager = call(instance, "getMobManager");
                logger.info("Hooked into MythicMobs: Hunt races can target mythic:<mob type>.");
                return true;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                // Try the other version's class
            }
        }
        logger.warning("MythicMobs is installed but its API wasn't recognised; mythic: race targets are disabled.");
        return false;
    }

    public static boolean isEnabled() {
        return mobManager != null && !failed;
    }

    /** @return true for a race target written as {@code mythic:<MobType>} */
    public static boolean isMythicName(@Nonnull String name) {
        return name.trim().toLowerCase(Locale.ROOT).startsWith(PREFIX);
    }

    /**
     * @return the entity's MythicMobs mob type (its internal name), or null if it isn't a MythicMobs mob
     */
    @Nullable
    public static String mobType(@Nonnull Entity entity) {
        Object manager = mobManager;
        if (manager == null || failed) return null;
        try {
            Object activeMob = unwrap(call(manager, "getActiveMob", UUID.class, entity.getUniqueId()));
            if (activeMob == null) return null;
            Object type = call(activeMob, "getMobType");
            return type == null ? null : type.toString();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            disable(e);
            return null;
        }
    }

    /**
     * @param name a mob type, with or without the {@code mythic:} prefix, in any case
     * @return the mob type as MythicMobs spells it, or null if MythicMobs has no such mob
     */
    @Nullable
    public static String findMobType(@Nonnull String name) {
        String wanted = stripPrefix(name);
        for (String type : mobTypes()) {
            if (type.equalsIgnoreCase(wanted)) return type;
        }
        return null;
    }

    /** @return every MythicMobs mob type, or none without MythicMobs */
    @Nonnull
    public static List<String> mobTypes() {
        Object manager = mobManager;
        if (manager == null || failed) return Collections.emptyList();
        try {
            Object names = call(manager, "getMobNames");
            List<String> types = new ArrayList<>();
            if (names instanceof Collection) {
                for (Object type : (Collection<?>) names) types.add(String.valueOf(type));
            }
            return types;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            disable(e);
            return Collections.emptyList();
        }
    }

    /**
     * @return the mob's display name from its MythicMobs config without colours, or null if it has none
     */
    @Nullable
    public static String displayName(@Nonnull String mobType) {
        Object manager = mobManager;
        if (manager == null || failed) return null;
        try {
            Object mob = unwrap(call(manager, "getMythicMob", String.class, mobType));
            if (mob == null) return null;
            Object display = call(mob, "getDisplayName");
            // MythicMobs 5 returns a PlaceholderString (get() is its text); old versions a String
            if (display != null && !(display instanceof String)) {
                display = call(display, "get");
            }
            if (display == null) return null;
            String plain = ChatColor.stripColor(MessageUtils.color(display.toString())).trim();
            return plain.isEmpty() ? null : plain;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            // A mob without a display name can throw here; that isn't a broken hook
            return null;
        }
    }

    @Nonnull
    public static String stripPrefix(@Nonnull String name) {
        String trimmed = name.trim();
        return isMythicName(trimmed) ? trimmed.substring(PREFIX.length()).trim() : trimmed;
    }

    @Nullable
    private static Object call(Object target, String method) throws ReflectiveOperationException {
        Method found = target.getClass().getMethod(method);
        // The object's class may be non-public even though the method is public
        found.setAccessible(true);
        return found.invoke(target);
    }

    @Nullable
    private static Object call(Object target, String method, Class<?> type, Object argument) throws ReflectiveOperationException {
        Method found = target.getClass().getMethod(method, type);
        found.setAccessible(true);
        return found.invoke(target, argument);
    }

    @Nullable
    private static Object unwrap(@Nullable Object value) {
        if (value instanceof Optional) return ((Optional<?>) value).orElse(null);
        return value;
    }

    private static void disable(Throwable cause) {
        if (failed) return;
        failed = true;
        Bukkit.getLogger().warning("[ChatGames] MythicMobs' API changed (" + cause + "); mythic: race targets are disabled.");
    }
}
