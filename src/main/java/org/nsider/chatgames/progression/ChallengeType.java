package org.nsider.chatgames.progression;

/**
 * What a challenge counts. The {@code type} in challenges.yml.
 */
public enum ChallengeType {
    /** Chat games or races won, in any place. */
    WIN_GAMES,
    /** Wins of the one game or race named in {@code game}. */
    WIN_GAME,
    /** Races won. */
    WIN_RACES,
    /** Global games taken part in (contributed at least once). */
    PLAY_GLOBAL,
    /** Global games completed: the server reached the goal and the player contributed enough. */
    WIN_GLOBAL,
    /** Chat games won in {@code seconds} or less (races don't count). */
    FAST_WIN,
    /** Points earned from games. */
    EARN_POINTS,
    /** Game events taken part in. */
    PLAY_EVENT,
    /** A type another plugin added through the API; challenges.yml names that type instead. */
    CUSTOM
}
