package org.nsider.chatgames.games.builder.logic;

public interface GameHandler<T> {
    T handle(String... args);
}
