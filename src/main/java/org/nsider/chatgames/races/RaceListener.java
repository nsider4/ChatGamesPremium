package org.nsider.chatgames.races;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.inventorymanager.extras.Nullable;

/**
 * Counts what players do towards the running race. Listens at MONITOR and skips
 * cancelled events, so only actions that really happened count.
 *
 * <p>On Folia these events arrive on the region thread of the player (or mob);
 * {@link RaceProgress} makes the counting safe.</p>
 */
public final class RaceListener implements Listener {

    /** Marks mobs from spawners and spawn eggs, which Hunt can be set to ignore. */
    private static final String SPAWNER_MOB = "chatgames_spawner_mob";
    /** The player inventory's storage slots (hotbar and main), the same on every version. */
    private static final int STORAGE_SLOTS = 36;

    private final ChatGames plugin;

    public RaceListener(ChatGames plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (countsSpawnerMobs() || !isSpawnerReason(event.getSpawnReason())) return;
        event.getEntity().setMetadata(SPAWNER_MOB, new FixedMetadataValue(plugin, true));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHunt(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        boolean fromSpawner = entity.hasMetadata(SPAWNER_MOB);
        // Dead entities keep their metadata until removed
        if (fromSpawner) entity.removeMetadata(SPAWNER_MOB, plugin);

        Race race = running(GameType.HUNT, entity.getKiller());
        if (race == null || !race.getTarget().matches(entity.getType())) return;
        if (fromSpawner && !countsSpawnerMobs()) return;
        plugin.getRaceManager().progress(race, entity.getKiller(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMine(BlockBreakEvent event) {
        Race race = running(GameType.MINE, event.getPlayer());
        if (race != null && race.getTarget().matchesBlock(event.getBlock().getType())) {
            plugin.getRaceManager().progress(race, event.getPlayer(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Race race = running(GameType.PLACE, event.getPlayer());
        if (race == null) return;
        // Some items place a different block (redstone dust, seeds), so either may match
        if (race.getTarget().matches(event.getItemInHand()) || race.getTarget().matchesBlock(event.getBlockPlaced().getType())) {
            plugin.getRaceManager().progress(race, event.getPlayer(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item)) return;
        Race race = running(GameType.FISH, event.getPlayer());
        if (race != null && race.getTarget().matches(((Item) event.getCaught()).getItemStack())) {
            plugin.getRaceManager().progress(race, event.getPlayer(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent event) {
        Race race = running(GameType.EAT, event.getPlayer());
        if (race != null && race.getTarget().matches(event.getItem())) {
            plugin.getRaceManager().progress(race, event.getPlayer(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) return;
        Player player = (Player) clicker;
        Race race = running(GameType.CRAFT, player);
        if (race == null) return;
        ItemStack result = event.getCurrentItem();
        if (isEmpty(result)) result = event.getRecipe().getResult();
        if (isEmpty(result) || !race.getTarget().matches(result)) return;

        int crafted = crafted(event, player, result);
        if (crafted > 0) plugin.getRaceManager().progress(race, player, crafted);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFurnace(FurnaceExtractEvent event) {
        Race race = running(GameType.FURNACE, event.getPlayer());
        if (race != null && race.getTarget().matchesBlock(event.getItemType()) && event.getItemAmount() > 0) {
            plugin.getRaceManager().progress(race, event.getPlayer(), event.getItemAmount());
        }
    }

    /**
     * @return the running race if it is this type and the player can take part in it
     */
    @Nullable
    private Race running(GameType type, @Nullable Player player) {
        Race race = plugin.getRaceManager().getCurrent();
        if (race == null || race.getType() != type || player == null || race.getProgress().isOver()) return null;
        if (player.getGameMode() == GameMode.CREATIVE && !ConfigValue.getBoolean(ConfigPaths.RACES_CREATIVE_ENABLED)) return null;
        if (!plugin.getGameManager().worldEnabled(player) || !plugin.getGameManager().gamesIsToggled(player)) return null;
        return race;
    }

    /**
     * Bukkit fires one event per click; a shift-click crafts many times. Uses the
     * player's own inventory rather than the event's view: InventoryView changed from
     * a class to an interface in 1.21, so calling it from code built against 1.8 fails.
     */
    private static int crafted(CraftItemEvent event, Player player, ItemStack result) {
        ClickType click = event.getClick();
        int perCraft = result.getAmount();
        if (click.isShiftClick()) {
            return RaceCounts.shiftCrafted(perCraft, fewestIngredient(event.getInventory()), room(player.getInventory(), result));
        }
        switch (click) {
            case NUMBER_KEY:
                return isEmpty(player.getInventory().getItem(event.getHotbarButton())) ? perCraft : 0;
            case DROP:
            case CONTROL_DROP:
                return perCraft;
            case MIDDLE:
            case CREATIVE:
                return 0;
            default:
                ItemStack cursor = event.getCursor();
                return RaceCounts.clickCrafted(perCraft, isEmpty(cursor), cursor != null && cursor.isSimilar(result),
                        cursor == null ? 0 : cursor.getAmount(), result.getMaxStackSize());
        }
    }

    private static int fewestIngredient(CraftingInventory inventory) {
        int fewest = 0;
        for (ItemStack item : inventory.getMatrix()) {
            if (isEmpty(item)) continue;
            if (fewest == 0 || item.getAmount() < fewest) fewest = item.getAmount();
        }
        return fewest;
    }

    private static int room(PlayerInventory inventory, ItemStack item) {
        int room = 0;
        for (int slot = 0; slot < STORAGE_SLOTS; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isEmpty(stack)) {
                room += item.getMaxStackSize();
            } else if (stack.isSimilar(item)) {
                room += Math.max(item.getMaxStackSize() - stack.getAmount(), 0);
            }
        }
        return room;
    }

    private static boolean isEmpty(@Nullable ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    private static boolean countsSpawnerMobs() {
        return ConfigValue.getBoolean(ConfigPaths.RACE_HUNT_COUNT_SPAWNER_MOBS);
    }

    private static boolean isSpawnerReason(CreatureSpawnEvent.SpawnReason reason) {
        // TRIAL_SPAWNER only exists from 1.21
        return reason == CreatureSpawnEvent.SpawnReason.SPAWNER || reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
                || "TRIAL_SPAWNER".equals(reason.name());
    }
}
