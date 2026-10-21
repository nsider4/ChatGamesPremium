package org.nsider.chatgames.menus;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nsider.chatgames.enums.GameType;
import org.nsider.inventorymanager.item.Materials;
import org.nsider.inventorymanager.menu.Slots;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped menus.yml: every menu the code opens exists, every item is a real material and
 * sits inside its menu, no two items share a slot, and every action is one that exists and
 * opens a menu that exists.
 */
class MenusFileTest {

    private static final String[] MENUS = {"main", "profile", "challenges", "streaks", "leaderboards",
            "leaderboard-games", "leaderboard-events", "admin", "admin-start", "admin-global", "admin-events",
            "admin-presets", "admin-logs", "debug", "debug-session"};

    /** The library's own actions plus the ones {@link Menus} registers. */
    private static final Set<String> ACTIONS = new HashSet<>(Arrays.asList("command", "player", "console", "message",
            "broadcast", "sound", "open", "switch", "back", "refresh", "close", "next-page", "previous-page", "page",
            "wait", "delay", "chatgames:no-permission", "chatgames:claim-challenge", "chatgames:claim-streak",
            "chatgames:debug", "chatgames:debug-answer", "chatgames:debug-timeout", "chatgames:debug-reroll",
            "chatgames:debug-end"));

    private static final String[] ACTION_PATHS = {"actions", "left-actions", "right-actions", "shift-left-actions",
            "shift-right-actions", "middle-actions", "drop-actions", "deny-actions", "open-actions", "open-deny-actions"};

    private static YamlConfiguration file;
    private static ConfigurationSection menus;

    @BeforeAll
    static void load() throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream("src/main/resources/menus.yml"), StandardCharsets.UTF_8)) {
            file = YamlConfiguration.loadConfiguration(reader);
        }
        menus = file.getConfigurationSection("menus");
    }

    @Test
    void everyMenuExistsAndFits() {
        assertNotNull(menus);
        for (String key : MENUS) {
            ConfigurationSection menu = menus.getConfigurationSection(key);
            assertNotNull(menu, key);
            int size = menu.getInt("rows") * 9;
            assertTrue(size >= 9 && size <= 54, key + " rows");

            Map<Integer, String> used = new HashMap<>();
            ConfigurationSection items = menu.getConfigurationSection("items");
            for (String item : items == null ? new ArrayList<String>() : new ArrayList<>(items.getKeys(false))) {
                List<Integer> slots = Slots.of(items.getConfigurationSection(item));
                assertFalse(slots.isEmpty(), key + "." + item + " has no slot");
                for (int slot : slots) {
                    assertTrue(slot >= 0 && slot < size, key + "." + item + " slot " + slot);
                    // "none" only shows when the list is empty, so it may sit on a list slot
                    if (item.equals("none")) continue;
                    String other = used.put(slot, item);
                    // Two items that each show only on a condition (e.g. running / idle) may take turns
                    boolean takeTurns = other != null && items.contains(item + ".view-requirements")
                            && items.contains(other + ".view-requirements");
                    if (!takeTurns) assertEquals(null, other, key + ": " + item + " and " + other + " share slot " + slot);
                }
            }
            List<String> lists = new ArrayList<>();
            lists.add("list-slots");
            ConfigurationSection named = menu.getConfigurationSection("lists");
            if (named != null) for (String list : named.getKeys(false)) lists.add("lists." + list);
            for (String path : lists) {
                for (int slot : Slots.at(menu, path)) {
                    assertTrue(slot >= 0 && slot < size, key + "." + path + " slot " + slot);
                    assertFalse(used.containsKey(slot), key + "." + path + " slot " + slot + " holds " + used.get(slot));
                }
            }
        }
    }

    @Test
    void everyMaterialExists() {
        List<String> materials = new ArrayList<>();
        collect(file, "material", materials);
        for (GameType type : GameType.values()) {
            materials.add(file.getString("game-icons." + type.getConfigKey(), "missing icon for " + type));
        }
        for (String material : materials) {
            if (material.contains("%")) continue; // filled in per entry
            assertTrue(Materials.match(material).isPresent(), "unknown material " + material);
        }
    }

    @Test
    void everyActionExistsAndOpensARealMenu() {
        List<String> actions = new ArrayList<>();
        for (String path : ACTION_PATHS) collect(menus, path, actions);
        assertFalse(actions.isEmpty());
        for (String action : actions) {
            assertTrue(action.startsWith("[") && action.indexOf(']') > 0, "not an action: " + action);
            String name = action.substring(1, action.indexOf(']')).toLowerCase(Locale.ROOT);
            assertTrue(ACTIONS.contains(name), "unknown action " + action);
            String argument = action.substring(action.indexOf(']') + 1).trim();
            if ((name.equals("open") || name.equals("switch") || name.equals("back")) && !argument.isEmpty()) {
                String target = argument.split("\\s+")[0];
                assertNotNull(menus.getConfigurationSection(target), action + " opens a menu that doesn't exist");
            }
        }
    }

    private static void collect(ConfigurationSection section, String key, List<String> values) {
        for (String child : section.getKeys(false)) {
            if (section.isConfigurationSection(child)) {
                collect(section.getConfigurationSection(child), key, values);
            } else if (child.equals(key)) {
                if (section.isList(child)) values.addAll(section.getStringList(child));
                else values.add(section.getString(child));
            }
        }
    }

    @Test
    void togglesHaveTheirOffLook() {
        for (String path : new String[]{"main.items.games-toggle", "main.items.sound-toggle", "challenges.items.tab-daily",
                "challenges.items.tab-weekly", "admin.items.toggle", "admin.items.toggle-all"}) {
            assertNotNull(menus.getConfigurationSection(path + ".when-off"), path);
            assertNotNull(menus.getString(path + ".state"), path + " has no state");
        }
    }

    @Test
    void wrongCaseSwapsLettersOnly() {
        assertEquals("cREEPER 12", DebugMenu.swapCase("Creeper 12"));
        assertEquals("42", DebugMenu.swapCase("42"));
    }
}
