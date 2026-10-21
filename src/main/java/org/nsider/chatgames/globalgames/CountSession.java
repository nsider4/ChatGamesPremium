package org.nsider.chatgames.globalgames;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.games.manager.GameSoundManager;
import org.nsider.chatgames.utils.messages.Msg;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Count: the server counts together in chat, one number per message, e.g. 1 to 20 (or
 * down, or in steps). A wrong number (ahead, behind, the one just said) or the same
 * player twice in a row is a mistake: the count starts over, or the game fails. Only
 * messages that are just a number are tries; they are hidden and shown again as a
 * formatted line. See {@link Counter} for the rules.
 */
final class CountSession extends GlobalSession {

    private final Counter counter;
    private final GlobalGameConfig.Count settings;
    private final GlobalGameConfig.Mode mode;
    private int mistakes;

    private CountSession(ChatGames plugin, GlobalGameManager manager, GlobalGameConfig.Entry entry,
                         GlobalGameConfig.Count settings, GlobalGameConfig.Mode mode) {
        this(plugin, manager, entry, settings, mode, new Counter(mode.getStart(), mode.getTarget(), mode.getStep(),
                settings.isNoDoubleCount()));
    }

    private CountSession(ChatGames plugin, GlobalGameManager manager, GlobalGameConfig.Entry entry,
                         GlobalGameConfig.Count settings, GlobalGameConfig.Mode mode, Counter counter) {
        super(plugin, manager, entry, counter.getGoal());
        this.settings = settings;
        this.mode = mode;
        this.counter = counter;
    }

    /**
     * @param args a mode's name, or {@code <start> <target> [step]}; none picks a random mode
     * @return the game, or null if the values are invalid (the console says why)
     */
    @Nullable
    static CountSession create(@Nonnull ChatGames plugin, @Nonnull GlobalGameManager manager,
                               @Nonnull GlobalGameConfig.Entry entry, @Nonnull String[] args) {
        GlobalGameConfig.Count settings = entry.getCount();
        if (settings == null) return null;
        GlobalGameConfig.Mode mode = mode(settings, args, plugin.getRandom());
        if (mode == null) {
            plugin.getLogger().warning("Can't start Count: give one of the modes " + settings.getModes().keySet()
                    + ", or <start> <target> [step].");
            return null;
        }
        return new CountSession(plugin, manager, entry, settings, mode);
    }

    @Nullable
    static GlobalGameConfig.Mode mode(@Nonnull GlobalGameConfig.Count settings, @Nonnull String[] args, @Nonnull Random random) {
        if (args.length == 0) {
            List<GlobalGameConfig.Mode> modes = new ArrayList<>(settings.getModes().values());
            return modes.get(random.nextInt(modes.size()));
        }
        GlobalGameConfig.Mode named = settings.getModes().get(args[0].toLowerCase(Locale.ROOT));
        if (named != null) return named;
        Integer start = Counter.parse(args[0]);
        Integer target = args.length > 1 ? Counter.parse(args[1]) : null;
        Integer step = args.length > 2 ? Counter.parse(args[2]) : Integer.valueOf(1);
        if (start == null || target == null || step == null || step == 0 || start.equals(target)) return null;
        return GlobalGameConfig.Mode.of("custom", start, target, step);
    }

    @Nonnull
    @Override
    Msg startMessage() {
        return fill(Msg.of(GlobalGameManager.messageList("count_start")))
                .with("%start%", counter.getStart())
                .with("%target%", counter.getTarget())
                .with("%step%", Math.abs(counter.getStep()))
                .with("%mode%", mode.getName());
    }

    @Override
    boolean onChat(@Nonnull Player player, @Nonnull String message) {
        Integer number = Counter.parse(message);
        if (number == null) return false;
        // One try at a time, so the count, the shared score and a reset always agree
        synchronized (this) {
            if (progress.isOver()) return true;
            int expected = counter.getNext();
            Counter.Result result = counter.submit(player.getUniqueId(), number);
            if (!result.isMistake()) {
                Msg counted = fill(Msg.of(GlobalGameManager.message("counted")))
                        .with("%player%", player.getName()).with("%number%", number);
                for (Player online : manager.players()) counted.send(online);
                manager.contribute(this, player, 1);
                return true;
            }
            mistakes++;
            boolean fails = settings.isFailOnMistake() || (settings.getMaxMistakes() > 0 && mistakes >= settings.getMaxMistakes());
            Msg broken = fill(Msg.of(GlobalGameManager.messageList(fails ? "count_broken_final" : "count_broken")))
                    .with("%player%", player.getName())
                    .with("%number%", number)
                    .with("%next%", expected)
                    .with("%start%", counter.getStart())
                    .with("%mistakes%", mistakes)
                    .with("%max_mistakes%", settings.getMaxMistakes() > 0 ? String.valueOf(settings.getMaxMistakes()) : "-")
                    .with("%reason%", reason(result, expected));
            for (Player online : manager.players()) {
                broken.send(online);
                GameSoundManager.playGameSound(online, "time-expired");
            }
            if (fails) {
                manager.fail(this);
            } else {
                counter.reset();
                progress.resetProgress();
            }
            return true;
        }
    }

    /** The reason for a mistake from count_reasons, which may say what was expected. */
    private static String reason(Counter.Result result, int expected) {
        String reason = GlobalGameManager.message("count_reasons." + result.name().toLowerCase(Locale.ROOT));
        return reason == null ? result.name().toLowerCase(Locale.ROOT) : reason.replace("%next%", String.valueOf(expected));
    }

    @Override
    boolean announcesMilestones() {
        return false;
    }

    @Nonnull
    @Override
    public String getGoalText() {
        return counter.getStart() + " " + GlobalGameManager.word("to") + " " + counter.getTarget();
    }

    /** @return the number to say next */
    public int getNext() {
        return counter.getNext();
    }
}
