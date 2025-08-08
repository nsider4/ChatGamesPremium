package org.nsider.chatgames.games.builder;

import lombok.Getter;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.config.settings.ListConfigValue;
import org.nsider.chatgames.games.builder.algorithms.*;
import org.nsider.chatgames.games.utils.EquationResult;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GameAlgorithms {

    private final ChatGames plugin;

    public GameAlgorithms(ChatGames instance) {
        this.plugin = instance;
    }

    public String scrambleWord(String word) {
        return ScrambleAndReverse.scrambleWord(word);
    }

    public String reverse(String word) {
        return ScrambleAndReverse.reverse(word);
    }

    public String generateRandomCharacters(int length, RandomCharacterGenerator.Type type) {
        return RandomCharacterGenerator.generateRandomCharacters(plugin, length, type);
    }

    public String randomMathQuestion() {
        List<String> operators = new ArrayList<>(ListConfigValue.getStringList(ConfigPaths.MATH_OPERATORS));
        int difficulty = ConfigValue.getInt(ConfigPaths.MATH_DIFFICULTY);
        return MathQuestionGenerator.generateRandomMathQuestion(operators, difficulty);
    }

    public String hideLetters(String word, int hidePercent) {
        return LetterHider.hideLetters(word, hidePercent);
    }

    public int getRandomLength(String range) {
        return RandomLengthGenerator.getRandomLength(range);
    }

    public EquationResult createSymbolEquation() {
        return SymbolEquationCreator.createSymbolEquation(plugin);
    }

    public List<String> generateMemorizationList(int listLength) {
        return MemorizationGenerator.generateMemorizationList(listLength);
    }
}
