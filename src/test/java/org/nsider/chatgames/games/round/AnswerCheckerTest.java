package org.nsider.chatgames.games.round;

import org.junit.jupiter.api.Test;
import org.nsider.chatgames.enums.GameType;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.nsider.chatgames.games.round.AnswerChecker.Result.CORRECT;
import static org.nsider.chatgames.games.round.AnswerChecker.Result.WRONG;
import static org.nsider.chatgames.games.round.AnswerChecker.Result.WRONG_CASE;

class AnswerCheckerTest {

    private static Round word(String answer) {
        return Round.builder(GameType.UNSCRAMBLE, answer).build();
    }

    @Test
    void spacesAndOneTrailingFullStopAreIgnored() {
        Round round = word("Nether Portal");
        assertEquals(CORRECT, AnswerChecker.check(round, "Nether Portal", true));
        assertEquals(CORRECT, AnswerChecker.check(round, "NetherPortal.", true));
        assertEquals(CORRECT, AnswerChecker.check(round, "  Nether  Portal  ", true));
        assertEquals(WRONG, AnswerChecker.check(round, "Nether Portals", true));
        assertEquals(WRONG, AnswerChecker.check(round, "   ", true));
    }

    @Test
    void capitalisationOnlyCountsInCaseSensitiveGames() {
        Round round = word("Creeper");
        assertEquals(WRONG_CASE, AnswerChecker.check(round, "creeper", true));
        assertEquals(CORRECT, AnswerChecker.check(round, "creeper", false));
        assertEquals(WRONG, AnswerChecker.check(round, "zombie", true));
    }

    @Test
    void anyAcceptedAnswerWins() {
        Round round = Round.builder(GameType.TRIVIA, "Nether Portal").answers(Arrays.asList("portal", "nether gate")).build();
        assertEquals(CORRECT, AnswerChecker.check(round, "PORTAL", false));
        assertEquals(CORRECT, AnswerChecker.check(round, "nether gate", false));
        assertEquals("Nether Portal", round.getMainAnswer());
    }

    @Test
    void listsCompareItemByItem() {
        Round ordered = Round.builder(GameType.MEMORIZE, "Candy Cane, Ribbon, Sleigh").listAnswer(true).build();
        assertEquals(CORRECT, AnswerChecker.check(ordered, "candy cane,ribbon, sleigh", false));
        assertEquals(CORRECT, AnswerChecker.check(ordered, "Candy Cane , Ribbon , Sleigh.", false));
        assertEquals(WRONG, AnswerChecker.check(ordered, "Ribbon, Candy Cane, Sleigh", false));
        assertEquals(WRONG, AnswerChecker.check(ordered, "Candy Cane, Ribbon", false));
        assertEquals(WRONG, AnswerChecker.check(ordered, "Candy Cane, Ribbon, Sleigh, Sleigh", false));

        Round anyOrder = Round.builder(GameType.MEMORIZE, "Candy Cane, Ribbon, Sleigh").listAnswer(false).build();
        assertEquals(CORRECT, AnswerChecker.check(anyOrder, "sleigh, candy cane, ribbon", false));
        assertEquals(WRONG, AnswerChecker.check(anyOrder, "sleigh, sleigh, ribbon", false));
    }

    @Test
    void listsCanBeCaseSensitive() {
        Round round = Round.builder(GameType.MEMORIZE, "Red, Blue").listAnswer(true).build();
        assertEquals(CORRECT, AnswerChecker.check(round, "Red, Blue", true));
        assertEquals(WRONG_CASE, AnswerChecker.check(round, "red, blue", true));
    }
}
