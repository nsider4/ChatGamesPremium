package org.nsider.chatgames.games.round;

import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.games.round.generators.MathRoundGenerator;
import org.nsider.chatgames.games.round.generators.MemorizeRoundGenerator;
import org.nsider.chatgames.games.round.generators.SequenceRoundGenerator;
import org.nsider.chatgames.games.round.generators.SimpleRoundGenerators;
import org.nsider.chatgames.games.round.generators.VariableRoundGenerator;
import org.nsider.chatgames.games.round.generators.WordAssociationRoundGenerator;
import org.nsider.chatgames.games.round.generators.WordRoundGenerator;

import org.nsider.inventorymanager.extras.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * The generator for each chat game. Races have none: they aren't answered in chat.
 */
public final class RoundGenerators {

    private static final Map<GameType, RoundGenerator> GENERATORS = new ConcurrentHashMap<>();

    static {
        for (GameType type : new GameType[]{GameType.UNSCRAMBLE, GameType.UNREVERSE, GameType.REACTION,
                GameType.FILLOUT, GameType.HOVERABLE}) {
            GENERATORS.put(type, new WordRoundGenerator(type));
        }
        GENERATORS.put(GameType.RANDOM, SimpleRoundGenerators.RANDOM);
        GENERATORS.put(GameType.CLICKABLE, SimpleRoundGenerators.CLICKABLE);
        GENERATORS.put(GameType.GUESS_THE_NUMBER, SimpleRoundGenerators.GUESS_THE_NUMBER);
        GENERATORS.put(GameType.TRIVIA, SimpleRoundGenerators.TRIVIA);
        GENERATORS.put(GameType.MATH, new MathRoundGenerator());
        GENERATORS.put(GameType.SEQUENCE, new SequenceRoundGenerator());
        GENERATORS.put(GameType.VARIABLE, new VariableRoundGenerator());
        GENERATORS.put(GameType.MEMORIZE, new MemorizeRoundGenerator());
        GENERATORS.put(GameType.WORD_ASSOCIATION, new WordAssociationRoundGenerator());
    }

    private RoundGenerators() {}

    @Nullable
    public static RoundGenerator get(GameType type) {
        return GENERATORS.get(type);
    }
}
