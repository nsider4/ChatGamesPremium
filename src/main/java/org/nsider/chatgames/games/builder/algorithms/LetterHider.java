package org.nsider.chatgames.games.builder.algorithms;

import java.util.ArrayList;
import java.util.List;

public class LetterHider {

    public static String hideLetters(String word, int hidePercent) {
        validateHidePercent(hidePercent);

        String[] words = word.split(" ");
        List<String> hiddenWords = new ArrayList<>();

        for (String s : words) {
            hiddenWords.add(hideLettersInWord(s, hidePercent));
        }

        return String.join(" ", hiddenWords);
    }

    private static void validateHidePercent(int hidePercent) {
        if (hidePercent <= 0 || hidePercent > 100) {
            throw new IllegalArgumentException("Percentage should be between 1 and 100");
        }
    }

    private static String hideLettersInWord(String word, int hidePercent) {
        int numHidden = (int) Math.ceil(word.length() * hidePercent / 100.0);
        StringBuilder hiddenWord = new StringBuilder(word);

        for (int i = 0; i < numHidden; i++) {
            int hidden = (int) (Math.random() * hiddenWord.length());
            hiddenWord.setCharAt(hidden, '_');
        }

        return hiddenWord.toString();
    }
}
