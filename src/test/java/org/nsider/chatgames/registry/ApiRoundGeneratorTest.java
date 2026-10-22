package org.nsider.chatgames.registry;

import org.junit.jupiter.api.Test;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.api.Question;
import org.nsider.chatgames.games.round.Hints;
import org.nsider.chatgames.games.round.Round;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRoundGeneratorTest {

    @Test
    void aQuestionBecomesARound() {
        Question question = Question.builder("Paris").answer("paris").answer(" ").answer("Paris")
                .display("Capital of France?").placeholder("%category%", "Geography").build();
        Round round = ApiRoundGenerator.toRound(GameType.TRIVIA, question);
        assertEquals(Arrays.asList("Paris", "paris"), round.getAnswers(), "blank and repeated answers are dropped");
        assertEquals("Paris", round.getMainAnswer());
        assertEquals("Capital of France?", round.getDisplay());
        assertEquals("Geography", round.getPlaceholders().get("%category%"));
        assertTrue(round.getHints().isEmpty());
        assertNull(round.getComponent());
    }

    @Test
    void hintsAreCappedOrRevealedFromTheAnswer() {
        Question many = Question.builder("apple").hints(Arrays.asList("1", "2", "3", "4", "5")).build();
        assertEquals(Hints.PREPARED, ApiRoundGenerator.toRound(GameType.TRIVIA, many).getHints().size());

        Question reveal = Question.builder("apple").revealHints().build();
        assertEquals(Hints.revealFromStart("apple"), ApiRoundGenerator.toRound(GameType.TRIVIA, reveal).getHints());
    }

    @Test
    void componentsAreKept() {
        Question question = Question.builder("go").component("%button%", "[Click]", "Click me", "/cg answer go").build();
        Round.ComponentSpec component = ApiRoundGenerator.toRound(GameType.CLICKABLE, question).getComponent();
        assertNotNull(component);
        assertEquals("%button%", component.getPlaceholder());
        assertEquals("[Click]", component.getText());
    }

    @Test
    void aQuestionNeedsAnAnswer() {
        assertThrows(IllegalStateException.class, () -> Question.builder(" ").build());
    }
}
