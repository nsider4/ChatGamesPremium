package org.nsider.chatgames.registry;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.api.ChatGame;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.api.RaceGame;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Adds a new game's sections to the server's config files the first time it is registered.
 *
 * <p>The sections are appended as text, so the comments in the owner's files stay as they are
 * (saving the whole file through Bukkit would drop them on old versions). A file that already
 * has the game's section is left alone: the owner may have changed it.</p>
 */
final class GameDefaults {

    private static final String BANNER = "<center>&r✓ ✯ %prefix%&r ✯ ✓";

    private GameDefaults() {}

    static void addChatGame(@Nonnull ChatGames plugin, @Nonnull GameType type, @Nonnull ChatGame game, @Nonnull Plugin owner) {
        String id = type.getConfigKey();
        String header = "#" + game.getDisplayName() + ", added by " + owner.getName();

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("enable", true);
        settings.put("winners", 1);
        settings.put("weight", 1);
        settings.put("points", 1);
        settings.put("hints.enable", true);
        settings.put("hints.after-percent", 50);
        settings.put("hints.max", 1);
        settings.put("countdown.enable", true);
        settings.put("countdown.last-seconds", 10);
        settings.put("case-sensitive", false);
        settings.put("timeToGuess_seconds", 20);
        settings.put("titleMessage", "&b%selection%");
        settings.put("subtitleMessage", "&a" + game.getDisplayName());
        settings.putAll(game.getSettings());
        append(plugin, ConfigManager.getGames(), id, header, settings);

        Map<String, Object> messages = new LinkedHashMap<>();
        messages.put("gameStartAnnouncement", or(game.getStartMessage(), Arrays.asList("", BANNER, "",
                "<center>&fYou have &e%timeToGuess% seconds &fto answer:", "<center>&b%selection%", "")));
        messages.put("correct_message", or(game.getWinMessage(), Arrays.asList("", BANNER, "",
                "<center>&a%winner_1% &7was the first to answer `&e%correct_answer%&7` (&c%time_1%s&7) and got a prize!", "")));
        messages.put("time_expired_message", or(game.getExpiredMessage(), Arrays.asList("", BANNER, "",
                "<center>&f%timeToGuess%s &chave passed! &fNobody got it.", "<center>The answer was &e%correct_answer%&f!", "")));
        append(plugin, ConfigManager.getMessagesConfig(), id, header, messages);

        List<String> rewards = or(game.getRewards(), Collections.singletonList("[playermsg] &aYou won a round of " + game.getDisplayName() + "!"));
        append(plugin, ConfigManager.getRewards(), id, header, Collections.<String, Object>singletonMap("rewards.1.reward_1.data", rewards));
    }

    static void addRace(@Nonnull ChatGames plugin, @Nonnull GameType type, @Nonnull RaceGame race, @Nonnull Plugin owner) {
        String id = type.getConfigKey();
        String header = "#" + race.getDisplayName() + ", added by " + owner.getName();

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("enable", true);
        settings.put("weight", 1);
        settings.put("points", 1);
        settings.put("timeToComplete_seconds", 180);
        settings.put("titleMessage", "&b" + race.getDisplayName().toUpperCase(Locale.ROOT));
        settings.put("subtitleMessage", "&a%amount%x %value%");
        settings.putAll(race.getSettings());
        for (Map.Entry<String, List<Integer>> target : race.getTargets().entrySet()) {
            for (int amount : target.getValue()) {
                // Every amount pays the first shared race reward (rewards.yml, races.rewards)
                settings.put("data." + target.getKey() + "." + amount + ".rewards", Collections.singletonList("reward_1"));
            }
        }
        append(plugin, ConfigManager.getRaces(), id, header, settings);

        Map<String, Object> messages = new LinkedHashMap<>();
        messages.put("gameStartAnnouncement", or(race.getStartMessage(), Arrays.asList("", BANNER, "",
                "<center>&fYou have &e%timeToComplete% seconds &ffor the &d%game%&f: `&b%amount%&fx &3%value%`", "")));
        messages.put("correct_message", or(race.getWinMessage(), Arrays.asList("", BANNER, "",
                "<center>&a%winner% &7won the %game% with `&e%amount%&fx &3%value%&7` (&c%time%s&7) and got a prize!", "")));
        messages.put("time_expired_message", or(race.getExpiredMessage(), Arrays.asList("", BANNER, "",
                "<center>&f%timeToComplete%s &chave passed! &fThe %game% is now over!")));
        append(plugin, ConfigManager.getMessagesConfig(), id, header, messages);
    }

    /** Appends {@code id:} with these values (dotted paths nest) unless the file has that section. */
    private static void append(ChatGames plugin, @Nullable YamlConfig file, String id, String header, Map<String, Object> values) {
        if (file == null || file.isConfigurationSection(id)) return;
        YamlConfiguration section = new YamlConfiguration();
        for (Map.Entry<String, Object> value : values.entrySet()) {
            section.set(id + "." + value.getKey(), value.getValue());
        }
        try {
            String text = new String(Files.readAllBytes(file.getFile().toPath()), StandardCharsets.UTF_8);
            String newline = text.contains("\r\n") ? "\r\n" : "\n";
            StringBuilder added = new StringBuilder();
            if (!text.isEmpty() && !text.endsWith("\n")) added.append(newline);
            added.append(newline).append(header).append(newline);
            added.append(section.saveToString().replace("\r\n", "\n").replace("\n", newline));
            Files.write(file.getFile().toPath(), (text + added).getBytes(StandardCharsets.UTF_8));
            file.reload();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not add " + id + " to " + file.getFile().getName(), e);
            // Still playable this run with the defaults in memory
            for (Map.Entry<String, Object> value : values.entrySet()) {
                file.set(id + "." + value.getKey(), value.getValue());
            }
        }
    }

    private static <T> T or(@Nullable T value, T fallback) {
        return value != null ? value : fallback;
    }
}
