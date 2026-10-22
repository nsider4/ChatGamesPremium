package org.nsider.chatgames.progression;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.nsider.chatgames.api.GameType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomChallengeTest {

    private static Challenge custom(String type) {
        return new Challenge("votes", Period.DAILY, ChallengeType.CUSTOM, type, 5, null, 0,
                "Vote", "", "PAPER", Collections.emptyList());
    }

    @Test
    void onlyTheSameCustomTypeCounts() {
        Challenge votes = custom("vote");
        assertEquals(3, votes.progressFor(Activity.custom(" VOTE ", 3)));
        assertEquals(0, votes.progressFor(Activity.custom("parkour", 3)));
        assertEquals(0, votes.progressFor(Activity.win(GameType.MATH, 1, 1)));
        assertEquals(0, votes.progressFor(Activity.event()));
    }

    @Test
    void unknownTypesAreSkippedWhileNoRegistryRuns() throws Exception {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString("daily:\n  challenges:\n    votes:\n      type: vote\n      amount: 5\n");
        List<String> warnings = new ArrayList<>();
        ChallengeConfig config = ChallengeConfig.load(file, warnings::add);
        assertTrue(config.active(Period.DAILY, LocalDate.of(2026, 10, 21)).isEmpty());
        assertEquals(1, warnings.size(), "warned: " + warnings);
    }
}
