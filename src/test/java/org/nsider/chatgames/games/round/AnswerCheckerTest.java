package org.nsider.chatgames.games.round;

import org.junit.jupiter.api.Test;
import org.nsider.chatgames.api.GameType;

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
    void oneTrailingExclamationOrQuestionMarkIsIgnored() {
        Round round = word("Creeper");
        assertEquals(CORRECT, AnswerChecker.check(round, "creeper!", false));
        assertEquals(CORRECT, AnswerChecker.check(round, "creeper?", false));
        assertEquals(WRONG, AnswerChecker.check(round, "creeper!!", false));
        assertEquals(CORRECT, AnswerChecker.check(word("-5"), "-5!", false));
    }

    @Test
    void accentsOnLatinLettersAreIgnored() {
        assertEquals(CORRECT, AnswerChecker.check(word("Café"), "cafe", false));
        assertEquals(CORRECT, AnswerChecker.check(word("Araña"), "arana", false));
        assertEquals(CORRECT, AnswerChecker.check(word("Spinne"), "Spínne", true));
        assertEquals(WRONG_CASE, AnswerChecker.check(word("Électricité"), "electricite", true));
        // Elsewhere the marks change the word
        assertEquals(WRONG, AnswerChecker.check(word("が"), "か", false));
        assertEquals(WRONG, AnswerChecker.check(word("йод"), "иод", false));
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

    @Test
    void colourAddedByChatPluginsIsIgnored() {
        Round round = word("Diamond Sword");
        // ColorChat / EssentialsX turn the message into coloured text before other plugins see it
        assertEquals(CORRECT, AnswerChecker.check(round, "§bDiamond Sword", true));
        assertEquals(CORRECT, AnswerChecker.check(round, "§l§aDiamond §cSword§r", true));
        assertEquals(CORRECT, AnswerChecker.check(round, "§x§f§f§5§7§3§3Diamond Sword", true));
        // Codes the player typed themselves
        assertEquals(CORRECT, AnswerChecker.check(round, "&bDiamond Sword", true));
        assertEquals(CORRECT, AnswerChecker.check(round, "&#FF5733Diamond Sword", true));
        assertEquals(WRONG_CASE, AnswerChecker.check(round, "§bdiamond sword", true));
    }

    @Test
    void anAmpersandInTheAnswerStillCounts() {
        assertEquals(CORRECT, AnswerChecker.check(word("R&B"), "R&B", true));
        assertEquals(CORRECT, AnswerChecker.check(word("R&B"), "§aR&B", true));
        assertEquals(WRONG, AnswerChecker.check(word("R&B"), "R", true));
    }

    @Test
    void invisibleCharactersAreIgnored() {
        // Anti-spam and chat plugins slip zero-width characters in; resource-pack glyph plugins too
        assertEquals(CORRECT, AnswerChecker.check(word("Creeper"), "Cre​eper", true));
        assertEquals(CORRECT, AnswerChecker.check(word("Creeper"), "﻿Creeper‍", true));
    }
}
