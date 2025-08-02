package org.nsider.chatgames.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import org.nsider.chatgames.ChatGames;
import org.nsider.inventorymanager.extras.Nonnull;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * Manages the loading and downloading of language translation files for the plugin.
 */
public class LanguageManager {

    private final ChatGames plugin;
    @Getter
    private JsonObject languageData;

    /**
     * Initializes the LanguageManager and loads the language data for the specified language.
     *
     * @param plugin The instance of the {@link ChatGames} plugin.
     * @param lang   The language code (e.g., "en", "es") for the desired language.
     */
    public LanguageManager(@Nonnull ChatGames plugin, @Nonnull String lang) {
        this.plugin = plugin;

        String filename = lang + ".json";
        File languageFile = new File(plugin.getDataFolder(), "translations/" + filename);

        //Ensure the language file exists, download if necessary
        if (!languageFile.exists()) {
            try {
                downloadLanguageFile(filename);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to download language file: " + filename, e);
                return;
            }
        }

        //Load language data
        loadLanguageFile(languageFile);
    }

    /**
     * Downloads a language file from the GitHub repository.
     *
     * @param filename The name of the language file to download.
     * @throws IOException If an error occurs during the download process.
     */
    private void downloadLanguageFile(@Nonnull String filename) throws IOException {
        String url = "https://raw.githubusercontent.com/nsider4/mine-translator/main/Languages/" + filename;
        File translationsDir = new File(plugin.getDataFolder(), "translations");

        if (!translationsDir.exists() && !translationsDir.mkdirs()) {
            throw new IOException("Failed to create translations directory: " + translationsDir.getAbsolutePath());
        }

        File file = new File(translationsDir, filename);

        try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream out = new FileOutputStream(file)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            plugin.getLogger().log(Level.INFO, "Language file {0} downloaded successfully!", filename);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error downloading language file: " + filename, e);
            throw e;
        }
    }

    /**
     * Loads the specified language file and parses its JSON content.
     *
     * @param file The language file to load.
     */
    private void loadLanguageFile(@Nonnull File file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {

            StringBuilder jsonString = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }

            JsonParser parser = new JsonParser();
            this.languageData = parser.parse(jsonString.toString()).getAsJsonObject();

            plugin.getLogger().log(Level.INFO, "Language file {0} loaded successfully!", file.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading language file: " + file.getName(), e);
        }
    }
}
