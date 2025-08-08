package org.nsider.chatgames.model.top;

import lombok.Getter;
import org.nsider.chatgames.enums.GameType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Getter
public class FastestData {
    private final EnumMap<GameType, Double> fastestPerGame;
    private final double bestOverall;

    public FastestData(Map<GameType, Double> perGame, double bestOverall) {
        this.fastestPerGame = new EnumMap<>(perGame);
        this.bestOverall = bestOverall;
    }

    public FastestData withGameTime(GameType game, double time) {
        EnumMap<GameType, Double> updated = new EnumMap<>(fastestPerGame);
        updated.put(game, time);
        double newBest = Math.min(bestOverall, time);
        return new FastestData(updated, newBest);
    }

    public FastestData withOverallFastest(double time) {
        return new FastestData(fastestPerGame, time);
    }

    public double getTime(GameType game) {
        return fastestPerGame.getOrDefault(game, 0.0);
    }
}