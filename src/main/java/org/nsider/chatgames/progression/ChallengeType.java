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
    /** Global games solved in time. */
    PLAY_GLOBAL,
    /** Global games finished in a rewarded place. */
    WIN_GLOBAL,
    /** Chat games won in {@code seconds} or less (races don't count). */
    FAST_WIN,
    /** Points earned from games. */
    EARN_POINTS,
    /** Game events taken part in. */
    PLAY_EVENT
}
