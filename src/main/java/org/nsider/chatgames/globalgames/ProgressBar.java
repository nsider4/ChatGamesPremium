package org.nsider.chatgames.globalgames;

import com.cryptomorin.xseries.messages.ActionBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A global game's progress: a boss bar from 1.9, the action bar on 1.8 (which has no
 * boss bar API). The plugin is built against 1.8, so the boss bar is reached by
 * reflection. Main/global thread only.
 */
final class ProgressBar {

    @Nullable private static BossBarApi api;
    private static boolean looked;

    private final GlobalGameConfig.Bar settings;
    private final Logger logger;
    @Nullable private Object bar;
    private final Set<UUID> viewers = new HashSet<>();

    ProgressBar(@Nonnull GlobalGameConfig.Bar settings, @Nonnull Logger logger) {
        this.settings = settings;
        this.logger = logger;
    }

    /**
     * Shows the bar to these players (and only them), with this text and fill.
     *
     * @param progress 0 to 1
     */
    void show(@Nonnull Collection<? extends Player> players, @Nonnull String text, double progress) {
        if (!settings.isEnabled()) return;
        double fill = Math.max(0, Math.min(1, progress));
        BossBarApi boss = api(logger);
        if (boss == null) {
            for (Player player : players) ActionBar.sendActionBar(player, text);
            return;
        }
        try {
            if (bar == null) bar = boss.create(text, settings.getColor(), settings.getStyle(), logger);
            boss.setTitle.invoke(bar, text);
            boss.setProgress.invoke(bar, fill);
            Set<UUID> now = new HashSet<>();
            for (Player player : players) {
                now.add(player.getUniqueId());
                if (viewers.add(player.getUniqueId())) boss.addPlayer.invoke(bar, player);
            }
            for (UUID uuid : new HashSet<>(viewers)) {
                if (now.contains(uuid)) continue;
                viewers.remove(uuid);
                Player gone = Bukkit.getPlayer(uuid);
                if (gone != null) boss.removePlayer.invoke(bar, gone);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            logger.log(Level.WARNING, "Could not update the global game's boss bar", e);
        }
    }

    /** Removes the bar from everyone. */
    void hide() {
        BossBarApi boss = api;
        if (bar != null && boss != null) {
            try {
                boss.removeAll.invoke(bar);
            } catch (ReflectiveOperationException | RuntimeException e) {
                logger.log(Level.WARNING, "Could not remove the global game's boss bar", e);
            }
        }
        bar = null;
        viewers.clear();
    }

    @Nullable
    private static synchronized BossBarApi api(Logger logger) {
        if (!looked) {
            looked = true;
            try {
                api = new BossBarApi();
            } catch (ClassNotFoundException e) {
                api = null; // 1.8: the action bar it is
            } catch (ReflectiveOperationException | RuntimeException e) {
                logger.log(Level.WARNING, "No boss bar on this server; the global progress shows in the action bar", e);
                api = null;
            }
        }
        return api;
    }

    /** {@code Bukkit.createBossBar} and the BossBar methods used, found once. */
    private static final class BossBarApi {
        final Class<?> color;
        final Class<?> style;
        final Class<?> flag;
        final Method create;
        final Method setTitle;
        final Method setProgress;
        final Method addPlayer;
        final Method removePlayer;
        final Method removeAll;

        BossBarApi() throws ReflectiveOperationException {
            color = Class.forName("org.bukkit.boss.BarColor");
            style = Class.forName("org.bukkit.boss.BarStyle");
            flag = Class.forName("org.bukkit.boss.BarFlag");
            Class<?> bossBar = Class.forName("org.bukkit.boss.BossBar");
            create = Bukkit.class.getMethod("createBossBar", String.class, color, style, Array.newInstance(flag, 0).getClass());
            setTitle = bossBar.getMethod("setTitle", String.class);
            setProgress = bossBar.getMethod("setProgress", double.class);
            addPlayer = bossBar.getMethod("addPlayer", Player.class);
            removePlayer = bossBar.getMethod("removePlayer", Player.class);
            removeAll = bossBar.getMethod("removeAll");
        }

        Object create(String text, String colorName, String styleName, Logger logger) throws ReflectiveOperationException {
            return create.invoke(null, text, constant(color, colorName, "YELLOW", logger),
                    constant(style, styleName, "SOLID", logger), Array.newInstance(flag, 0));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Object constant(Class<?> type, String name, String fallback, Logger logger) {
            try {
                return Enum.valueOf((Class) type, name);
            } catch (IllegalArgumentException e) {
                logger.warning("global_games.yml: progress-bar has no " + type.getSimpleName() + " " + name + "; using " + fallback);
                return Enum.valueOf((Class) type, fallback);
            }
        }
    }
}
