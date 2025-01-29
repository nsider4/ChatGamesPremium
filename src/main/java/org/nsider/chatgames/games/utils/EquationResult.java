package org.nsider.chatgames.games.utils;

import lombok.Getter;
import java.util.List;

@Getter
public class EquationResult {
    private final List<String> messageLines;
    private final int answer;

    public EquationResult(List<String> messageLines, int answer) {
        this.messageLines = messageLines;
        this.answer = answer;
    }
}


