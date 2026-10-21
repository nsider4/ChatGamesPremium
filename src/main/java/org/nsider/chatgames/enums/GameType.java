package org.nsider.chatgames.enums;

import lombok.Getter;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.Locale;

/**
 * The type of game.
 */
@Getter
public enum GameType {
    UNSCRAMBLE("unscramble", false),
    MATH("math", false),
    TRIVIA("trivia", false),
    REACTION("reaction", false),
    CLICKABLE("clickable", false),
    RANDOM("random", false),
    UNREVERSE("unreverse", false),
    VARIABLE("variable", false),
    GUESS_THE_NUMBER("guess_the_number", false),
    MEMORIZE("memorize", false),
    SEQUENCE("sequence", false),
    FILLOUT("fillout", false),
    HOVERABLE("hoverable", false),
    WORD_ASSOCIATION("word_association", false),
    /** Counting together in chat; only played as a global game. */
    COUNT("count", false, true),
    HUNT("hunt", true),
    PLACE("place", true),
    MINE("mine", true),
    FISH("fish", true),
    EAT("eat", true),
    CRAFT("craft", true),
    FURNACE("furnace", true);

    /** Section name used for this game in games.yml / races.yml, messages.yml and rewards.yml. */
    private final String configKey;
    private final boolean race;
    /** Only played as a global game: never in the rotation, never a regular start. */
    private final boolean globalOnly;

    GameType(String configKey, boolean race) {
        this(configKey, race, false);
    }

    GameType(String configKey, boolean race, boolean globalOnly) {
        this.configKey = configKey;
        this.race = race;
        this.globalOnly = globalOnly;
    }

    /**
     * Material races target a material; HUNT is the only race that targets an entity.
     */
    public boolean isMaterialRace() {
        return race && this != HUNT;
    }

    /**
     * @return the races' config keys as a quoted SQL list, e.g. {@code 'hunt','place'}
     *         (constants, so safe to put in a query)
     */
    public static String raceKeysForSql() {
        StringBuilder keys = new StringBuilder();
        for (GameType type : values()) {
            if (!type.race) continue;
            if (keys.length() > 0) keys.append(',');
            keys.append('\'').append(type.configKey).append('\'');
        }
        return keys.toString();
    }

    /**
     * Looks a game up by enum name or config key, ignoring case.
     * Accepts the 1.9.9 name "shoppinglist" for {@link #MEMORIZE}.
     *
     * @return the game, or null if the name matches none
     */
    @Nullable
    public static GameType fromName(@Nullable String name) {
        if (name == null) return null;
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("shoppinglist")) return MEMORIZE;
        for (GameType type : values()) {
            if (type.configKey.equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }
}
