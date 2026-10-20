package org.nsider.chatgames.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.events.ChatGamesEndEvent;
import org.nsider.chatgames.events.ChatGamesStartEvent;
import org.nsider.chatgames.games.utils.PlayerResult;
import org.nsider.chatgames.races.Race;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

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
        Race race = plugin.getRaceBuilder().getRace();
        if (race == null) return "";

        return gameType.isMaterialRace()
                ? String.format("Race Starting: %s | Material: %s | Amount: %d",
                gameType.name(), race.getMaterial().name(), race.getAmount())
                : String.format("Race Starting: %s | Entity: %s | Amount: %d",
                gameType.name(), race.getEntityType().getEntityType().name(), race.getAmount());
    }

    /**
     * Formats a log message for race end events.
     */
    private String formatRaceEndLog(ChatGamesEndEvent event) {
        Race race = plugin.getRaceBuilder().getRace();
        if (race == null) return "";

        return event.getType().isMaterialRace()
                ? String.format("Race Ending: %s | Has Winner: %s%s | Material: %s | Amount: %d",
                event.getType().name(), event.hasWinner() ? "Yes" : "No",
                event.hasWinner() ? " | Winners: " + formatWinners(event.getWinners()) : "",
                race.getMaterial().name(), race.getAmount())
                : String.format("Race Ending: %s | Has Winner: %s%s | Entity: %s | Amount: %d",
                event.getType().name(), event.hasWinner() ? "Yes" : "No",
                event.hasWinner() ? " | Winners: " + formatWinners(event.getWinners()) : "",
                race.getEntityType().getEntityType().name(), race.getAmount());
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
        if (ConfigValue.getBoolean(ConfigPaths.LOG_TO_CONSOLE)) {
            plugin.getLogger().info(logMessage);
        }
        if (ConfigValue.getBoolean(ConfigPaths.LOG_TO_FILE)) {
            writeLogToFile(logMessage);
        }
    }

    /**
     * Writes logs to a local file.
     */
    private void writeLogToFile(String logMessage) {
        File logFile = plugin.getLogUtils().getLogFile();
        if (logFile == null) {
            Bukkit.getLogger().severe("Log file is not defined in the gamelog configuration.");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(logMessage);
            writer.newLine();
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to write log to file: " + e.getMessage());
        }
    }
}
