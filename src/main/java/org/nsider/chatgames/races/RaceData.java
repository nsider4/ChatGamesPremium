package org.nsider.chatgames.races;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * One race's {@code data} from races.yml: the targets (materials or mobs) it can ask
 * for, the amounts for each, and the reward names for each amount.
 *
 * <pre>
 * data:
 *   ZOMBIE:
 *     3:
 *       rewards: [reward_2]
 * </pre>
 *
 * Target names are kept upper-case as written; whether they exist on this server
 * version is checked when a race starts. Never changed after loading.
 */
public final class RaceData {

    public static final RaceData EMPTY = new RaceData(Collections.<String, Map<Integer, List<String>>>emptyMap());

    private final Map<String, Map<Integer, List<String>>> targets;

    private RaceData(Map<String, Map<Integer, List<String>>> targets) {
        this.targets = targets;
    }

    /**
     * @param data the race's {@code data} section, or null if it has none
     * @param warn told about amounts that aren't whole numbers above 0
     */
    @Nonnull
    public static RaceData load(@Nullable ConfigurationSection data, @Nonnull Consumer<String> warn) {
        if (data == null) return EMPTY;
        Map<String, Map<Integer, List<String>>> targets = new LinkedHashMap<>();
        for (String target : data.getKeys(false)) {
            ConfigurationSection amounts = data.getConfigurationSection(target);
            if (amounts == null) continue;
            Map<Integer, List<String>> rewards = new LinkedHashMap<>();
            for (String amount : amounts.getKeys(false)) {
                int parsed;
                try {
                    parsed = Integer.parseInt(amount.trim());
                } catch (NumberFormatException e) {
                    parsed = 0;
                }
                if (parsed <= 0) {
                    warn.accept(data.getCurrentPath() + "." + target + "." + amount + " is not an amount above 0");
                    continue;
                }
                rewards.put(parsed, Collections.unmodifiableList(
                        new ArrayList<>(amounts.getStringList(amount + ".rewards"))));
            }
            if (!rewards.isEmpty()) {
                targets.put(target.toUpperCase(Locale.ROOT), Collections.unmodifiableMap(rewards));
            }
        }
        return new RaceData(Collections.unmodifiableMap(targets));
    }

    public boolean isEmpty() {
        return targets.isEmpty();
    }

    /** @return the target names, upper-case, in file order */
    @Nonnull
    public Set<String> getTargets() {
        return targets.keySet();
    }

    /** @return the amounts configured for a target, in file order; empty if it has none */
    @Nonnull
    public List<Integer> getAmounts(@Nonnull String target) {
        Map<Integer, List<String>> amounts = targets.get(target.toUpperCase(Locale.ROOT));
        return amounts == null ? Collections.<Integer>emptyList() : new ArrayList<>(amounts.keySet());
    }

    /**
     * @return the reward names for exactly this target and amount; empty for values
     *         started by command that races.yml doesn't list
     */
    @Nonnull
    public List<String> getRewards(@Nonnull String target, int amount) {
        Map<Integer, List<String>> amounts = targets.get(target.toUpperCase(Locale.ROOT));
        List<String> rewards = amounts == null ? null : amounts.get(amount);
        return rewards == null ? Collections.<String>emptyList() : rewards;
    }

    /**
     * Picks a target and one of its amounts at random, among the targets that exist on this server.
     *
     * @param exists whether a target name exists on this server version
     * @return null if none does
     */
    @Nullable
    public Pick pick(@Nonnull Random random, @Nonnull Predicate<String> exists) {
        List<String> candidates = new ArrayList<>();
        for (String target : targets.keySet()) {
            if (exists.test(target)) candidates.add(target);
        }
        if (candidates.isEmpty()) return null;
        String target = candidates.get(random.nextInt(candidates.size()));
        List<Integer> amounts = getAmounts(target);
        return new Pick(target, amounts.get(random.nextInt(amounts.size())));
    }

    /** A target and how many of it to collect. */
    public static final class Pick {
        private final String target;
        private final int amount;

        public Pick(@Nonnull String target, int amount) {
            this.target = target;
            this.amount = amount;
        }

        @Nonnull
        public String getTarget() {
            return target;
        }

        public int getAmount() {
            return amount;
        }
    }
}
