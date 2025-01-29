package org.nsider.chatgames.games.builder.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScrambleAndReverse {

    public static String scrambleWord(String word) {
        String[] words = word.split(" ");
        List<String> scrambledWords = new ArrayList<>();

        for (String newWord : words) {
            scrambledWords.add(scrambleSingleWord(newWord));
        }

        return String.join(" ", scrambledWords);
    }

    private static String scrambleSingleWord(String word) {
        List<Character> characters = new ArrayList<>();
        for (char c : word.toCharArray()) {
            characters.add(c);
        }

        Collections.shuffle(characters);

        StringBuilder scrambledWord = new StringBuilder();
        for (char c : characters) {
            scrambledWord.append(c);
        }

        return scrambledWord.toString();
    }

    public static String reverse(String word) {
        return new StringBuilder(word).reverse().toString();
    }
}

