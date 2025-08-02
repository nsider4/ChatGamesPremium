package org.nsider.chatgames.inventory;

import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.inventory.InventoryBuilder;
import org.nsider.inventorymanager.inventory.InventoryHandler;
import org.nsider.inventorymanager.item.ItemBuilder;
import org.nsider.inventorymanager.item.ItemData;

import java.util.*;

/**
 * Class for managing inventories.
 * This class provides methods for creating inventories, adding items to inventories, and sending inventories to players.
 */
public class InventoryManagement {

    private static ChatGames plugin;
    /**
     * The map of inventories for each player.
     * The key is the player's UUID, and the value is the inventory ID.
     */
    @Getter private static HashMap<UUID, Integer> playerPage = new HashMap<>();

    /**
     * Private constructor to initialize the InventoryManagement instance.
     *
     * @param plugin the ChatSystem plugin instance
     */
    private InventoryManagement(ChatGames plugin) {
        InventoryManagement.plugin = plugin;
        //initialize inventories
    }

    /**
     * Initializes the InventoryManagement class with the plugin instance.
     * Ensures that only one instance of InventoryManagement is created.
     *
     * @param plugin the ChatSystem plugin instance
     */
    public static void initialize(ChatGames plugin) {
        new InventoryManagement(plugin);
    }

    /**
     * Loads the inventories for each menu.
     */
    public static void loadBaseInventories() {
        for(Menu menu : Menu.values()) {
            loadInventory(menu);
        }
    }

    public static void reloadBaseInventories() {
        InventoryHandler.getInstance().clearAllInventories();

        loadBaseInventories();
    }

    /**
     * Gets the inventory builder for the specified menu.
     *
     * @param menu the menu to get the {@link InventoryBuilder} for
     */
    protected static InventoryBuilder getMenu(Menu menu) {
        InventoryHandler handler = InventoryHandler.getInstance();

        //TODO LOGIC PER MENU

        return null;
    }

    /**
     * Opens the inventory for the specified menu and applies placeholder replacements if needed.
     *
     * @param player The player to open the inventory for.
     * @param menu   The menu to open the inventory for.
     */
    public static void openInventory(@Nonnull Player player, @Nonnull Menu menu) {
        InventoryBuilder inventoryBuilder = getMenu(menu);
        if (inventoryBuilder == null) return;

        applyPlaceholderItems(player, inventoryBuilder, menu);

        inventoryBuilder.openInventory(player);
    }

    /**
     * Applies placeholder replacements to items marked as placeholders in the configuration.
     * This modifies items dynamically based on the player's information.
     *
     * @param player           The player whose placeholders will be used.
     * @param inventoryBuilder The inventory builder where placeholders need to be applied.
     * @param menu             The menu being processed.
     */
    private static void applyPlaceholderItems(@Nonnull Player player, @Nonnull InventoryBuilder inventoryBuilder, @Nonnull Menu menu) {
        YamlConfig config = menu.getConfig();
        String sectionPath = menu.getSection();
        ConfigurationSection itemsSection = config.getConfigurationSection(sectionPath + ".Items");
        if (itemsSection == null) return;

        for (String item : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(item);

            if (itemSection.getBoolean("placeholder", false)) {
                String rawName = color(itemSection.getString("name"));
                List<String> rawLore = colorList(itemSection.getStringList("lore"));

                Optional<ItemData> itemDataOptional = inventoryBuilder.getItemDataWithName(rawName);

                if (itemDataOptional.isPresent()) {
                    ItemData itemData = itemDataOptional.get();

                    String modifiedName = PlaceholderAPI.setPlaceholders(player, rawName);
                    List<String> modifiedLore = PlaceholderAPI.setPlaceholders(player, rawLore);

                    ItemStack newItem = ItemBuilder.fromItemData(itemData);

                    ItemMeta itemMeta = newItem.getItemMeta();
                    if (itemMeta != null) {
                        itemMeta.setDisplayName(modifiedName);
                        itemMeta.setLore(modifiedLore);
                        newItem.setItemMeta(itemMeta);
                    }

                    itemData.getSlots().forEach(slot -> inventoryBuilder.getInventory().setItem(slot, newItem));
                } else {
                    plugin.getLogger().warning("No item data found for item: " + item);
                }
            }
        }
    }


    /**
     * Creates an inventory with the specified menu.
     *
     * @param menu the menu to create the inventory for
     */
    public static InventoryBuilder createInventory(Menu menu) {
        YamlConfig config = menu.getConfig();
        String section = menu.getSection();

        InventoryBuilder builder = new InventoryBuilder(
                color(config.getString(section + ".GUIName")),
                config.getInt(section + ".GUISize")
        );

        ConfigurationSection itemsSection = config.getConfigurationSection(section + ".Items");

        if (itemsSection != null) {
            itemsSection.getKeys(false).forEach(item -> {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(item);

                if (itemSection != null) {
                    ItemData data = ItemData.builder()
                            .setMaterial(itemSection.getString("material"))
                            .setName(color(itemSection.getString("name")))
                            .setLore(colorList(itemSection.getStringList("lore")))
                            .setAmount(itemSection.getInt("amount"))
                            .setSlots(itemSection.getIntegerList("slots"))
                            .build();

                    builder.addItem(data);
                }
            });

            builder.save();
        }

        return builder;
    }

    /**
     * Loads an inventory with the specified menu.
     *
     * @param baseMenu the menu to load the inventory for
     */
    public static void loadInventory(
            Menu baseMenu)
    {

        YamlConfig config = baseMenu.getConfig();
        String section = baseMenu.getSection();

        InventoryBuilder builder = createInventory(baseMenu);
        builder.load();
    }

    /**
     * Sets the items in empty slots in an inventory.
     *
     * @param builder the inventory builder
     * @param items the list of item data
     */
    public static void setItemsInEmptySlots(InventoryBuilder builder, List<ItemData> items) {
        InventoryBuilder baseBuilder = InventoryHandler.getInstance().getInventoryBuilder(builder.getBaseInventoryName());

        if(baseBuilder == null) {
            return;
        }

        List<Integer> emptySlots = getEmptySlots(baseBuilder);

        for (int i = 0; i < emptySlots.size() && !items.isEmpty(); i++) {
            int emptySlot = emptySlots.get(i);

            ItemData itemData = items.remove(0);
            itemData.setSlots(Collections.singletonList(emptySlot));
            builder.addItem(itemData);
        }
    }

    /**
     * Gets a list of empty slots in an inventory.
     *
     * @param builder the inventory builder
     * @return a list of empty slots
     */
    protected static List<Integer> getEmptySlots(InventoryBuilder builder) {
        List<Integer> emptySlots = new ArrayList<>();

        for (int i = 0; i < builder.getInventorySize(); i++) {
            int finalI = i;
            boolean isSlotOccupied = builder.getItemsMap().values().stream()
                    .anyMatch(slots -> slots.contains(finalI));

            if (!isSlotOccupied) {
                emptySlots.add(i);
            }
        }

        return emptySlots;
    }

    /**
     * Gets a page of items from a list.
     *
     * @param allItems the list of items
     * @param pageNumber the page number
     * @param pageLength the number of items per page
     * @return a list of items on the specified page
     */
    public static <T> List<T> getPage(List<T> allItems, int pageNumber, int pageLength) {
        List<T> itemsPerPage = new ArrayList<>();

        int startIndex = (pageNumber - 1) * pageLength;
        if (startIndex >= allItems.size()) {
            return itemsPerPage;
        }
        int endIndex = Math.min(startIndex + pageLength, allItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            itemsPerPage.add(allItems.get(i));
        }

        return itemsPerPage;
    }

    /**
     * Adds the player into the player page map
     *
     * @param p       The player to add
     * @param page    The page to add the player to
     */
    public static void openPage(Player p, int page) {
        playerPage.put(p.getUniqueId(), page);
    }

    /**
     * Removes the player from the player page map
     *
     * @param p The player to remove
     */
    public static void closePages(Player p) {
        playerPage.remove(p.getUniqueId());
    }

    private static String color(String message) {
        return MessageUtils.color(message);
    }

    private static List<String> colorList(List<String> list) {
        return MessageUtils.colorList(list);
    }
}


