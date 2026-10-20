package org.nsider.chatgames.enums;

import lombok.Getter;

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

    GameType(String configKey, boolean race) {
        this.configKey = configKey;
        this.race = race;
    }

    /**
     * Material races target a material; HUNT is the only race that targets an entity.
     */
    public boolean isMaterialRace() {
        return race && this != HUNT;
    }
}
