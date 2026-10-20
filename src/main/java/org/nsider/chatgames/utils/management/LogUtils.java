package org.nsider.chatgames.utils.management;

import org.bukkit.Bukkit;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.inventorymanager.extras.Nonnull;

import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Utility class for handling logging functionality within the plugin.
 * Provides methods for retrieving log files, reading specific logs,
 * and extracting details from log entries.
 */
public class LogUtils {

    /** The instance of the main plugin. */
    private final ChatGames plugin;

    /**
     * Constructs a new LogHandler instance.
     *
     * @param instance The plugin instance.
     */
    public LogUtils(@Nonnull ChatGames instance) {
        this.plugin = instance;
    }

    /**
     * Retrieves the log file used by the plugin, creating it if it does not exist.
     *
     * @return The log file.
     */
    @Nonnull
    public File getLogFile() {
        File logFile = new File(plugin.getDataFolder(), ConfigValue.getString(ConfigPaths.LOG_FILE_PATH));

        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().severe("Failed to create log file: " + e.getMessage());
            }
        }

        return logFile;
    }

    /**
     * Reads the last specified number of lines from the log file.
     *
     * @param amount The number of lines to retrieve.
     * @return A list of the last 'amount' log lines, or an empty list if an error occurs.
     */
    @Nonnull
    public List<String> getLastLogs(int amount) {
        File logFile = getLogFile();
        if (!logFile.exists()) {
            Bukkit.getLogger().severe("Log file does not exist.");
            return new ArrayList<>();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            Deque<String> lines = new ArrayDeque<>();
            String line;

            while ((line = reader.readLine()) != null) {
                if (lines.size() == amount) {
                    lines.pollFirst();
                }
                lines.addLast(line);
            }

            return new ArrayList<>(lines);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to read logs: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Identifies the type of log entry based on its content.
     *
     * @param log The log entry to analyze.
     * @return The type of the log (e.g., "Game Start", "Race End", etc.).
     */
    @Nonnull
    public String identifyLogType(@Nonnull String log) {
        if (log.startsWith("Game Starting")) {
            return "Game Start";
        } else if (log.startsWith("Game Ending")) {
            return "Game End";
        } else if (log.startsWith("Race Starting")) {
            return log.contains("Material:") ? "Race Start" : "Hunt Start";
        } else if (log.startsWith("Race Ending")) {
            return log.contains("Material:") ? "Race End" : "Hunt End";
        }

        return "Unknown";
    }

    /**
     * Extracts the list of winners from a log entry.
     *
     * @param log The log entry to analyze.
     * @return A comma-separated list of winners, or "None" if no winners are found.
     */
    @Nonnull
    public String extractWinnersFromLog(@Nonnull String log) {
        String winnersPrefix = "Winners:";
        int startIdx = log.indexOf(winnersPrefix);

        if (startIdx != -1) {
            int endIdx = log.indexOf(" |", startIdx);

            if (endIdx == -1) {
                endIdx = log.length();
            }

            return log.substring(startIdx + winnersPrefix.length(), endIdx).trim();
        }

        return "None";
    }

    /**
     * Extracts the value associated with a specific key from a log entry.
     * Log entries are expected to follow the format "key: value | key: value".
     *
     * @param log The log entry to analyze.
     * @param key The key whose value is to be extracted.
     * @return The value associated with the key, or "Unknown" if the key is not found.
     */
    @Nonnull
    public String extractValueFromLog(@Nonnull String log, @Nonnull String key) {
        String[] parts = log.split(" \\| ");
        for (String part : parts) {
            if (part.startsWith(key)) {
                String[] keyValue = part.split(": ", 2);
                if (keyValue.length > 1) {
                    return keyValue[1].trim();
                }
            }
        }

        return "Unknown";
    }
}
