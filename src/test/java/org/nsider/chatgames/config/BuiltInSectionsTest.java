package org.nsider.chatgames.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInSectionsTest {

    private static final Function<String, InputStream> RESOURCES = name -> BuiltInSectionsTest.class.getClassLoader().getResourceAsStream(name);

    @TempDir
    File data;

    @Test
    void everyListedSectionIsInTheShippedFiles() throws IOException {
        for (String entry : BuiltInSections.SECTIONS) {
            String[] parts = entry.split(":", 2);
            assertNotNull(YamlText.block(lines(shipped(parts[0])), parts[1]), entry);
        }
    }

    /** A 2.0.0 file (the shipped one without the new sections) ends up reading like the shipped one. */
    @Test
    void sectionsAreCopiedWhereTheyBelong() throws IOException {
        for (String file : Arrays.asList("races.yml", "messages.yml", "global_games.yml", "menus.yml")) {
            String shipped = shipped(file);
            String old = shipped;
            List<String> paths = paths(file);
            for (String path : paths) old = remove(old, path);
            assertFalse(YamlText.values(YamlText.parse(old, file)).equals(YamlText.values(YamlText.parse(shipped, file))), file);

            String result = old;
            for (String path : paths) {
                result = BuiltInSections.add(result, shipped, path);
                assertNotNull(result, file + " " + path);
            }
            assertEquals(YamlText.values(YamlText.parse(shipped, file)), YamlText.values(YamlText.parse(result, file)), file);
        }
    }

    @Test
    void ownersSectionsAndCommentsStay() throws IOException {
        String owner = "# my notes\r\ngames:\r\n  mine:\r\n    goal: 999\r\n\r\nmessages:\r\n  x: 'y'\r\n";
        String result = BuiltInSections.add(owner, shipped("global_games.yml"), "games.tame");
        assertNotNull(result);
        assertTrue(result.startsWith("# my notes\r\ngames:\r\n  mine:\r\n    goal: 999\r\n  tame:\r\n"), result);
        assertFalse(result.replace("\r\n", "").contains("\n"), "keeps the owner's line endings");
        assertEquals(15, YamlText.parse(result, "x").getInt("games.tame.goal"));
        assertEquals(999, YamlText.parse(result, "x").getInt("games.mine.goal"));
        assertEquals("y", YamlText.parse(result, "x").getString("messages.x"));

        assertNull(BuiltInSections.add(result, shipped("global_games.yml"), "games.tame"), "already there");
        assertNull(BuiltInSections.add("messages: {}\n", shipped("global_games.yml"), "games.tame"), "no parent");
    }

    @Test
    void aDeletedSectionIsNotAddedAgain() throws IOException {
        File races = new File(data, "races.yml");
        String old = remove(remove(shipped("races.yml"), "tame"), "shear");
        Files.write(races.toPath(), old.getBytes(StandardCharsets.UTF_8));

        List<String> added = BuiltInSections.addMissing(data, RESOURCES);
        assertTrue(added.containsAll(Arrays.asList("races.yml:tame", "races.yml:shear")), added.toString());
        assertTrue(YamlText.load(races).isConfigurationSection("tame"));

        // The owner deletes Shear; files that don't exist yet are only marked
        Files.write(races.toPath(), remove(read(races), "shear").getBytes(StandardCharsets.UTF_8));
        assertEquals(Collections.emptyList(), BuiltInSections.addMissing(data, RESOURCES));
        assertFalse(YamlText.load(races).isConfigurationSection("shear"));
    }

    private static List<String> paths(String file) {
        List<String> paths = new ArrayList<>();
        for (String entry : BuiltInSections.SECTIONS) {
            if (entry.startsWith(file + ":")) paths.add(entry.substring(file.length() + 1));
        }
        return paths;
    }

    private static String remove(String text, String path) {
        List<String> lines = lines(text);
        int[] block = YamlText.block(lines, path);
        assertNotNull(block, path);
        lines.subList(block[0], block[1]).clear();
        return String.join("\n", lines);
    }

    private static List<String> lines(String text) {
        return new ArrayList<>(Arrays.asList(text.replace("\r\n", "\n").split("\n", -1)));
    }

    private static String shipped(String name) throws IOException {
        return new String(YamlText.read(RESOURCES, name), StandardCharsets.UTF_8);
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
