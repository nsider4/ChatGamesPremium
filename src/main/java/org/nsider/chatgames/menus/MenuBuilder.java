package org.nsider.chatgames.menus;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.utils.gameplay.VersionUtils;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;

/**
 * Builds one player's copy of a menu from its menus.yml section:
 * <pre>
 * title: '...'        rows: 1-6
 * filler: {material: ..., name: ' '}     fills every slot left empty
 * items:
 *   key: {slot: 10 (or slots: [..]), material, name, lore, amount, glow, skull-owner, when-off: {...}}
 * list-slots: [...]   where list menus (challenges, games...) put their entries
 * templates:          what each kind of list entry looks like
 * </pre>
 * Placeholders set with {@link #with} fill the title, names, lore and the material. A lore
 * line that is exactly a list placeholder ({@link #withList}) becomes that many lines.
 * PlaceholderAPI placeholders work too when it is installed.
 */
public final class MenuBuilder {

    /** Inventory titles longer than this throw on 1.8. */
    private static final int LEGACY_TITLE_LIMIT = 32;

    private final ChatGames plugin;
    private final ConfigurationSection menu;
    private final Player viewer;
    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, List<String>> lists = new HashMap<>();
    private final Map<Integer, ItemStack> items = new HashMap<>();
    private final MenuHolder holder = new MenuHolder();

    private MenuBuilder(ChatGames plugin, ConfigurationSection menu, Player viewer) {
        this.plugin = plugin;
        this.menu = menu;
        this.viewer = viewer;
    }

    /**
     * @param key the menu's section in menus.yml
     */
    @Nonnull
    public static MenuBuilder of(@Nonnull ChatGames plugin, @Nonnull String key, @Nonnull Player viewer) {
        ConfigurationSection section = ConfigManager.getMenus().getConfigurationSection(key);
        if (section == null) {
            plugin.getLogger().warning("menus.yml has no '" + key + "' menu; it opens empty.");
            section = new MemoryConfiguration();
        }
        return new MenuBuilder(plugin, section, viewer).with("%player%", viewer.getName());
    }

    @Nonnull
    public MenuBuilder with(@Nonnull String placeholder, @Nullable Object value) {
        values.put(placeholder, value == null ? "" : String.valueOf(value));
        return this;
    }

    @Nonnull
    public MenuBuilder withList(@Nonnull String placeholder, @Nonnull List<String> lines) {
        lists.put(placeholder, lines);
        return this;
    }

    @Nonnull
    public ConfigurationSection getSection() {
        return menu;
    }

    /** @return the menu's text setting (e.g. a format under {@code formats}), with colours */
    @Nonnull
    public String text(@Nonnull String path, @Nonnull String fallback) {
        return MessageUtils.color(menu.getString(path, fallback));
    }

    /**
     * Places a configured item.
     *
     * @return false if menus.yml leaves the item out
     */
    public boolean item(@Nonnull String key, @Nullable Runnable action) {
        ConfigurationSection section = menu.getConfigurationSection("items." + key);
        if (section == null) return false;
        place(slotsOf(section), section, Collections.<String, String>emptyMap(), action);
        return true;
    }

    /**
     * Places an item with an on and an off look: when off, its {@code when-off} section's
     * keys replace the item's own. (Not "off": YAML reads a bare off as the boolean false.)
     */
    public boolean item(@Nonnull String key, boolean on, @Nullable Runnable action) {
        ConfigurationSection section = menu.getConfigurationSection("items." + key);
        if (section == null) return false;
        ConfigurationSection off = section.getConfigurationSection("when-off");
        place(slotsOf(section), on || off == null ? section : overlay(section, off), Collections.<String, String>emptyMap(), action);
        return true;
    }

    /** @return the template, from the menu's {@code templates} section; null if missing */
    @Nullable
    public ConfigurationSection template(@Nonnull String key) {
        return menu.getConfigurationSection("templates." + key);
    }

    /**
     * Places one list entry or any other item built from a template, with its own placeholders
     * on top of the menu's.
     */
    public void place(int slot, @Nullable ConfigurationSection template, @Nonnull Map<String, String> extra,
                      @Nullable Runnable action) {
        if (template == null) return;
        place(Collections.singletonList(slot), template, extra, action);
    }

    private void place(List<Integer> slots, ConfigurationSection section, Map<String, String> extra, Runnable action) {
        ItemStack item = build(section, extra);
        int size = rows() * 9;
        for (int slot : slots) {
            if (slot < 0 || slot >= size) continue;
            items.put(slot, item);
            holder.setAction(slot, action);
        }
    }

    /** @return the slots list entries go in, in order */
    @Nonnull
    public List<Integer> listSlots(@Nonnull String path) {
        return menu.getIntegerList(path);
    }

    /**
     * Fills {@code list-slots} with one page of entries, and shows the {@code previous} and
     * {@code next} items when there are other pages. {@code %page%} and {@code %pages%} are set.
     *
     * @param reopen opens the menu again at another page (from 0)
     */
    public <T> void paged(@Nonnull List<T> entries, int page, @Nonnull EntryRenderer<T> render, @Nonnull IntConsumer reopen) {
        List<Integer> slots = listSlots("list-slots");
        if (slots.isEmpty()) return;
        int pages = Math.max(1, (entries.size() + slots.size() - 1) / slots.size());
        int current = Math.max(0, Math.min(page, pages - 1));
        with("%page%", current + 1).with("%pages%", pages);
        for (int i = 0; i < slots.size(); i++) {
            int index = current * slots.size() + i;
            if (index >= entries.size()) break;
            render.render(slots.get(i), entries.get(index));
        }
        if (current > 0) item("previous", () -> reopen.accept(current - 1));
        if (current < pages - 1) item("next", () -> reopen.accept(current + 1));
    }

    /** Draws one list entry in its slot. */
    public interface EntryRenderer<T> {
        void render(int slot, T entry);
    }

    /**
     * Builds the inventory and opens it on the player's thread (next tick, which is also
     * the safe way to switch menus from inside a click).
     */
    public void open() {
        int size = rows() * 9;
        String title = fill(menu.getString("title", "&8ChatGames"), Collections.<String, String>emptyMap());
        if (!VersionUtils.isAtLeast(1, 9) && title.length() > LEGACY_TITLE_LIMIT) {
            title = title.substring(0, LEGACY_TITLE_LIMIT);
        }
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);

        ConfigurationSection filler = menu.getConfigurationSection("filler");
        ItemStack fill = filler == null ? null : build(filler, Collections.<String, String>emptyMap());
        for (int slot = 0; slot < size; slot++) {
            ItemStack item = items.get(slot);
            if (item == null) item = fill;
            if (item != null) inventory.setItem(slot, item);
        }
        plugin.getScheduler().atPlayer(viewer, () -> viewer.openInventory(inventory));
    }

    private int rows() {
        return Math.max(1, Math.min(6, menu.getInt("rows", 3)));
    }

    private static List<Integer> slotsOf(ConfigurationSection section) {
        if (section.isList("slots")) return section.getIntegerList("slots");
        return Collections.singletonList(section.getInt("slot", -1));
    }

    /** @return the base's values with the override's on top (one level; nested sections are skipped) */
    @Nonnull
    public static ConfigurationSection overlay(@Nonnull ConfigurationSection base, @Nonnull ConfigurationSection override) {
        MemoryConfiguration merged = new MemoryConfiguration();
        for (String key : base.getKeys(false)) {
            if (!base.isConfigurationSection(key)) merged.set(key, base.get(key));
        }
        for (String key : override.getKeys(false)) {
            if (!override.isConfigurationSection(key)) merged.set(key, override.get(key));
        }
        return merged;
    }

    private ItemStack build(ConfigurationSection section, Map<String, String> extra) {
        String materialName = replace(section.getString("material", "stone"), extra).trim();
        Optional<XMaterial> material = XMaterial.matchXMaterial(materialName);
        ItemStack item = material.isPresent() && material.get().isSupported() ? material.get().parseItem() : null;
        if (item == null) {
            plugin.getLogger().warning("menus.yml: '" + materialName + "' is not an item on this server version; showing stone.");
            item = XMaterial.STONE.parseItem();
        }
        item.setAmount(Math.max(1, Math.min(64, section.getInt("amount", 1))));

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (section.contains("name")) meta.setDisplayName(fill(section.getString("name"), extra));
        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            // An entry can point its own placeholder at a list, e.g. %top% -> %top_math%
            List<String> expanded = lists.get(replace(line, extra).trim());
            if (expanded != null) {
                for (String part : expanded) lore.add(fill(part, extra));
            } else {
                lore.add(fill(line, extra));
            }
        }
        if (!lore.isEmpty()) meta.setLore(lore);
        if (section.getBoolean("glow", false)) {
            Enchantment glow = XEnchantment.UNBREAKING.getEnchant();
            if (glow != null) meta.addEnchant(glow, 1, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        String owner = section.getString("skull-owner");
        if (owner != null && meta instanceof SkullMeta) {
            setOwner((SkullMeta) meta, replace(owner, extra));
        }
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    private static void setOwner(SkullMeta meta, String name) {
        // The only owner setter in the 1.8 API; still present on 26.x
        meta.setOwner(name);
    }

    private String fill(String text, Map<String, String> extra) {
        String filled = replace(text, extra);
        if (plugin.isPlaceHolderAPIEnabled() && filled.indexOf('%') >= 0) {
            filled = PlaceholderAPI.setPlaceholders(viewer, filled);
        }
        return MessageUtils.color(filled);
    }

    private String replace(String text, Map<String, String> extra) {
        if (text == null) return "";
        String result = text;
        for (Map.Entry<String, String> entry : extra.entrySet()) result = result.replace(entry.getKey(), entry.getValue());
        for (Map.Entry<String, String> entry : values.entrySet()) result = result.replace(entry.getKey(), entry.getValue());
        return result;
    }
}
