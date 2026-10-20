package org.nsider.chatgames.games.round;

import lombok.Getter;
import org.nsider.chatgames.enums.GameType;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One generated question: what players see and which answers win.
 *
 * <p>A round is pure data. Generators build it without touching players or the
 * scheduler, so every game can be unit-tested and previewed (the debug menu
 * builds rounds without starting them).</p>
 */
@Getter
public final class Round {

    private final GameType type;
    /** Every accepted answer; the first is the one shown when the round ends. */
    private final List<String> answers;
    /** Replaces %selection% in the announcement and title. */
    private final String display;
    /** Extra placeholders for this game's messages, e.g. %line_1% for Variable. */
    private final Map<String, String> placeholders;
    /** Clickable/hoverable chat text, or null. */
    @Nullable private final ComponentSpec component;
    /** Seconds the list stays on screen before chat is cleared; 0 for games without a memorise phase. */
    private final int memorizeSeconds;
    /** The answer is a comma separated list (Memorize). */
    private final boolean listAnswer;
    /** For list answers: the items must be typed in the shown order. */
    private final boolean orderMatters;
    /** Progressive hints, revealed one by one while the round runs. Empty if the game has none. */
    private final List<String> hints;

    private Round(Builder builder) {
        this.type = builder.type;
        this.answers = Collections.unmodifiableList(new ArrayList<>(builder.answers));
        this.display = builder.display;
        this.placeholders = Collections.unmodifiableMap(new LinkedHashMap<>(builder.placeholders));
        this.component = builder.component;
        this.memorizeSeconds = builder.memorizeSeconds;
        this.listAnswer = builder.listAnswer;
        this.orderMatters = builder.orderMatters;
        this.hints = Collections.unmodifiableList(new ArrayList<>(builder.hints));
    }

    @Nonnull
    public String getMainAnswer() {
        return answers.get(0);
    }

    public static Builder builder(@Nonnull GameType type, @Nonnull String answer) {
        return new Builder(type).answer(answer);
    }

    public static final class Builder {
        private final GameType type;
        private final List<String> answers = new ArrayList<>();
        private String display = "";
        private final Map<String, String> placeholders = new LinkedHashMap<>();
        private ComponentSpec component;
        private int memorizeSeconds;
        private boolean listAnswer;
        private boolean orderMatters = true;
        private final List<String> hints = new ArrayList<>();

        private Builder(GameType type) {
            this.type = type;
        }

        public Builder answer(String answer) {
            if (answer != null && !answer.trim().isEmpty() && !answers.contains(answer)) {
                answers.add(answer);
            }
            return this;
        }

        public Builder answers(Iterable<String> more) {
            for (String answer : more) answer(answer);
            return this;
        }

        public Builder display(String display) {
            this.display = display == null ? "" : display;
            return this;
        }

        public Builder placeholder(String key, Object value) {
            placeholders.put(key, String.valueOf(value));
            return this;
        }

        public Builder component(ComponentSpec component) {
            this.component = component;
            return this;
        }

        public Builder memorize(int seconds) {
            this.memorizeSeconds = Math.max(0, seconds);
            return this;
        }

        public Builder listAnswer(boolean orderMatters) {
            this.listAnswer = true;
            this.orderMatters = orderMatters;
            return this;
        }

        public Builder hints(List<String> hints) {
            this.hints.addAll(hints);
            return this;
        }

        public Round build() {
            if (answers.isEmpty()) {
                throw new IllegalStateException(type + " round has no answer; check its word list or settings");
            }
            return new Round(this);
        }
    }

    /**
     * Interactive chat text. The announcer turns it into a component per player,
     * so PlaceholderAPI and colours are applied for each viewer.
     */
    @Getter
    public static final class ComponentSpec {
        /** The placeholder in the announcement lines that the component replaces, e.g. %clickableText%. */
        private final String placeholder;
        private final String text;
        private final String hoverText;
        /** Command run on click, or null if the text is not clickable. */
        @Nullable private final String clickCommand;

        public ComponentSpec(String placeholder, String text, String hoverText, @Nullable String clickCommand) {
            this.placeholder = placeholder;
            this.text = text;
            this.hoverText = hoverText;
            this.clickCommand = clickCommand;
        }
    }
}
