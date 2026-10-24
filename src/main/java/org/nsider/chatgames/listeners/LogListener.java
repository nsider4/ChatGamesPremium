package org.nsider.chatgames.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.api.event.ChatGamesEndEvent;
import org.nsider.chatgames.api.event.ChatGamesStartEvent;
import org.nsider.chatgames.api.PlayerResult;
import org.nsider.chatgames.races.Race;
import org.nsider.chatgames.races.RaceTarget;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Writes a line for every game and race start and end, to the console and/or the log file
 * that {@code /chatgames logs} reads.
 */
public class LogListener implements Listener {

    private final ChatGames plugin;

    public LogListener(ChatGames instance) {
        this.plugin = instance;
    }

    @EventHandler
    public void onGameStart(ChatGamesStartEvent event) {
        String logMessage = event.getType().isRace()
                ? formatRaceStartLog(event.getType())
                : formatGameStartLog(event);

        log(logMessage);
    }

    @EventHandler
    public void onGameEnd(ChatGamesEndEvent event) {
        String logMessage = event.getType().isRace()
                ? formatRaceEndLog(event)
                : formatGameEndLog(event);

        log(logMessage);
    }

    /**
     * Formats a log message for game start events.
     */
    private String formatGameStartLog(ChatGamesStartEvent event) {
        return String.format(
                "Game Starting: %s | Has Answer: %s%s",
                event.getType().name(),
                event.hasAnswer(),
                event.hasAnswer() ? " | Answer: " + event.getAnswer() : ""
        );
    }

    /**
     * Formats a log message for game end events.
     */
    private String formatGameEndLog(ChatGamesEndEvent event) {
        return String.format(
                "Game Ending: %s | Has Winner: %s%s%s",
                event.getType().name(),
                event.hasWinner(),
                event.hasWinner() ? " | Winners: " + formatWinners(event.getWinners()) : "",
                event.hasAnswer() ? " | Answer: " + event.getAnswer() : ""
        );
    }

    /**
     * Formats a log message for race start events.
     */
    private String formatRaceStartLog(GameType gameType) {
        // Start and end events fire while the race is still the current one
        Race race = plugin.getRaceManager().getCurrent();
        if (race == null) return "";

        return String.format("Race Starting: %s | %s | Amount: %d", gameType.name(), target(race), race.getAmount());
    }

    /**
     * Formats a log message for race end events.
     */
    private String formatRaceEndLog(ChatGamesEndEvent event) {
        Race race = plugin.getRaceManager().getCurrent();
        if (race == null) return "";

        return String.format("Race Ending: %s | Has Winner: %s%s | %s | Amount: %d",
                event.getType().name(), event.hasWinner() ? "Yes" : "No",
                event.hasWinner() ? " | Winners: " + formatWinners(event.getWinners()) : "",
                target(race), race.getAmount());
    }

    private static String target(Race race) {
        RaceTarget target = race.getTarget();
        String kind = race.getType().isMaterialRace() ? "Material: " : race.getType().isMobRace() ? "Entity: " : "Target: ";
        return kind + target.getServerName();
    }

    /**
     * Formats the list of winners.
     */
    private String formatWinners(List<PlayerResult> winners) {
        if (winners.isEmpty()) return "None";

        StringBuilder winnersList = new StringBuilder();
        for (PlayerResult result : winners) {
            winnersList.append(result.getPlayer().getName())
                    .append(" (Time: ").append(result.getTime()).append("s), ");
        }
        winnersList.setLength(winnersList.length() - 2); // Remove last comma
        return winnersList.toString();
    }

    /**
     * Logs messages to the console and/or a log file.
     */
    private void log(String logMessage) {
        if (logMessage.isEmpty()) return;
        if (ConfigValue.getBoolean(ConfigPaths.LOG_TO_CONSOLE)) {
            plugin.getLogger().info(logMessage);
        }
        if (ConfigValue.getBoolean(ConfigPaths.LOG_TO_FILE)) {
            // Disk work: off the main thread, except while disabling, when no new tasks can start
            if (plugin.isEnabled()) plugin.getScheduler().async(() -> writeLogToFile(logMessage));
            else writeLogToFile(logMessage);
        }
    }

    /**
     * Appends a line to the log file. Synchronized so lines written from two threads never mix.
     */
    private synchronized void writeLogToFile(String logMessage) {
        File logFile = plugin.getLogUtils().getLogFile();
        // UTF-8, so answers and names with accents read back the same on any server
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8))) {
            writer.write(logMessage);
            writer.newLine();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write log to file: " + e.getMessage());
        }
    }
}
