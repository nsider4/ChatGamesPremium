package org.nsider.chatgames.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One round of a custom chat game: what players see and which answers win. Built by a
 * {@link QuestionGenerator}.
 *
 * <pre>{@code
 * Question.builder("apple")
 *         .answer("apples")
 *         .display("What fruit keeps the doctor away?")
 *         .revealHints()
 *         .build();
 * }</pre>
 *
 * In the game's messages (messages.yml), {@code %selection%} is replaced by the display text and
 * {@code %correct_answer%} by the first answer. Placeholders added here work there too.
 */
public final class Question {

    private final List<String> answers;
    private final String display;
    private final Map<String, String> placeholders;
    private final List<String> hints;
    private final boolean revealHints;
    private final String componentPlaceholder;
    private final String componentText;
    private final String componentHover;
    private final String componentCommand;

    private Question(Builder builder) {
        this.answers = Collections.unmodifiableList(new ArrayList<>(builder.answers));
        this.display = builder.display;
        this.placeholders = Collections.unmodifiableMap(new LinkedHashMap<>(builder.placeholders));
        this.hints = Collections.unmodifiableList(new ArrayList<>(builder.hints));
        this.revealHints = builder.revealHints;
        this.componentPlaceholder = builder.componentPlaceholder;
        this.componentText = builder.componentText;
        this.componentHover = builder.componentHover;
        this.componentCommand = builder.componentCommand;
    }

    /**
     * @param answer the answer shown when the round ends; more can be added with {@link Builder#answer}
     */
    public static Builder builder(String answer) {
        return new Builder().answer(answer);
    }

    /** @return every accepted answer; the first is the one shown when the round ends */
    public List<String> getAnswers() {
        return answers;
    }

    /** @return the text for {@code %selection%} */
    public String getDisplay() {
        return display;
    }

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    /** @return the hints, shown one by one while the round runs (if the game's hints are on) */
    public List<String> getHints() {
        return hints;
    }

    /** @return true if ChatGames should make the hints by revealing the answer letter by letter */
    public boolean isRevealHints() {
        return revealHints;
    }

    /** @return the placeholder an interactive chat text replaces, or null if there is none */
    public String getComponentPlaceholder() {
        return componentPlaceholder;
    }

    public String getComponentText() {
        return componentText;
    }

    public String getComponentHover() {
        return componentHover;
    }

    /** @return the command run when the text is clicked, or null if it isn't clickable */
    public String getComponentCommand() {
        return componentCommand;
    }

    public static final class Builder {
        private final List<String> answers = new ArrayList<>();
        private String display = "";
        private final Map<String, String> placeholders = new LinkedHashMap<>();
        private final List<String> hints = new ArrayList<>();
        private boolean revealHints;
        private String componentPlaceholder;
        private String componentText;
        private String componentHover;
        private String componentCommand;

        private Builder() {}

        /** Adds an accepted answer. Blank and repeated answers are ignored. */
        public Builder answer(String answer) {
            if (answer != null && !answer.trim().isEmpty() && !answers.contains(answer)) answers.add(answer);
            return this;
        }

        public Builder answers(Iterable<String> more) {
            for (String answer : more) answer(answer);
            return this;
        }

        /** The text for {@code %selection%}: the question, puzzle or whatever players react to. */
        public Builder display(String display) {
            this.display = display == null ? "" : display;
            return this;
        }

        /** A placeholder for this round's messages, e.g. {@code placeholder("%category%", "Fruit")}. */
        public Builder placeholder(String key, Object value) {
            placeholders.put(key, String.valueOf(value));
            return this;
        }

        /** Hints, shown one by one while the round runs. At most 3 are used. */
        public Builder hints(List<String> hints) {
            this.hints.addAll(hints);
            return this;
        }

        /** Makes hints that reveal the first answer a letter at a time: "A____", "Ap___"... */
        public Builder revealHints() {
            this.revealHints = true;
            return this;
        }

        /**
         * Interactive chat text that replaces {@code placeholder} in the start announcement, so
         * the game's start message must contain that placeholder.
         *
         * <p>To make a click answer the round, use {@code "/chatgames " + answer} as the command,
         * like the built-in Clickable game: the click then counts as that answer, and typing the
         * answer in chat doesn't. Give such a round an answer nobody can guess, e.g. a random key.</p>
         *
         * @param hoverText    shown when the text is hovered
         * @param clickCommand run by the player who clicks it, or null for text that is only hovered
         */
        public Builder component(String placeholder, String text, String hoverText, String clickCommand) {
            this.componentPlaceholder = placeholder;
            this.componentText = text;
            this.componentHover = hoverText;
            this.componentCommand = clickCommand;
            return this;
        }

        /** @throws IllegalStateException if no answer was given */
        public Question build() {
            if (answers.isEmpty()) throw new IllegalStateException("A question needs at least one answer");
            return new Question(this);
        }
    }
}
