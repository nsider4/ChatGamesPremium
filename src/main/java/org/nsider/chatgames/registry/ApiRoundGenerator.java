package org.nsider.chatgames.registry;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.api.Question;
import org.nsider.chatgames.api.QuestionContext;
import org.nsider.chatgames.api.QuestionGenerator;
import org.nsider.chatgames.games.round.Hints;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.games.round.RoundContext;
import org.nsider.chatgames.games.round.RoundGenerator;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Plays another plugin's chat game: asks its {@link QuestionGenerator} for a question and turns
 * it into a {@link Round}, so it runs through the same code as the built-in games.
 */
final class ApiRoundGenerator implements RoundGenerator {

    private final GameType type;
    private final QuestionGenerator generator;

    ApiRoundGenerator(@Nonnull GameType type, @Nonnull QuestionGenerator generator) {
        this.type = type;
        this.generator = generator;
    }

    @Nonnull
    @Override
    public Round generate(@Nonnull RoundContext context, @Nonnull String... args) {
        Question question;
        try {
            question = generator.generate(new Context(type, context, args));
        } catch (LinkageError e) {
            // The plugin was built against classes this server doesn't have
            throw new IllegalStateException(type.getOwner() + "'s generator failed: " + e, e);
        }
        if (question == null) throw new IllegalStateException(type.getOwner() + "'s generator returned no question");
        return toRound(type, question);
    }

    @Nonnull
    static Round toRound(@Nonnull GameType type, @Nonnull Question question) {
        List<String> answers = question.getAnswers();
        Round.Builder round = Round.builder(type, answers.get(0)).answers(answers).display(question.getDisplay());
        for (Map.Entry<String, String> placeholder : question.getPlaceholders().entrySet()) {
            round.placeholder(placeholder.getKey(), placeholder.getValue());
        }
        if (!question.getHints().isEmpty()) {
            round.hints(question.getHints().subList(0, Math.min(Hints.PREPARED, question.getHints().size())));
        } else if (question.isRevealHints()) {
            round.hints(Hints.revealFromStart(answers.get(0)));
        }
        if (question.getComponentPlaceholder() != null) {
            round.component(new Round.ComponentSpec(question.getComponentPlaceholder(), question.getComponentText(),
                    question.getComponentHover(), question.getComponentCommand()));
        }
        return round.build();
    }

    private static final class Context implements QuestionContext {
        private final GameType type;
        private final RoundContext context;
        private final String[] args;

        Context(GameType type, RoundContext context, String[] args) {
            this.type = type;
            this.context = context;
            this.args = args.clone();
        }

        @Override
        public GameType getGame() {
            return type;
        }

        @Override
        public ConfigurationSection getSettings() {
            return context.getSettings();
        }

        @Override
        public ConfigurationSection getMessages() {
            return context.getMessages();
        }

        @Override
        public ConfigurationSection getWords() {
            return context.getWords();
        }

        @Override
        public String[] getArgs() {
            return args.clone();
        }

        @Override
        public Random getRandom() {
            return context.getRandom();
        }

        @Override
        public String randomWord(List<String> list) {
            List<String> usable = context.filterBlacklisted(list);
            if (usable.isEmpty()) throw new IllegalStateException(type.getId() + ": no usable words in the list");
            return usable.get(context.getRandom().nextInt(usable.size()));
        }
    }
}
