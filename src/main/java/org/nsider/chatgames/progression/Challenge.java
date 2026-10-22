package org.nsider.chatgames.progression;

import org.nsider.chatgames.menus.Menus;
import org.nsider.chatgames.api.GameType;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * One challenge from challenges.yml. Unchanging.
 */
public final class Challenge {

    private final String id;
    private final Period period;
    private final ChallengeType type;
    private final int amount;
    @Nullable private final GameType game;
    private final double seconds;
    private final String name;
    private final String description;
    private final String icon;
    private final List<String> rewards;

    Challenge(String id, Period period, ChallengeType type, int amount, @Nullable GameType game, double seconds,
              String name, String description, String icon, List<String> rewards) {
        this.id = id;
        this.period = period;
        this.type = type;
        this.amount = amount;
        this.game = game;
        this.seconds = seconds;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.rewards = Collections.unmodifiableList(rewards);
    }

    /**
     * @return how much the activity adds to this challenge, 0 if it doesn't count
     */
    int progressFor(@Nonnull Activity activity) {
        switch (type) {
            case WIN_GAMES:
                return activity.getKind() == Activity.Kind.WIN ? 1 : 0;
            case WIN_GAME:
                return activity.getKind() == Activity.Kind.WIN && activity.getGame() == game ? 1 : 0;
            case WIN_RACES:
                return activity.getKind() == Activity.Kind.WIN && activity.getGame() != null && activity.getGame().isRace() ? 1 : 0;
            case PLAY_GLOBAL:
                return activity.getKind() == Activity.Kind.GLOBAL ? 1 : 0;
            case WIN_GLOBAL:
                return activity.getKind() == Activity.Kind.GLOBAL && activity.isWin() ? 1 : 0;
            case FAST_WIN:
                return activity.isWin() && activity.getGame() != null && !activity.getGame().isRace()
                        && activity.getSeconds() <= seconds ? 1 : 0;
            case EARN_POINTS:
                return Math.max(0, activity.getPoints());
            case PLAY_EVENT:
                return activity.getKind() == Activity.Kind.EVENT ? 1 : 0;
            default:
                return 0;
        }
    }

    /**
     * Fills {@code %amount%}, {@code %game%}, {@code %seconds%} and {@code %challenge%} in a
     * text from this challenge (its name, description or a reward line).
     */
    @Nonnull
    public String fill(@Nonnull String text) {
        String seconds = this.seconds == Math.floor(this.seconds) ? String.valueOf((long) this.seconds) : String.valueOf(this.seconds);
        // The name goes in first, so its own placeholders are filled too
        return text.replace("%challenge%", name)
                .replace("%amount%", String.valueOf(amount))
                .replace("%game%", game == null ? "" : Menus.gameName(game))
                .replace("%seconds%", seconds);
    }

    /** @return the key in challenges.yml, e.g. "win_5" */
    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public Period getPeriod() {
        return period;
    }

    @Nonnull
    public ChallengeType getType() {
        return type;
    }

    /** @return the count needed to complete it */
    public int getAmount() {
        return amount;
    }

    @Nullable
    public GameType getGame() {
        return game;
    }

    public double getSeconds() {
        return seconds;
    }

    /** @return the display name, with its colour codes, placeholders filled in */
    @Nonnull
    public String getName() {
        return fill(name);
    }

    @Nonnull
    public String getDescription() {
        return fill(description);
    }

    /** @return the menu icon's material name */
    @Nonnull
    public String getIcon() {
        return icon;
    }

    /** @return the reward lines, in the rewards.yml {@code data} format */
    @Nonnull
    public List<String> getRewards() {
        return rewards;
    }
}
