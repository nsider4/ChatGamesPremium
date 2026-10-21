package org.nsider.chatgames.gameevents;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * When an event starts by itself: {@code SATURDAY 20:00} once a week, or
 * {@code DAILY 18:30} (or just {@code 18:30}) every day. Unchanging.
 */
public final class EventSchedule {

    /** Null for every day. */
    @Nullable private final DayOfWeek day;
    private final LocalTime time;

    EventSchedule(@Nullable DayOfWeek day, @Nonnull LocalTime time) {
        this.day = day;
        this.time = time;
    }

    /**
     * @return the schedule, or null if the text isn't {@code [DAY|DAILY] HH:mm}
     */
    @Nullable
    public static EventSchedule parse(@Nullable String text) {
        if (text == null) return null;
        String[] parts = text.trim().toUpperCase(Locale.ROOT).split("\\s+");
        if (parts.length == 0 || parts.length > 2) return null;
        DayOfWeek day = null;
        if (parts.length == 2 && !parts[0].equals("DAILY")) {
            try {
                day = DayOfWeek.valueOf(parts[0]);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        try {
            return new EventSchedule(day, LocalTime.parse(parts[parts.length - 1]));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * @return true if a start time falls after {@code from} and no later than {@code to}
     */
    public boolean isDue(@Nonnull ZonedDateTime from, @Nonnull ZonedDateTime to) {
        if (!to.isAfter(from)) return false;
        for (LocalDate date = from.toLocalDate(); !date.isAfter(to.toLocalDate()); date = date.plusDays(1)) {
            if (day != null && date.getDayOfWeek() != day) continue;
            ZonedDateTime start = date.atTime(time).atZone(to.getZone());
            if (start.isAfter(from) && !start.isAfter(to)) return true;
        }
        return false;
    }

    /** @return the first start time after {@code now} */
    @Nonnull
    public ZonedDateTime next(@Nonnull ZonedDateTime now) {
        LocalDate date = now.toLocalDate();
        for (int i = 0; i <= 7; i++, date = date.plusDays(1)) {
            if (day != null && date.getDayOfWeek() != day) continue;
            ZonedDateTime start = date.atTime(time).atZone(now.getZone());
            if (start.isAfter(now)) return start;
        }
        // Unreachable: a weekly time comes round within 8 days
        return now.plusWeeks(1);
    }

    @Override
    public String toString() {
        return (day == null ? "DAILY" : day.name()) + " " + time;
    }
}
