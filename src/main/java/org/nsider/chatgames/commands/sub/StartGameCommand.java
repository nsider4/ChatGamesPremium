package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.races.RaceData;
import org.nsider.chatgames.races.RaceTarget;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * {@code /chatgames <game> [values]}: starts one game now, e.g. {@code /chatgames unscramble creeper}
 * or {@code /chatgames math 12 x 3}. One instance is registered per game and race.
 *
 * <p>The values are passed to the game's round generator; each game documents its own
 * in games.yml. Races take a target and amount: {@code /chatgames mine diamond_ore 5}
 * (without the amount, one races.yml lists for it). A game can be started this way even
 * when it is disabled in the rotation.</p>
 */
public class StartGameCommand extends SubCommand {

    private final ChatGames plugin;
    private final GameType type;

    public StartGameCommand(@Nonnull ChatGames plugin, @Nonnull GameType type) {
        // Memorize was called "shoppinglist" in 1.9.9
        super(type.getConfigKey(), null, false, type == GameType.MEMORIZE ? new String[]{"shoppinglist"} : new String[0]);
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public boolean canUse(@Nonnull CommandSender sender) {
        return CommandSupport.canStart(sender, type);
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        start(plugin, sender, type, args);
    }

    /**
     * Starts a game for a command sender, with the checks every start command shares.
     */
    static void start(@Nonnull ChatGames plugin, @Nonnull CommandSender sender, @Nonnull GameType type, @Nonnull String[] args) {
        if (sender instanceof Player && !plugin.getGameManager().worldEnabled((Player) sender)
                && !ConfigValue.getBoolean(ConfigPaths.ALLOW_STARTING_WHEN_DISABLED)
                && !sender.hasPermission(PermissionKey.START_ALL.getPermission())) {
            CommandSupport.send(sender, MessageKey.CANNOT_START_DISABLED);
            return;
        }
        if (type.isRace() && !validRaceValues(plugin, sender, type, args)) return;
        // Game state lives on the main thread (the global region on Folia)
        plugin.getScheduler().onGlobal(() -> {
            if (plugin.isInGame()) {
                CommandSupport.send(sender, MessageKey.GAME_ALREADY_RUNNING);
                return;
            }
            boolean started;
            try {
                started = plugin.getGameManager().startGame(type, args);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not start " + type.getConfigKey(), e);
                started = false;
            }
            CommandSupport.send(sender, started ? MessageKey.GAME_STARTED : MessageKey.GAME_NOT_STARTED,
                    "%game%", type.getConfigKey());
        });
    }

    /**
     * Checks a race's target and amount up front, so the sender learns what was wrong.
     */
    private static boolean validRaceValues(ChatGames plugin, CommandSender sender, GameType type, String[] args) {
        if (args.length == 0) return true;
        if (RaceTarget.resolve(type, args[0]) == null) {
            CommandSupport.send(sender, MessageKey.TYPE_NOT_FOUND, "%type%", args[0]);
            return false;
        }
        if (args.length == 1) {
            if (!plugin.getRaceManager().getData(type).getAmounts(args[0]).isEmpty()) return true;
            CommandSupport.usage(sender, "/chatgames " + type.getConfigKey() + " " + args[0] + " <amount>");
            return false;
        }
        Integer amount = CommandSupport.parseAmount(sender, args[1]);
        if (amount == null) return false;
        if (amount == 0) {
            CommandSupport.send(sender, MessageKey.NUMBER_NOT_FOUND);
            return false;
        }
        return true;
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (type.isRace()) return raceSuggestions(args);
        if (args.length != 1) return Collections.emptyList();
        // A hint of what each game accepts
        switch (type) {
            case MATH:
                return Collections.singletonList("<expression>");
            case GUESS_THE_NUMBER:
                return Collections.singletonList("<number> <spread>");
            case RANDOM:
            case MEMORIZE:
                return Collections.singletonList("<length>");
            case UNSCRAMBLE:
            case UNREVERSE:
            case REACTION:
            case FILLOUT:
            case HOVERABLE:
                return Collections.singletonList("<word>");
            case TRIVIA:
                return Collections.singletonList("<question id>");
            default:
                return Collections.emptyList();
        }
    }

    /** The targets races.yml lists, then their amounts; any other valid name also works. */
    private List<String> raceSuggestions(String[] args) {
        RaceData data = plugin.getRaceManager().getData(type);
        if (args.length == 1) {
            List<String> targets = new ArrayList<>();
            for (String target : data.getTargets()) targets.add(target.toLowerCase(Locale.ROOT));
            List<String> matches = MessageUtils.filterSuggestions(targets, args[0]);
            return matches.isEmpty() ? Collections.singletonList(type == GameType.HUNT ? "<mob>" : "<material>") : matches;
        }
        if (args.length == 2) {
            List<String> amounts = new ArrayList<>();
            for (int amount : data.getAmounts(args[0])) amounts.add(String.valueOf(amount));
            return amounts.isEmpty() ? Collections.singletonList("<amount>") : MessageUtils.filterSuggestions(amounts, args[1]);
        }
        return Collections.emptyList();
    }
}
