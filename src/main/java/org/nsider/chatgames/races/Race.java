package org.nsider.chatgames.races;

import lombok.Getter;
import org.nsider.chatgames.enums.GameType;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.List;

/**
 * A running race: what to collect, how many, and everyone's progress. Its fields
 * never change; {@link RaceProgress} is the only part that moves, and it is thread-safe.
 */
@Getter
public final class Race {

    private final GameType type;
    private final RaceTarget target;
    /** The target's name in the configured language, as shown in messages. */
    private final String displayName;
    private final int amount;
    /** Reward names from races.yml (looked up in rewards.yml under races.rewards). */
    private final List<String> rewards;
    private final int timeLimitSeconds;
    private final RaceProgress progress;
    private final long startNanos = System.nanoTime();

    public Race(@Nonnull GameType type, @Nonnull RaceTarget target, @Nonnull String displayName, int amount,
                @Nonnull List<String> rewards, int timeLimitSeconds) {
        this.type = type;
        this.target = target;
        this.displayName = displayName;
        this.amount = amount;
        this.rewards = rewards;
        this.timeLimitSeconds = timeLimitSeconds;
        this.progress = new RaceProgress(amount);
    }

    public double getElapsedSeconds() {
        return (System.nanoTime() - startNanos) / 1_000_000_000.0;
    }
}
