package org.nsider.chatgames.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Upgrading a 1.9.9 data folder (the real 1.9.9 files, shipped under legacy/1.9.9). */
class LegacyMigrationTest {

    private static final String[] OLD_FILES = {"config.yml", "chatRaces.yml", "messages.yml", "words.yml", "rewards.yml"};
    private static final Function<String, InputStream> RESOURCES = name -> LegacyMigrationTest.class.getClassLoader().getResourceAsStream(name);

    @TempDir
    File folder;

    @BeforeEach
    void install199() throws IOException {
        for (String name : OLD_FILES) {
            try (InputStream in = RESOURCES.apply("legacy/1.9.9/" + name)) {
                Files.copy(in, new File(folder, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    @Test
    void anUnchangedFolderGetsThe200FilesExactlyAsShipped() throws IOException {
        assertTrue(LegacyMigration.isNeeded(folder));
        LegacyMigration.Result result = LegacyMigration.migrate(folder, RESOURCES);

        for (String target : LegacyMigration.TARGETS) {
            assertArrayEquals(resource(target), Files.readAllBytes(new File(folder, target).toPath()), target + " keeps its comments");
        }
        assertFalse(new File(folder, "chatRaces.yml").exists(), "replaced by races.yml");
        assertFalse(result.rewardsKept);
        for (int carried : result.carried.values()) assertEquals(0, carried);
        assertEquals(OLD_FILES.length, result.backup.list().length, "every old file is backed up");
        assertFalse(LegacyMigration.isNeeded(folder), "runs once");
    }

    @Test
    void changedValuesMoveToTheirNewFilesAndPaths() throws IOException {
        change("config.yml", "prefix", "&6MY SERVER");
        change("config.yml", "unscramble.winners", 3);
        change("config.yml", "shoppinglist.listLength", 7);
        change("config.yml", "discord.webhook-url", "https://example.invalid/hook");
        change("config.yml", "log-file", "my_log.txt");
        change("chatRaces.yml", "enable-races", false);
        change("messages.yml", "shoppinglist.correct_message", "&aWell remembered, %player%!");
        change("words.yml", "shoppinglist.items", Arrays.asList("Apple", "Bread"));

        LegacyMigration.Result result = LegacyMigration.migrate(folder, RESOURCES);

        YamlConfiguration config = yaml("config.yml");
        assertEquals("&6MY SERVER", config.getString("prefix"));
        assertFalse(config.contains("unscramble"), "games left config.yml");
        assertFalse(config.contains("discord"));
        assertEquals("chatgames_fastest_times", config.getString("fastest_times_table"), "new keys get their default");

        YamlConfiguration games = yaml("games.yml");
        assertEquals(3, games.getInt("unscramble.winners"));
        assertEquals(7, games.getInt("memorize.listLength"), "shoppinglist is memorize now");
        assertFalse(games.contains("shoppinglist"));
        assertEquals(1, games.getInt("math.winners"), "unchanged values are the 2.0.0 defaults");

        YamlConfiguration addons = yaml("add-ons.yml");
        assertEquals("https://example.invalid/hook", addons.getString("discord.webhook-url"));
        assertEquals("my_log.txt", addons.getString("log-file"));

        assertFalse(yaml("races.yml").getBoolean("enable-races", true));
        assertEquals("&aWell remembered, %player%!", yaml("messages.yml").getString("memorize.correct_message"));
        assertEquals(Arrays.asList("Apple", "Bread"), yaml("words.yml").getStringList("memorize.pools.shopping_list"));

        for (String target : new String[]{"config.yml", "games.yml", "add-ons.yml", "races.yml", "messages.yml", "words.yml"}) {
            assertEquals(comments(resource(target)), comments(Files.readAllBytes(new File(folder, target).toPath())),
                    target + " keeps every comment, even where Bukkit can't save them");
        }
        assertEquals(1, (int) result.carried.get("config.yml"));
        assertEquals(2, (int) result.carried.get("games.yml"));
        assertEquals(2, (int) result.carried.get("add-ons.yml"));
        assertEquals(1, (int) result.carried.get("races.yml"));
    }

    @Test
    void editedRewardsAreKeptWholeAndNewGamesGetDefaults() throws IOException {
        change("rewards.yml", "shoppinglist.rewards.1.reward_1.data", Collections.singletonList("[playermsg] remembered"));
        change("rewards.yml", "unscramble.rewards.2.reward_1.data", Collections.singletonList("[playermsg] second"));

        LegacyMigration.Result result = LegacyMigration.migrate(folder, RESOURCES);

        assertTrue(result.rewardsKept);
        YamlConfiguration rewards = yaml("rewards.yml");
        assertEquals(Collections.singletonList("[playermsg] remembered"), rewards.getStringList("memorize.rewards.1.reward_1.data"));
        assertEquals(Collections.singletonList("[playermsg] second"), rewards.getStringList("unscramble.rewards.2.reward_1.data"));
        assertFalse(rewards.contains("shoppinglist"));
        assertTrue(rewards.contains("sequence"), "games 1.9.9 didn't have get the default rewards");
        assertTrue(rewards.contains("word_association"));
    }

    @Test
    void a200FolderIsLeftAlone() throws IOException {
        for (String name : OLD_FILES) Files.delete(new File(folder, name).toPath());
        Files.write(new File(folder, "config.yml").toPath(), resource("config.yml"));
        assertFalse(LegacyMigration.isNeeded(folder), "no game sections and no chatRaces.yml");
        Files.write(new File(folder, "chatRaces.yml").toPath(), resource("legacy/1.9.9/chatRaces.yml"));
        Files.write(new File(folder, "games.yml").toPath(), resource("games.yml"));
        assertFalse(LegacyMigration.isNeeded(folder), "games.yml means it was already upgraded");
    }

    @Test
    void patchReplacesOneValueAndLeavesEveryOtherLine() {
        String text = lines("# top", "root:", "  # about a", "  a: 1", "  list:", "  - x", "  - y", "", "  # after",
                "  b: 'two'", "other:", "  a: 5");
        assertEquals(lines("# top", "root:", "  # about a", "  a: 3", "  list:", "  - x", "  - y", "", "  # after",
                "  b: 'two'", "other:", "  a: 5"), LegacyMigration.patch(text, "root.a", 3));
        assertEquals(lines("# top", "root:", "  # about a", "  a: 1", "  list:", "  - 'it''s'", "", "  # after",
                "  b: 'two'", "other:", "  a: 5"), LegacyMigration.patch(text, "root.list", Collections.singletonList("it's")));
        assertEquals(lines("# top", "root:", "  # about a", "  a: 1", "  list:", "  - x", "  - y", "", "  # after",
                "  b: 'two'", "other:", "  a: true"), LegacyMigration.patch(text, "other.a", true));
        assertEquals(lines("# top", "root:", "  # about a", "  a: 1", "  list: 'one line'", "", "  # after",
                "  b: 'two'", "other:", "  a: 5"), LegacyMigration.patch(text, "root.list", "one line"));
        assertEquals(null, LegacyMigration.patch(text, "root.missing", 1), "an unknown path falls back to Bukkit's save");
        assertEquals(null, LegacyMigration.patch(text, "root", 1), "a section isn't a value");
    }

    private static String lines(String... lines) {
        return String.join("\n", lines) + "\n";
    }

    private static long comments(byte[] yaml) {
        return Arrays.stream(new String(yaml, StandardCharsets.UTF_8).split("\n")).filter(l -> l.trim().startsWith("#")).count();
    }


    private void change(String file, String path, Object value) throws IOException {
        YamlConfiguration yaml = yaml(file);
        yaml.set(path, value);
        Files.write(new File(folder, file).toPath(), yaml.saveToString().getBytes(StandardCharsets.UTF_8));
    }

    /** Read as UTF-8: Bukkit 1.8's loadConfiguration(File) uses the system charset and mangles symbols. */
    private YamlConfiguration yaml(String file) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(new String(Files.readAllBytes(new File(folder, file).toPath()), StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException e) {
            throw new IOException(e);
        }
        return yaml;
    }

    private static byte[] resource(String name) throws IOException {
        try (InputStream in = RESOURCES.apply(name)) {
            byte[] buffer = new byte[1 << 20];
            int length = 0;
            for (int read; (read = in.read(buffer, length, buffer.length - length)) > 0; ) length += read;
            return Arrays.copyOf(buffer, length);
        }
    }
}
