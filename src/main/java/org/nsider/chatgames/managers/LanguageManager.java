package org.nsider.chatgames.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.nsider.chatgames.ChatGames;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * Translates block, item and mob names into the server's language (the {@code lang} setting).
 *
 * <p>Translation files come from the mine-translator repository and are cached in
 * {@code plugins/ChatGames-Premium/translations}. A missing file is downloaded in
 * the background, so a slow or offline network never delays startup; until it
 * arrives, names fall back to the English names built from the enum.</p>
 */
public class LanguageManager {

    private static final String BASE_URL = "https://raw.githubusercontent.com/nsider4/mine-translator/main/Languages/";

    private final ChatGames plugin;
    // Swapped in from the download thread, read from game threads
    private volatile JsonObject languageData;

    public LanguageManager(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads the translation file for a language, downloading it first if needed.
     *
     * @param lang the language code, e.g. "en_us" or "es_es"
     */
    public void load(@Nonnull String lang) {
        String filename = lang.toLowerCase(java.util.Locale.ROOT) + ".json";
        File languageFile = new File(plugin.getDataFolder(), "translations/" + filename);

        if (languageFile.exists()) {
            loadLanguageFile(languageFile);
            return;
        }

        plugin.getScheduler().async(() -> {
            try {
                downloadLanguageFile(filename, languageFile);
                loadLanguageFile(languageFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not download language file {0}; using English names. ({1})",
                        new Object[]{filename, e.getMessage()});
            }
        });
    }

    /**
     * @param key a Minecraft translation key, e.g. "block.minecraft.stone"
     * @return the translated text, or null if it isn't loaded or doesn't exist
     */
    @Nullable
    public String translate(@Nonnull String key) {
        JsonObject data = languageData;
        if (data == null) return null;
        JsonElement element = data.get(key);
        return element == null || !element.isJsonPrimitive() ? null : element.getAsString();
    }

    /**
     * Downloads a language file from the GitHub repository.
     *
     * @throws IOException If an error occurs during the download process.
     */
    private void downloadLanguageFile(@Nonnull String filename, @Nonnull File target) throws IOException {
        File translationsDir = target.getParentFile();
        if (!translationsDir.exists() && !translationsDir.mkdirs()) {
            throw new IOException("Failed to create translations directory: " + translationsDir.getAbsolutePath());
        }

        // Written to a temp file first so a failed download never leaves a broken file behind
        File temp = new File(translationsDir, filename + ".part");
        try (BufferedInputStream in = new BufferedInputStream(new URL(BASE_URL + filename).openStream());
             FileOutputStream out = new FileOutputStream(temp)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        if (target.exists() && !target.delete() || !temp.renameTo(target)) {
            throw new IOException("Could not move " + temp.getName() + " into place");
        }
        plugin.getLogger().log(Level.INFO, "Language file {0} downloaded.", filename);
    }

    /**
     * Loads the specified language file and parses its JSON content.
     */
    @SuppressWarnings("deprecation") // the instance parser is the only one old Gson (1.8 servers) has
    private void loadLanguageFile(@Nonnull File file) {
        try (Reader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {

            this.languageData = new JsonParser().parse(reader).getAsJsonObject();
            plugin.getLogger().log(Level.INFO, "Language file {0} loaded.", file.getName());
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading language file: " + file.getName(), e);
        }
    }
}
