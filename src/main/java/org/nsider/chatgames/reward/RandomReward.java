package org.nsider.chatgames.reward;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * Two independent chance draws: one over reward commands ({@code 50%~ command})
 * and one over whole rewards. Chances are percentages; if they add up to less
 * than 100, the rest of the time nothing is drawn.
 *
 * <p>Each draw keeps its own running total. 1.9.9 shared one total between them,
 * so resetting one draw corrupted the other.</p>
 */
public class RandomReward {

    private final Draw commands = new Draw();
    private final Draw rewards = new Draw();

    public void add(double chance, String value) {
        commands.add(chance, value);
    }

    public void addReward(double chance, String value) {
        rewards.add(chance, value);
    }

    /** @return a drawn command, or "" if the draw landed on nothing */
    public String getRandomValue() {
        return commands.draw();
    }

    /** @return a drawn reward name, or "" if the draw landed on nothing */
    public String getRandomReward() {
        return rewards.draw();
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }

    public boolean isEmptyReward() {
        return rewards.isEmpty();
    }

    public void resetValues() {
        commands.clear();
    }

    public void resetRewards() {
        rewards.clear();
    }

    private static final class Draw {
        private final NavigableMap<Double, String> entries = new TreeMap<>();
        private final Random random = new Random();
        private double total;

        void add(double chance, String value) {
            if (chance > 0) {
                total += chance;
                entries.put(total, value);
            }
        }

        String draw() {
            if (entries.isEmpty()) return "";
            double roll = random.nextDouble() * Math.max(100, total);
            Map.Entry<Double, String> entry = entries.higherEntry(roll);
            return entry == null ? "" : entry.getValue();
        }

        boolean isEmpty() {
            return entries.isEmpty();
        }

        void clear() {
            entries.clear();
            total = 0;
        }
    }
}
