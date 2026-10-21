package org.nsider.chatgames.progression;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.enums.GameType;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * challenges.yml: the time zone days are counted in, and the daily and weekly challenges.
 * Unchanging; a reload builds a new one.
 */
public final class ChallengeConfig {

    /** Ids end up in stat names (64 characters at most), so they are kept short and plain. */
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_-]{1,32}");

    public static final ChallengeConfig EMPTY = new ChallengeConfig(ZoneId.systemDefault(),
            Collections.<Period, Board>emptyMap());

    private final ZoneId zone;
    private final Map<Period, Board> boards;

    private ChallengeConfig(ZoneId zone, Map<Period, Board> boards) {
        this.zone = zone;
        this.boards = boards;
    }

    /**
     * @param file challenges.yml
     * @param warn told about anything that was skipped
     */
    @Nonnull
    public static ChallengeConfig load(@Nonnull ConfigurationSection file, @Nonnull Consumer<String> warn) {
        ZoneId zone = ZoneId.systemDefault();
        String zoneName = file.getString("timezone", "").trim();
        if (!zoneName.isEmpty()) {
            try {
                zone = ZoneId.of(zoneName);
            } catch (DateTimeException e) {
                warn.accept("unknown timezone '" + zoneName + "', using the server's (" + zone.getId() + ")");
            }
        }

        Map<Period, Board> boards = new EnumMap<>(Period.class);
        for (Period period : Period.values()) {
            ConfigurationSection section = file.getConfigurationSection(period.getKey());
            if (section != null && section.getBoolean("enabled", true)) {
                boards.put(period, Board.load(period, section, warn));
            }
        }
        return new ChallengeConfig(zone, boards);
    }

    /** @return the time zone that decides when a day starts, for challenges and streaks */
    @Nonnull
    public ZoneId getZone() {
        return zone;
    }

    @Nonnull
    public LocalDate today() {
        return LocalDate.now(zone);
    }

    public boolean isEnabled(@Nonnull Period period) {
        Board board = boards.get(period);
        return board != null && !board.challenges.isEmpty();
    }

    /**
     * @return the period's id on that date (see {@link Period#id})
     */
    public long periodId(@Nonnull Period period, @Nonnull LocalDate date) {
        Board board = boards.get(period);
        return period.id(date, board == null ? DayOfWeek.MONDAY : board.resetDay);
    }

    /**
     * @return the challenges active on that date, in file order; the same for every player
     */
    @Nonnull
    public List<Challenge> active(@Nonnull Period period, @Nonnull LocalDate date) {
        Board board = boards.get(period);
        if (board == null) return Collections.emptyList();
        return board.active(periodId(period, date));
    }

    /** @return the date the period that contains this date ends on (its last day) */
    @Nonnull
    public LocalDate lastDay(@Nonnull Period period, @Nonnull LocalDate date) {
        return LocalDate.ofEpochDay(periodId(period, date)).plusDays(period == Period.DAILY ? 0 : 6);
    }

    /** The challenges of one period. */
    private static final class Board {
        final List<Challenge> challenges;
        final boolean random;
        final int amount;
        final DayOfWeek resetDay;

        Board(List<Challenge> challenges, boolean random, int amount, DayOfWeek resetDay) {
            this.challenges = challenges;
            this.random = random;
            this.amount = amount;
            this.resetDay = resetDay;
        }

        static Board load(Period period, ConfigurationSection section, Consumer<String> warn) {
            String where = period.getKey() + ".";
            DayOfWeek resetDay = DayOfWeek.MONDAY;
            String day = section.getString("reset-day", "MONDAY");
            try {
                resetDay = DayOfWeek.valueOf(day.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                if (period == Period.WEEKLY) warn.accept(where + "reset-day '" + day + "' isn't a day of the week, using MONDAY");
            }

            String mode = section.getString("mode", "fixed").trim().toLowerCase(Locale.ROOT);
            if (!mode.equals("fixed") && !mode.equals("random")) {
                warn.accept(where + "mode '" + mode + "' should be fixed or random, using fixed");
            }

            List<Challenge> challenges = new ArrayList<>();
            ConfigurationSection list = section.getConfigurationSection("challenges");
            if (list != null) {
                for (String id : list.getKeys(false)) {
                    ConfigurationSection entry = list.getConfigurationSection(id);
                    if (entry == null) continue;
                    Challenge challenge = challenge(period, id, entry, where + "challenges." + id, warn);
                    if (challenge != null) challenges.add(challenge);
                }
            }
            return new Board(challenges, mode.equals("random"), Math.max(1, section.getInt("amount", 3)), resetDay);
        }

        @Nullable
        static Challenge challenge(Period period, String id, ConfigurationSection entry, String where, Consumer<String> warn) {
            if (!VALID_ID.matcher(id).matches()) {
                warn.accept(where + ": ids can only use a-z, 0-9, - and _ (up to 32); skipped");
                return null;
            }
            ChallengeType type;
            try {
                type = ChallengeType.valueOf(entry.getString("type", "").trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                warn.accept(where + ": unknown type '" + entry.getString("type") + "'; skipped");
                return null;
            }
            int amount = entry.getInt("amount", 1);
            if (amount < 1) {
                warn.accept(where + ": amount must be 1 or more; skipped");
                return null;
            }
            GameType game = null;
            if (type == ChallengeType.WIN_GAME) {
                game = GameType.fromName(entry.getString("game"));
                if (game == null) {
                    warn.accept(where + ": WIN_GAME needs a game, '" + entry.getString("game") + "' isn't one; skipped");
                    return null;
                }
            }
            double seconds = entry.getDouble("seconds", 0);
            if (type == ChallengeType.FAST_WIN && seconds <= 0) {
                warn.accept(where + ": FAST_WIN needs seconds above 0; skipped");
                return null;
            }
            return new Challenge(id, period, type, amount, game, seconds,
                    entry.getString("name", id), entry.getString("description", ""),
                    entry.getString("icon", "PAPER"), entry.getStringList("rewards"));
        }

        List<Challenge> active(long periodId) {
            if (!random || amount >= challenges.size()) return challenges;
            // Seeded by the period, so every player and every restart draws the same ones
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < challenges.size(); i++) order.add(i);
            Collections.shuffle(order, new Random(mix(periodId)));
            List<Integer> picked = new ArrayList<>(order.subList(0, amount));
            Collections.sort(picked);
            List<Challenge> active = new ArrayList<>();
            for (int index : picked) active.add(challenges.get(index));
            return active;
        }

        /** Spreads neighbouring days apart: java.util.Random draws almost alike from close seeds. */
        private static long mix(long value) {
            long z = value + 0x9E3779B97F4A7C15L;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        }
    }
}
