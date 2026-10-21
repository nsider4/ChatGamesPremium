package org.nsider.chatgames.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.nsider.chatgames.ChatGames;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * Translates block, item and mob names into the server's language (the {@code lang} setting).
 *
 * <p>Translation files come from the mine-translator repository and are cached in
 * {@code plugins/ChatGames-Premium/translations}. Each file names the Minecraft version it
 * was built from; on load the repository's {@code version.json} is checked in the background
 * and a missing or older file is downloaded again. A slow or offline network never delays
 * startup: the cached file (or the English names built from the enum) is used meanwhile.</p>
 */
public class LanguageManager {

    // The property points the plugin at a mirror (or a local copy, for testing)
    private static final String BASE_URL = System.getProperty("chatgames.translations-url",
            "https://raw.githubusercontent.com/nsider4/mine-translator/main/Languages/");
    /** The key scripts/build-translations.py adds to every file. */
    static final String VERSION_KEY = "mine-translator.version";
    private static final int TIMEOUT_MILLIS = 10_000;

    private final ChatGames plugin;
    // Swapped in from the download thread, read from game threads
    private volatile JsonObject languageData;
    // A download for a language that was replaced meanwhile (reload) is ignored
    private volatile String currentFile;

    public LanguageManager(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads the translation file for a language, then checks for a newer one in the background.
     *
     * @param lang the language code, e.g. "en_us" or "es_es"
     */
    public void load(@Nonnull String lang) {
        String filename = lang.toLowerCase(java.util.Locale.ROOT) + ".json";
        File languageFile = new File(plugin.getDataFolder(), "translations/" + filename);
        currentFile = filename;

        JsonObject cached = languageFile.exists() ? read(languageFile) : null;
        if (cached != null) use(filename, cached);
        String localVersion = cached == null ? null : versionOf(cached);

        plugin.getScheduler().async(() -> {
            try {
                if (cached != null) {
                    String latest = latestVersion();
                    if (!needsUpdate(localVersion, latest)) return;
                    plugin.getLogger().log(Level.INFO, "Updating language file {0} ({1} -> {2}).",
                            new Object[]{filename, localVersion == null ? "old" : localVersion, latest});
                }
                downloadLanguageFile(filename, languageFile);
                JsonObject downloaded = read(languageFile);
                if (downloaded != null) use(filename, downloaded);
            } catch (IOException e) {
                if (cached == null) {
                    plugin.getLogger().log(Level.WARNING, "Could not download language file {0}; using English names. ({1})",
                            new Object[]{filename, e.getMessage()});
                }
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
     * @param local  the version the cached file was built from, null for files from before versions were added
     * @param latest the version the repository has, null if unknown (offline, or no version.json)
     * @return whether the cached file should be downloaded again
     */
    static boolean needsUpdate(@Nullable String local, @Nullable String latest) {
        return latest != null && !latest.equals(local);
    }

    @Nullable
    static String versionOf(@Nonnull JsonObject data) {
        JsonElement version = data.get(VERSION_KEY);
        return version != null && version.isJsonPrimitive() ? version.getAsString() : null;
    }

    private void use(@Nonnull String filename, @Nonnull JsonObject data) {
        if (!filename.equals(currentFile)) return;
        languageData = data;
        plugin.getLogger().log(Level.INFO, "Language file {0} loaded.", filename);
    }

    /** The version named by the repository's version.json. */
    @Nullable
    @SuppressWarnings("deprecation") // the instance parser is the only one old Gson (1.8 servers) has
    private String latestVersion() throws IOException {
        try (Reader reader = new InputStreamReader(open(BASE_URL + "version.json"), StandardCharsets.UTF_8)) {
            JsonElement version = new JsonParser().parse(reader).getAsJsonObject().get("version");
            return version != null && version.isJsonPrimitive() ? version.getAsString() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static InputStream open(@Nonnull String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        return new BufferedInputStream(connection.getInputStream());
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
        try (InputStream in = open(BASE_URL + filename);
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

    /** Parses a language file, or returns null (and logs) if it can't be read. */
    @Nullable
    @SuppressWarnings("deprecation") // the instance parser is the only one old Gson (1.8 servers) has
    private JsonObject read(@Nonnull File file) {
        try (Reader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            return new JsonParser().parse(reader).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading language file: " + file.getName(), e);
            return null;
        }
    }
}
