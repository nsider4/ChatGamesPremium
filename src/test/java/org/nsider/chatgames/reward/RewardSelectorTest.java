package org.nsider.chatgames.reward;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewardSelectorTest {

    private static ConfigurationSection rewards(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return config;
    }

    /** One generator for every draw: neighbouring seeds give java.util.Random nearly the same first number. */
    private static final Random RANDOM = new Random(42);

    private static List<String> select(ConfigurationSection rewards, List<String> only, int online, long round) {
        List<String> invalid = new ArrayList<>();
        List<String> commands = RewardSelector.select(rewards, only, online, permission -> permission.equals("vip"),
                RANDOM, invalid::add);
        assertTrue(invalid.isEmpty(), "unexpected invalid lines: " + invalid);
        return commands;
    }

    @Test
    void rewardsWithoutChanceAreAlwaysGiven() throws Exception {
        ConfigurationSection section = rewards(
                "a:\n  data:\n    - '[consolecmd] give %player% iron_block 5'\n"
                        + "b:\n  data:\n    - '[playermsg] hi'\n    - '[playermsg] there'\n");
        assertEquals(Arrays.asList("[consolecmd] give %player% iron_block 5", "[playermsg] hi", "[playermsg] there"),
                select(section, null, 1, 1));
    }

    @Test
    void onlyOneRewardWithAChanceIsDrawn() throws Exception {
        // The first recode draft ignored chance and gave every reward
        ConfigurationSection section = rewards(
                "a:\n  chance: 50\n  data: ['A']\nb:\n  chance: 50\n  data: ['B']\n");
        Set<String> seen = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            List<String> commands = select(section, null, 1, seed);
            assertEquals(1, commands.size(), "chances adding up to 100 always give exactly one");
            seen.addAll(commands);
        }
        assertEquals(new HashSet<>(Arrays.asList("A", "B")), seen);
    }

    @Test
    void chancesBelowOneHundredSometimesGiveNothing() throws Exception {
        ConfigurationSection section = rewards("a:\n  chance: 10\n  data: ['A']\n");
        int given = 0;
        for (long seed = 0; seed < 1000; seed++) given += select(section, null, 1, seed).size();
        assertTrue(given > 50 && given < 150, "about 10% of 1000, was " + given);
    }

    @Test
    void minimumPlayersSkipsTheReward() throws Exception {
        ConfigurationSection section = rewards("a:\n  minimum-players: 3\n  data: ['A']\nb:\n  data: ['B']\n");
        assertEquals(Collections.singletonList("B"), select(section, null, 2, 1));
        assertEquals(Arrays.asList("A", "B"), select(section, null, 3, 1));
    }

    @Test
    void commandChancesDrawOneLine() throws Exception {
        ConfigurationSection section = rewards(
                "a:\n  data:\n    - 'always'\n    - '50.0%~ heads'\n    - '50.0%~ tails'\n");
        for (long seed = 0; seed < 50; seed++) {
            List<String> commands = select(section, null, 1, seed);
            assertEquals(2, commands.size());
            assertEquals("always", commands.get(0));
            assertTrue(commands.get(1).equals("heads") || commands.get(1).equals("tails"));
        }
    }

    @Test
    void permissionLinesNeedThePermission() {
        assertEquals("[consolecmd] give %player% diamond 1", RewardSelector.withoutPermission(
                "[consolecmd] give %player% diamond 1 has:vip", p -> p.equals("vip")));
        assertNull(RewardSelector.withoutPermission("[consolecmd] give %player% diamond 1 has:vip", p -> false));
        assertEquals("[playermsg] hi there", RewardSelector.withoutPermission("[playermsg] hi has:vip there", p -> true));
        assertEquals("plain", RewardSelector.withoutPermission(" plain ", p -> false));
    }

    @Test
    void racesOnlyGetTheRewardsTheyList() throws Exception {
        ConfigurationSection section = rewards("reward_1:\n  data: ['one']\nreward_2:\n  data: ['two']\n");
        assertEquals(Collections.singletonList("two"), select(section, Collections.singletonList("reward_2"), 1, 1));
        assertTrue(select(section, Collections.singletonList("missing"), 1, 1).isEmpty());
    }

    @Test
    void invalidChancesAreReported() throws Exception {
        ConfigurationSection section = rewards("a:\n  data: ['abc%~ broken']\n");
        List<String> invalid = new ArrayList<>();
        assertTrue(RewardSelector.select(section, null, 1, p -> true, new Random(1), invalid::add).isEmpty());
        assertEquals(Collections.singletonList("abc%~ broken"), invalid);
    }

    @Test
    void theDefaultRewardsFileParses() throws Exception {
        YamlConfiguration file = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/rewards.yml"), StandardCharsets.UTF_8)) {
            file.load(reader);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        ConfigurationSection unscramble = file.getConfigurationSection("unscramble.rewards.1");
        assertNotNull(unscramble);
        ConfigurationSection races = file.getConfigurationSection("races.rewards");
        assertNotNull(races);
        for (long seed = 0; seed < 20; seed++) {
            List<String> commands = select(races, Collections.singletonList("reward_2"), 1, seed);
            assertTrue(commands.contains("[playermsg] &aYou won!"));
            assertFalse(commands.isEmpty());
        }
    }
}
