package org.nsider.chatgames.progression;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * streaks.yml: each streak's name and its 7-day reward ladder. Unchanging; a reload
 * builds a new one.
 */
public final class StreakConfig {

    public static final int LADDER_DAYS = 7;

    public static final StreakConfig EMPTY = new StreakConfig(false, Collections.<StreakType, Ladder>emptyMap());

    private final boolean loop;
    private final Map<StreakType, Ladder> ladders;

    private StreakConfig(boolean loop, Map<StreakType, Ladder> ladders) {
        this.loop = loop;
        this.ladders = ladders;
    }

    @Nonnull
    public static StreakConfig load(@Nonnull ConfigurationSection file, @Nonnull Consumer<String> warn) {
        String after = file.getString("after-day-7", "repeat").trim().toLowerCase(Locale.ROOT);
        if (!after.equals("repeat") && !after.equals("loop")) {
            warn.accept("after-day-7 '" + after + "' should be repeat or loop, using repeat");
        }
        Map<StreakType, Ladder> ladders = new EnumMap<>(StreakType.class);
        for (StreakType type : StreakType.values()) {
            ConfigurationSection section = file.getConfigurationSection(type.getKey());
            if (section == null || !section.getBoolean("enabled", true)) continue;
            List<List<String>> rewards = new ArrayList<>();
            for (int day = 1; day <= LADDER_DAYS; day++) {
                rewards.add(section.getStringList("rewards." + day));
            }
            ladders.put(type, new Ladder(section.getString("name", type.getKey()), rewards));
        }
        return new StreakConfig(after.equals("loop"), ladders);
    }

    public boolean isEnabled(@Nonnull StreakType type) {
        return ladders.containsKey(type);
    }

    @Nonnull
    public String getName(@Nonnull StreakType type) {
        Ladder ladder = ladders.get(type);
        return ladder == null ? type.getKey() : ladder.name;
    }

    /**
     * @param streakDay the streak's length on the day being rewarded, 1 or more
     * @return the ladder step (1 to 7) paying that day: past day 7 it stays on 7, or loops
     *         back to 1 with {@code after-day-7: loop}
     */
    public int ladderDay(int streakDay) {
        if (streakDay <= LADDER_DAYS) return Math.max(1, streakDay);
        return loop ? (streakDay - 1) % LADDER_DAYS + 1 : LADDER_DAYS;
    }

    /** @return the reward lines for that streak day, in the rewards.yml {@code data} format */
    @Nonnull
    public List<String> getRewards(@Nonnull StreakType type, int streakDay) {
        Ladder ladder = ladders.get(type);
        return ladder == null ? Collections.<String>emptyList() : ladder.rewards.get(ladderDay(streakDay) - 1);
    }

    private static final class Ladder {
        final String name;
        final List<List<String>> rewards;

        Ladder(String name, List<List<String>> rewards) {
            this.name = name;
            this.rewards = rewards;
        }
    }
}
