package org.nsider.chatgames.menus;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nsider.chatgames.enums.GameType;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped menus.yml: every item is a real material, sits inside its menu, and no two
 * items share a slot. Every menu the code opens exists.
 */
class MenusFileTest {

    private static final String[] MENUS = {"main", "profile", "challenges", "streaks", "leaderboards",
            "leaderboard-games", "leaderboard-events", "admin", "admin-start", "admin-global", "admin-events",
            "admin-presets", "admin-logs", "debug", "debug-session"};

    private static YamlConfiguration file;

    @BeforeAll
    static void load() throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream("src/main/resources/menus.yml"), StandardCharsets.UTF_8)) {
            file = YamlConfiguration.loadConfiguration(reader);
        }
    }

    @Test
    void everyMenuExistsAndFits() {
        for (String key : MENUS) {
            ConfigurationSection menu = file.getConfigurationSection(key);
            assertNotNull(menu, key);
            int size = menu.getInt("rows") * 9;
            assertTrue(size >= 9 && size <= 54, key + " rows");

            Map<Integer, String> used = new HashMap<>();
            ConfigurationSection items = menu.getConfigurationSection("items");
            for (String item : items == null ? new ArrayList<String>() : new ArrayList<>(items.getKeys(false))) {
                ConfigurationSection section = items.getConfigurationSection(item);
                List<Integer> slots = section.isList("slots") ? section.getIntegerList("slots")
                        : java.util.Collections.singletonList(section.getInt("slot", -1));
                for (int slot : slots) {
                    assertTrue(slot >= 0 && slot < size, key + "." + item + " slot " + slot);
                    // "none" only shows when the list is empty, so it may sit on a list slot
                    if (!item.equals("none")) {
                        String other = used.put(slot, item);
                        assertEquals(null, other, key + ": " + item + " and " + other + " share slot " + slot);
                    }
                }
            }
            for (String path : new String[]{"list-slots", "delay-slots", "ladders.challenge", "ladders.daily", "ladders.global"}) {
                for (int slot : menu.getIntegerList(path)) {
                    assertTrue(slot >= 0 && slot < size, key + "." + path + " slot " + slot);
                    assertFalse(used.containsKey(slot), key + "." + path + " slot " + slot + " holds " + used.get(slot));
                }
            }
        }
    }

    @Test
    void everyMaterialExists() {
        List<String> materials = new ArrayList<>();
        collect(file, materials);
        for (GameType type : GameType.values()) {
            materials.add(file.getString("game-icons." + type.getConfigKey(), "missing icon for " + type));
        }
        for (String material : materials) {
            if (material.contains("%")) continue; // filled in per entry
            assertTrue(XMaterial.matchXMaterial(material).isPresent(), "unknown material " + material);
        }
    }

    private static void collect(ConfigurationSection section, List<String> materials) {
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) collect(section.getConfigurationSection(key), materials);
            else if (key.equals("material")) materials.add(section.getString(key));
        }
    }

    @Test
    void togglesHaveTheirOffLook() {
        for (String path : new String[]{"main.items.games-toggle", "main.items.sound-toggle", "challenges.items.tab-daily",
                "challenges.items.tab-weekly", "admin.items.toggle", "admin.items.toggle-all"}) {
            assertNotNull(file.getConfigurationSection(path + ".when-off"), path);
        }
    }

    @Test
    void wrongCaseSwapsLettersOnly() {
        assertEquals("cREEPER 12", DebugMenu.swapCase("Creeper 12"));
        assertEquals("42", DebugMenu.swapCase("42"));
    }
}
