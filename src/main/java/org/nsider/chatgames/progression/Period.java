package org.nsider.chatgames.progression;

import org.nsider.inventorymanager.extras.Nonnull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * How long a set of challenges lasts before it resets.
 */
public enum Period {
    DAILY("daily"),
    WEEKLY("weekly");

    private final String key;

    Period(String key) {
        this.key = key;
    }

    /** @return the section name in challenges.yml and the stat prefix, e.g. "daily" */
    @Nonnull
    public String getKey() {
        return key;
    }

    /**
     * @param resetDay the day a week starts on (ignored for daily)
     * @return a number that is the same for every date in one period and different for the next:
     *         the epoch day of the period's first day
     */
    public long id(@Nonnull LocalDate date, @Nonnull DayOfWeek resetDay) {
        return this == DAILY ? date.toEpochDay() : date.with(TemporalAdjusters.previousOrSame(resetDay)).toEpochDay();
    }
}
