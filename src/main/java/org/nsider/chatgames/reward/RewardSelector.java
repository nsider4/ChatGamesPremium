package org.nsider.chatgames.reward;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Picks the reward commands a winner gets from a rewards.yml section, the 1.9.9 way:
 * <ol>
 *   <li>rewards needing more online players ({@code minimum-players}) are skipped</li>
 *   <li>rewards without a {@code chance} are always given; of those with one, a single
 *       reward is drawn (chances are percentages; below 100 in total, sometimes none)</li>
 *   <li>in the chosen rewards, lines ending in {@code has:<permission>} need that permission,
 *       lines without {@code <chance>%~ } always run, and one of the lines with it is drawn</li>
 * </ol>
 * Holds no state, so any thread may use it.
 */
public final class RewardSelector {

    private static final String CHANCE_SEPARATOR = "%~ ";
    private static final String PERMISSION_PREFIX = "has:";

    private RewardSelector() {}

    /**
     * @param rewards       the section holding the rewards by name
     * @param only          the reward names to consider (races list theirs), or null for every reward
     * @param onlinePlayers for {@code minimum-players}
     * @param hasPermission for {@code has:} lines
     * @param invalid       told about lines whose chance isn't a number
     * @return the commands to run, in order
     */
    @Nonnull
    public static List<String> select(@Nonnull ConfigurationSection rewards, @Nullable List<String> only, int onlinePlayers,
                                      @Nonnull Predicate<String> hasPermission, @Nonnull Random random,
                                      @Nonnull Consumer<String> invalid) {
        List<ConfigurationSection> chosen = new ArrayList<>();
        Draw<ConfigurationSection> rewardDraw = new Draw<>();
        for (String name : only != null ? only : new ArrayList<>(rewards.getKeys(false))) {
            ConfigurationSection reward = rewards.getConfigurationSection(name);
            if (reward == null || onlinePlayers < reward.getInt("minimum-players", 0)) continue;
            if (reward.isSet("chance")) {
                rewardDraw.add(reward.getDouble("chance"), reward);
            } else {
                chosen.add(reward);
            }
        }
        ConfigurationSection drawn = rewardDraw.draw(random);
        if (drawn != null) chosen.add(drawn);

        List<String> commands = new ArrayList<>();
        Draw<String> commandDraw = new Draw<>();
        for (ConfigurationSection reward : chosen) {
            for (String line : reward.getStringList("data")) {
                String command = withoutPermission(line, hasPermission);
                if (command == null || command.isEmpty()) continue;
                int separator = command.indexOf(CHANCE_SEPARATOR);
                if (separator < 0) {
                    commands.add(command);
                    continue;
                }
                try {
                    commandDraw.add(Double.parseDouble(command.substring(0, separator).trim()),
                            command.substring(separator + CHANCE_SEPARATOR.length()).trim());
                } catch (NumberFormatException e) {
                    invalid.accept(line);
                }
            }
        }
        String extra = commandDraw.draw(random);
        if (extra != null) commands.add(extra);
        return commands;
    }

    /**
     * @return the line without its {@code has:} part, or null if the winner lacks that permission
     */
    @Nullable
    static String withoutPermission(@Nonnull String line, @Nonnull Predicate<String> hasPermission) {
        int start = line.indexOf(PERMISSION_PREFIX);
        if (start < 0) return line.trim();
        int end = line.indexOf(' ', start);
        String permission = line.substring(start + PERMISSION_PREFIX.length(), end < 0 ? line.length() : end);
        if (!hasPermission.test(permission)) return null;
        String before = line.substring(0, start).trim();
        String after = end < 0 ? "" : line.substring(end).trim();
        return after.isEmpty() ? before : before + " " + after;
    }

    /** A weighted draw over percentages; past 100 in total, the weights are scaled to fit. */
    private static final class Draw<T> {
        private final NavigableMap<Double, T> entries = new TreeMap<>();
        private double total;

        void add(double chance, T value) {
            if (chance > 0) {
                total += chance;
                entries.put(total, value);
            }
        }

        @Nullable
        T draw(Random random) {
            if (entries.isEmpty()) return null;
            Map.Entry<Double, T> entry = entries.higherEntry(random.nextDouble() * Math.max(100, total));
            return entry == null ? null : entry.getValue();
        }
    }
}
