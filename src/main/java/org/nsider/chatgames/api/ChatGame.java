package org.nsider.chatgames.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A custom chat game, for {@link ChatGamesAPI#registerGame}.
 *
 * <pre>{@code
 * ChatGame emoji = ChatGame.builder("emoji_quiz")
 *         .displayName("Emoji Quiz")
 *         .icon("painting")
 *         .setting("timeToGuess_seconds", 25)
 *         .generator(context -> Question.builder("pizza").display("🍕 ?").revealHints().build())
 *         .build();
 * ChatGamesAPI.get().registerGame(plugin, emoji);
 * }</pre>
 *
 * <p>The game then plays like the built-in ones: in the rotation (with its weight), in presets,
 * events and challenges, with {@code /chatgames <id>}, leaderboards and placeholders. The first
 * time it is registered, its sections are added to games.yml, messages.yml and rewards.yml from
 * the defaults given here; after that the server owner's values are used.</p>
 */
public final class ChatGame {

    private final String id;
    private final String displayName;
    private final String icon;
    private final Map<String, Object> settings;
    private final List<String> startMessage;
    private final List<String> winMessage;
    private final List<String> expiredMessage;
    private final List<String> rewards;
    private final QuestionGenerator generator;

    private ChatGame(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.icon = builder.icon;
        this.settings = Collections.unmodifiableMap(new LinkedHashMap<>(builder.settings));
        this.startMessage = builder.startMessage;
        this.winMessage = builder.winMessage;
        this.expiredMessage = builder.expiredMessage;
        this.rewards = builder.rewards;
        this.generator = builder.generator;
    }

    /**
     * @param id the game's id: 2-32 lower-case letters, digits or underscores, starting with a
     *           letter (see {@link GameType#isValidId})
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    /** @return default games.yml values that differ from the standard ones */
    public Map<String, Object> getSettings() {
        return settings;
    }

    /** @return the default start announcement, or null for the standard one */
    public List<String> getStartMessage() {
        return startMessage;
    }

    public List<String> getWinMessage() {
        return winMessage;
    }

    public List<String> getExpiredMessage() {
        return expiredMessage;
    }

    /** @return the default reward lines for the first place, or null for the standard one */
    public List<String> getRewards() {
        return rewards;
    }

    public QuestionGenerator getGenerator() {
        return generator;
    }

    public static final class Builder {
        private final String id;
        private String displayName;
        private String icon = "paper";
        private final Map<String, Object> settings = new LinkedHashMap<>();
        private List<String> startMessage;
        private List<String> winMessage;
        private List<String> expiredMessage;
        private List<String> rewards;
        private QuestionGenerator generator;

        private Builder(String id) {
            this.id = id;
        }

        /** The name shown in menus and in {@code %game%}. Defaults to the id in title case. */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /** The menu icon: any material name from 1.8 to 26.x. Defaults to paper. */
        public Builder icon(String material) {
            this.icon = material;
            return this;
        }

        /**
         * A default games.yml value, e.g. {@code setting("timeToGuess_seconds", 30)} or
         * {@code setting("hints.enable", false)}. Values the game reads itself go here too.
         */
        public Builder setting(String path, Object value) {
            settings.put(path, value);
            return this;
        }

        /**
         * The default messages.yml lines. Each may use {@code %selection%}, {@code %correct_answer%},
         * {@code %timeToGuess%}, {@code %winner_1%}, {@code %time_1%}, {@code <center>}, colours
         * and the question's own placeholders. A null list keeps the standard lines.
         */
        public Builder messages(List<String> start, List<String> win, List<String> expired) {
            this.startMessage = copy(start);
            this.winMessage = copy(win);
            this.expiredMessage = copy(expired);
            return this;
        }

        /** The default rewards.yml lines for the first place, e.g. {@code [consolecmd] give %player% diamond 1}. */
        public Builder rewards(String... lines) {
            this.rewards = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(lines)));
            return this;
        }

        public Builder generator(QuestionGenerator generator) {
            this.generator = generator;
            return this;
        }

        /** @throws IllegalStateException if the id is malformed or there is no generator */
        public ChatGame build() {
            if (!GameType.isValidId(id)) {
                throw new IllegalStateException("Game id '" + id + "' must be 2-32 lower-case letters, digits or _ and start with a letter");
            }
            if (generator == null) throw new IllegalStateException("Chat game '" + id + "' needs a generator");
            if (displayName == null || displayName.trim().isEmpty()) displayName = titleCase(id);
            return new ChatGame(this);
        }

        private static List<String> copy(List<String> lines) {
            return lines == null ? null : Collections.unmodifiableList(new ArrayList<>(lines));
        }
    }

    static String titleCase(String id) {
        StringBuilder result = new StringBuilder();
        for (String part : id.split("_")) {
            if (part.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
}
