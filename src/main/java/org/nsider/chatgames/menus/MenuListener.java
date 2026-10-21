package org.nsider.chatgames.menus;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nsider.chatgames.ChatGames;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.logging.Level;

/**
 * Keeps items in place in every menu and runs the clicked slot's action. Runs on the
 * clicking player's thread (their region on Folia).
 *
 * <p>{@code event.getView()} is avoided: InventoryView became an interface in 1.21, so
 * calling it from code built against 1.8 fails. {@code getInventory()} is the top inventory.</p>
 */
public final class MenuListener implements Listener {

    private final ChatGames plugin;

    public MenuListener(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return; // the player's own inventory
        Runnable action = ((MenuHolder) event.getInventory().getHolder()).getAction(slot);
        if (action == null) return;
        try {
            action.run();
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "A menu button failed", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getMenus().getDebug().end(event.getPlayer().getUniqueId());
    }
}
