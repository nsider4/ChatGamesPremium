package org.nsider.chatgames.games.manager;

import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.games.Game;
import org.nsider.chatgames.games.round.GameSettings;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.games.utils.PlayerResult;
import org.nsider.chatgames.managers.TopManager;
import org.nsider.chatgames.utils.management.TimeUtils;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.chatgames.utils.messages.Msg;

import org.nsider.inventorymanager.extras.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every chat-game message a round sends: the start, the memorise phase, hints,
 * the action-bar countdown, the winners and "time's up". Messages come from the
 * game's section of messages.yml.
 *
 * <p>Start, hint and countdown messages go only to players who can play (world
 * enabled and games toggled on). Results go to everyone in an enabled world, so
 * players who toggled games off still see who won, as in 1.9.9.</p>
 */
public final class RoundAnnouncer {

    private static final int CLEARED_LINES = 100;

    private final ChatGames plugin;

    public RoundAnnouncer(ChatGames plugin) {
        this.plugin = plugin;
    }

    /**
     * The question. For Memorize this shows the list to remember.
     *
     * @param global a global game: its announcement from global_games.yml goes first
     */
    public void announceStart(@Nonnull Round round, @Nonnull GameSettings settings, boolean global) {
        Msg message = base(round, settings, "gameStartAnnouncement").component(round.getComponent());
        Msg header = global ? plugin.getGlobalGameManager().startMessage(round.getType(), settings.getTimeSeconds()) : null;
        for (Player player : players()) {
            if (!canPlay(player)) continue;
            GameSoundManager.playGameSound(player, "game-start");
            if (header != null) header.send(player);
            message.send(player);
            if (round.getMemorizeSeconds() == 0) {
                sendTitle(player, round, settings);
            }
        }
    }

    /** Memorize: the list disappears and answering opens. */
    public void announceMemorizeOver(@Nonnull Round round, @Nonnull GameSettings settings) {
        Msg message = base(round, settings, "messageAfterMemorized");
        for (Player player : players()) {
            if (!canPlay(player)) continue;
            for (int i = 0; i < CLEARED_LINES; i++) player.sendMessage("");
            message.send(player);
            sendTitle(player, round, settings);
        }
    }

    public void announceHint(@Nonnull Game game, @Nonnull Round round, @Nonnull GameSettings settings, @Nonnull String hint) {
        Msg message = Msg.of(messageOrDefault(round, "hint_message")).with(placeholders(round, settings)).with("%hint%", hint);
        for (Player player : players()) {
            // Players who already placed don't need the hint
            if (canPlay(player) && !game.hasResult(player.getUniqueId())) message.send(player);
        }
    }

    public void showCountdown(@Nonnull Game game, @Nonnull Round round, int secondsLeft) {
        String text = messageOrDefault(round, "countdown_actionbar");
        if (text == null || text.isEmpty()) return;
        String colored = MessageUtils.color(text.replace("%seconds%", String.valueOf(secondsLeft)));
        for (Player player : players()) {
            if (canPlay(player) && !game.hasResult(player.getUniqueId())) {
                ActionBar.sendActionBar(player, colored);
            }
        }
    }

    public void announceWinners(@Nonnull Game game) {
        Round round = game.getRound();
        GameSettings settings = game.getSettings();
        String none = ConfigManager.getMessagesConfig().getString("no_winner_placeholder", "---");
        Msg message = Msg.of(section(round).getStringList("correct_message"))
                .with(placeholders(round, settings))
                .with("%correct_answer%", game.getAnswer());

        List<PlayerResult> results = game.getResultsByPlace();
        for (int place = 1; place <= game.getMaxWinners(); place++) {
            PlayerResult result = place <= results.size() ? results.get(place - 1) : null;
            OfflinePlayer winner = result != null ? result.getPlayer() : null;
            Player online = winner != null ? winner.getPlayer() : null;
            message.with("%winner_" + place + "%", winner != null ? winner.getName() : none)
                    .with("%winner_displayname_" + place + "%", online != null ? online.getDisplayName()
                            : winner != null ? winner.getName() : none)
                    .with("%time_" + place + "%", result != null ? String.valueOf(TimeUtils.roundTo2Decimals(result.getTime())) : none)
                    .with("%points_" + place + "%", winner != null ? String.valueOf(TopManager.getPoints(winner)) : none);
        }

        for (Player player : players()) {
            if (!plugin.getGameManager().worldEnabled(player)) continue;
            message.send(player);
            if (game.hasResult(player.getUniqueId())) {
                GameSoundManager.playGameSound(player, "win");
            }
        }
    }

    public void announceTimeExpired(@Nonnull Game game) {
        Round round = game.getRound();
        Msg message = Msg.of(section(round).getStringList("time_expired_message"))
                .with(placeholders(round, game.getSettings()))
                .with("%correct_answer%", game.getAnswer());
        for (Player player : players()) {
            if (!plugin.getGameManager().worldEnabled(player)) continue;
            message.send(player);
            if (canPlay(player)) GameSoundManager.playGameSound(player, "time-expired");
        }
    }

    private Msg base(Round round, GameSettings settings, String key) {
        return Msg.of(section(round).getStringList(key)).with(placeholders(round, settings));
    }

    private static Map<String, String> placeholders(Round round, GameSettings settings) {
        Map<String, String> values = new LinkedHashMap<>(round.getPlaceholders());
        values.put("%selection%", round.getDisplay());
        values.put("%timeToGuess%", String.valueOf(settings.getTimeSeconds()));
        values.put("%game%", round.getType().getConfigKey());
        return values;
    }

    private void sendTitle(Player player, Round round, GameSettings settings) {
        if (settings.getTitle().isEmpty() && settings.getSubtitle().isEmpty()) return;
        Map<String, String> values = placeholders(round, settings);
        String title = settings.getTitle();
        String subtitle = settings.getSubtitle();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            title = title.replace(entry.getKey(), entry.getValue());
            subtitle = subtitle.replace(entry.getKey(), entry.getValue());
        }
        Titles.sendTitle(player, 10, 60, 10, MessageUtils.color(title), MessageUtils.color(subtitle));
    }

    /** The game's own message, or the shared one at the top of messages.yml. */
    private static String messageOrDefault(Round round, String key) {
        String own = section(round).getString(key);
        return own != null ? own : ConfigManager.getMessagesConfig().getString(key);
    }

    private static ConfigurationSection section(Round round) {
        ConfigurationSection section = GameConfigManager.section(ConfigManager.getMessagesConfig(), round.getType());
        return section != null ? section : new MemoryConfiguration();
    }

    private boolean canPlay(Player player) {
        return plugin.getGameManager().worldEnabled(player) && plugin.getGameManager().gamesIsToggled(player);
    }

    private static List<Player> players() {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }
}
