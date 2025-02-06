package org.nsider.chatgames.reward;

import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class RandomReward {

    private final NavigableMap<Double, String> map = new TreeMap<>();
    private final NavigableMap<Double, String> map1 = new TreeMap<>();
    private final Random random = new Random();
    private double total = 0;

    public void add(double chance, String value) {
        if (chance > 0) {
            total += chance;
            map.put(total, value);
        }
    }

    public void addReward(double chance, String value) {
        if (chance > 0) {
            total += chance;
            map1.put(total, value);
        }
    }

    public String getRandomValue() {
        if(total < 100) { this.add(100-total, ""); }
        final double value = random.nextDouble() * total;
        return map.higherEntry(value).getValue();
    }

    public String getRandomReward() {
        if(total < 100) { this.addReward(100-total, ""); }
        final double value = random.nextDouble() * total;
        return map1.higherEntry(value).getValue();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean isEmptyReward() {
        return map1.isEmpty();
    }

    public void resetValues() {
        map.clear();
        total = 0;
    }

    public void resetRewards() {
        map1.clear();
        total = 0;
    }
}
