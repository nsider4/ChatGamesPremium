package org.nsider.chatgames.menus;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Marks an inventory as one of this plugin's menus and holds what each slot does when
 * clicked. Each player gets their own inventory, built when the menu opens.
 */
public final class MenuHolder implements InventoryHolder {

    private final Map<Integer, Runnable> actions = new HashMap<>();
    private Inventory inventory;

    void setInventory(@Nonnull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void setAction(int slot, @Nullable Runnable action) {
        if (action == null) actions.remove(slot);
        else actions.put(slot, action);
    }

    /** @return what clicking the slot does, or null for nothing */
    @Nullable
    Runnable getAction(int slot) {
        return actions.get(slot);
    }
}
