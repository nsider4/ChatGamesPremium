package org.nsider.chatgames.config;

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
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * plugin.yml must declare exactly the permissions the code checks, with the same defaults.
 */
class PermissionKeyTest {

    private static ConfigurationSection permissions;

    @BeforeAll
    static void load() throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream("src/main/resources/plugin.yml"), StandardCharsets.UTF_8)) {
            permissions = YamlConfiguration.loadConfiguration(reader).getConfigurationSection("permissions");
        }
        assertNotNull(permissions, "plugin.yml has no permissions section");
    }

    /** Bukkit's YAML reads the dots in a permission name as nesting, so the full name is a path. */
    private static ConfigurationSection declared(String permission) {
        return permissions.getConfigurationSection(permission);
    }

    @Test
    void everyKeyIsDeclaredWithItsDefault() {
        for (PermissionKey key : PermissionKey.values()) {
            if (key == PermissionKey.START_TYPE) continue;
            ConfigurationSection section = declared(key.getPermission());
            assertNotNull(section, key.getPermission() + " missing from plugin.yml");
            assertEquals(key.getDefaultValue(), String.valueOf(section.get("default")), key.getPermission() + " default");
        }
    }

    @Test
    void everyGameHasAStartPermissionUnderStartAll() {
        ConfigurationSection children = declared(PermissionKey.START_ALL.getPermission()).getConfigurationSection("children");
        assertNotNull(children, "chatgames.start.* has no children");
        for (GameType type : GameType.values()) {
            String permission = PermissionKey.startPermission(type);
            assertNotNull(declared(permission), permission + " missing from plugin.yml");
            assertTrue(children.getBoolean(permission), permission + " not granted by chatgames.start.*");
        }
    }

    @Test
    void nothingStaleIsDeclared() {
        Set<String> known = new HashSet<>();
        for (PermissionKey key : PermissionKey.values()) known.add(key.getPermission());
        for (GameType type : GameType.values()) known.add(PermissionKey.startPermission(type));
        for (String path : permissions.getKeys(true)) {
            if (permissions.isConfigurationSection(path) && permissions.contains(path + ".description")) {
                assertTrue(known.contains(path), "plugin.yml declares " + path + ", which the code never checks");
            }
        }
    }
}
