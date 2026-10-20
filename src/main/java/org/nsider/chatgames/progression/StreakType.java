package org.nsider.chatgames.progression;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.Locale;

/**
 * The three streaks: days in a row on which a player did something.
 */
public enum StreakType {
    /** Completed every daily challenge. */
    CHALLENGE("challenge"),
    /** Won at least one game. */
    DAILY("daily"),
    /** Took part in at least one global game. */
    GLOBAL("global");

    private final String key;

    StreakType(String key) {
        this.key = key;
    }

    /** @return the section in streaks.yml, the stat prefix and the placeholder name, e.g. "daily" */
    @Nonnull
    public String getKey() {
        return key;
    }

    @Nullable
    public static StreakType fromName(@Nullable String name) {
        if (name == null) return null;
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        for (StreakType type : values()) {
            if (type.key.equals(normalized)) return type;
        }
        return null;
    }
}
