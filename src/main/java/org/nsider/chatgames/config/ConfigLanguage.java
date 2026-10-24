package org.nsider.chatgames.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Puts the config files (messages, menus, word lists...) in the server's language.
 *
 * <p>Each language's copy of the default files lives in the mine-translator repository, under
 * {@code Configs/<FORMAT>/<code>/}, with a {@code version.json} that lists the languages, which
 * {@code lang} codes each one covers, and a version per language. English is the copy in the jar.</p>
 *
 * <p>When {@code lang} (or {@code config-language}) asks for another language, or a newer version
 * of the current one is published, the files are downloaded in the background and merged into
 * the server's files on the global thread: every value the owner changed is kept, except a text
 * that the new language translates, which takes the translation when switching language. The
 * old files are copied to {@code backups/} first. The downloaded copy is kept in
 * {@code translations/configs/<code>/}: it is what the next switch compares against to tell
 * the owner's changes apart from the defaults.</p>
 */
public final class ConfigLanguage {

    /**
     * The config layout the translations are for. A release that moves or removes keys gets a new
     * folder, so older plugins keep downloading files they understand.
     */
    static final String FORMAT = "2.0";
    static final String ENGLISH = "en";
    /** The files that have translated copies. */
    static final String[] FILES = {"config.yml", "games.yml", "messages.yml", "menus.yml", "words.yml", "races.yml",
            "global_games.yml", "challenges.yml", "streaks.yml", "events.yml", "rewards.yml", "add-ons.yml",
            "presets.yml", "milestones.yml", "discord.yml"};
    // The property points the plugin at a mirror (or a local copy, for testing)
    private static final String BASE_URL = System.getProperty("chatgames.config-translations-url",
            "https://raw.githubusercontent.com/nsider4/mine-translator/main/Configs/");
    private static final int TIMEOUT_MILLIS = 10_000;

    private final ChatGames plugin;
    private final File folder;
    // Set while this class reloads the plugin itself, so that reload doesn't check again
    private volatile boolean applying;

    public ConfigLanguage(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "translations/configs");
    }

    /**
     * Checks in the background whether the config files should change language (or get a newer
     * translation) and, if so, downloads it and switches. Called on enable and on reload.
     */
    public void check() {
        if (applying) return;
        String setting = ConfigValue.getString(ConfigPaths.CONFIG_LANGUAGE);
        String mode = setting == null || setting.trim().isEmpty() ? "auto" : setting.trim().toLowerCase(Locale.ROOT);
        if (mode.equals("none")) return;
        String lang = String.valueOf(ConfigValue.getString(ConfigPaths.LANGUAGE)).trim().toLowerCase(Locale.ROOT);

        State state = State.read(new File(folder, "state.yml"));
        boolean firstRun = state == null;
        if (firstRun) {
            // Installs from before this feature have English files, whatever lang says; they
            // switch when lang changes, or when config-language names a language
            state = new State(ENGLISH, 0, lang, FORMAT);
            state.save(new File(folder, "state.yml"));
        }
        State current = state;

        plugin.getScheduler().async(() -> {
            Map<String, Language> languages;
            try {
                languages = languages();
            } catch (IOException e) {
                plugin.getLogger().log(Level.FINE, "Could not check config translations: " + e.getMessage());
                return;
            }
            boolean langChanged = !lang.equals(current.lang);
            String wanted = !mode.equals("auto") ? mode
                    : langChanged ? codeFor(lang, languages) : current.language;

            if (wanted == null) {
                plugin.getLogger().log(Level.INFO, "There are no translated config files for {0} yet; they stay in {1}.",
                        new Object[]{lang, current.language});
                current.lang = lang;
                current.save(new File(folder, "state.yml"));
                return;
            }
            Language language = languages.get(wanted);
            if (language == null && !wanted.equals(ENGLISH)) {
                plugin.getLogger().log(Level.WARNING, "config-language ''{0}'' has no translated files. Available: {1}",
                        new Object[]{wanted, String.join(", ", languages.keySet())});
                return;
            }
            if (firstRun && mode.equals("auto")) {
                String suggested = codeFor(lang, languages);
                if (suggested != null && !suggested.equals(ENGLISH)) {
                    plugin.getLogger().log(Level.INFO, "The config files can be in {0}: set config-language to ''{1}'' in config.yml, then /chatgames reload.",
                            new Object[]{languages.get(suggested).name, suggested});
                }
                return;
            }
            int version = wanted.equals(ENGLISH) || language == null ? 0 : language.version;
            boolean switching = !wanted.equals(current.language);
            // Files from an older layout's folder are updated whatever their version number says
            boolean newFormat = !FORMAT.equals(current.format) && !wanted.equals(ENGLISH);
            if (!switching && !newFormat && version <= current.version) {
                if (langChanged) {
                    current.lang = lang;
                    current.save(new File(folder, "state.yml"));
                }
                return;
            }

            File download = new File(folder, wanted + ".download");
            if (!wanted.equals(ENGLISH)) {
                try {
                    download(wanted, download);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Could not download the {0} config files; they stay as they are. ({1})",
                            new Object[]{wanted, e.getMessage()});
                    return;
                }
            }
            String name = language == null ? "English" : language.name;
            plugin.getScheduler().global(() -> apply(current, wanted, name, version, lang, switching, download));
        });
    }

    /** Merges the new language into the server's files, then reloads. Global thread. */
    private void apply(State state, String code, String name, int version, String lang, boolean switching, File download) {
        try {
            File data = plugin.getDataFolder();
            Map<String, String> merged = new LinkedHashMap<>();
            int replaced = 0;
            for (String file : FILES) {
                File target = new File(data, file);
                if (!target.exists()) continue;
                String next = code.equals(ENGLISH) ? jar(file) : text(new File(download, file));
                String previous = previousDefault(state.language, file);
                Merge merge = merge(text(target), previous, jar(file), next, switching);
                replaced += merge.replaced;
                if (!merge.text.equals(text(target))) merged.put(file, merge.text);
            }

            File backup = null;
            if (!merged.isEmpty()) {
                backup = new File(data, "backups/config-language-" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()));
                if (!backup.mkdirs() && !backup.isDirectory()) throw new IOException("Could not create " + backup);
                for (String file : merged.keySet()) {
                    Files.copy(new File(data, file).toPath(), new File(backup, file).toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                for (Map.Entry<String, String> entry : merged.entrySet()) {
                    Files.write(new File(data, entry.getKey()).toPath(), entry.getValue().getBytes(StandardCharsets.UTF_8));
                }
            }

            // The new language's defaults are what the next switch compares the files with
            File cache = new File(folder, code);
            deleteFolder(cache);
            if (!code.equals(ENGLISH) && !download.renameTo(cache)) throw new IOException("Could not keep " + download);
            new State(code, version, lang, FORMAT).save(new File(folder, "state.yml"));

            if (switching) {
                plugin.getLogger().log(Level.INFO, "The config files are now in {0}. Your settings were kept{1}{2}",
                        new Object[]{name, replaced > 0 ? "; " + replaced + " of your own texts were replaced by the translation" : "",
                                backup == null ? "." : ". The old files are in " + backup.getPath() + "."});
            } else {
                plugin.getLogger().log(Level.INFO, "Updated the {0} config files to version {1}{2}",
                        new Object[]{name, version, backup == null ? "." : ". The old files are in " + backup.getPath() + "."});
            }
            if (!merged.isEmpty()) {
                applying = true;
                try {
                    plugin.reloadAll();
                } finally {
                    applying = false;
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not switch the config files to " + name + "; they were left as they were.", e);
        } finally {
            deleteFolder(download);
        }
    }

    /** The result of merging one file. */
    static final class Merge {
        final String text;
        /** Texts the owner had changed that the new language's translation replaced. */
        final int replaced;

        Merge(String text, int replaced) {
            this.text = text;
            this.replaced = replaced;
        }
    }

    /**
     * Brings the owner's file to a new set of defaults (another language, or a newer translation).
     *
     * <p>Every value the owner changed from {@code previousDefault} is kept, keys they added are
     * kept and keys they removed stay removed. The exception is a value that both the owner and
     * the new defaults changed: when {@code switching} language it is a text the owner wrote in
     * the old language, and the translation wins; for an update of the same language the
     * owner's text wins.</p>
     *
     * @param current         the owner's file
     * @param previousDefault the defaults the owner's file started from
     * @param pluginDefault   the English defaults in the jar, for keys {@code previousDefault} doesn't
     *                        have: sections a plugin update added to the owner's file in English
     * @param newDefault      the new defaults, whose text (and comments) the result is built on
     */
    @Nonnull
    static Merge merge(@Nonnull String current, @Nonnull String previousDefault, @Nonnull String pluginDefault,
                       @Nonnull String newDefault, boolean switching) throws IOException {
        YamlConfiguration owner = YamlText.parse(current, "file");
        Map<String, Object> mine = YamlText.values(owner);
        Map<String, Object> before = YamlText.values(YamlText.parse(previousDefault, "default"));
        for (Map.Entry<String, Object> entry : YamlText.values(YamlText.parse(pluginDefault, "plugin default")).entrySet()) {
            if (mine.containsKey(entry.getKey())) before.putIfAbsent(entry.getKey(), entry.getValue());
        }
        Map<String, Object> after = YamlText.values(YamlText.parse(newDefault, "new default"));
        YamlConfiguration merged = YamlText.parse(newDefault, "new default");

        int replaced = 0;
        for (Map.Entry<String, Object> entry : mine.entrySet()) {
            String path = entry.getKey();
            Object value = entry.getValue();
            if (!before.containsKey(path)) {
                merged.set(path, owner.get(path));
            } else if (Objects.equals(value, before.get(path)) || !after.containsKey(path)) {
                continue;
            } else if (Objects.equals(before.get(path), after.get(path)) || !switching) {
                merged.set(path, owner.get(path));
            } else {
                replaced++;
            }
        }
        for (String path : before.keySet()) {
            if (!mine.containsKey(path) && !owner.isConfigurationSection(path)) merged.set(path, null);
        }
        removeEmptySections(merged, owner);
        return new Merge(YamlText.write(newDefault, merged), replaced);
    }

    /** Sections left empty by keys the owner removed (a trivia question, a challenge...). */
    private static void removeEmptySections(YamlConfiguration merged, YamlConfiguration owner) {
        boolean removed = true;
        while (removed) {
            removed = false;
            for (String path : new ArrayList<>(merged.getKeys(true))) {
                if (merged.isConfigurationSection(path) && merged.getConfigurationSection(path).getKeys(false).isEmpty()
                        && !owner.isConfigurationSection(path)) {
                    merged.set(path, null);
                    removed = true;
                }
            }
        }
    }

    /**
     * @return the language whose files cover a {@code lang} code: one that lists it, else one
     *         named like its start ("es" for "es_ar"), else null
     */
    @Nullable
    static String codeFor(@Nonnull String lang, @Nonnull Map<String, Language> languages) {
        for (Map.Entry<String, Language> entry : languages.entrySet()) {
            if (entry.getValue().lang.contains(lang)) return entry.getKey();
        }
        String start = lang.split("_", 2)[0];
        if (languages.containsKey(start) || start.equals(ENGLISH)) return start;
        return null;
    }

    private String previousDefault(String language, String file) throws IOException {
        File cached = new File(folder, language + "/" + file);
        // English is the jar; a missing copy (deleted by hand) is treated as English too
        return !language.equals(ENGLISH) && cached.exists() ? text(cached) : jar(file);
    }

    private String jar(String file) throws IOException {
        return new String(YamlText.read(plugin::getResource, file), StandardCharsets.UTF_8);
    }

    private static String text(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    /** One entry of version.json. */
    static final class Language {
        final String name;
        final List<String> lang;
        final int version;

        Language(String name, List<String> lang, int version) {
            this.name = name;
            this.lang = lang;
            this.version = version;
        }
    }

    @SuppressWarnings("deprecation") // the instance parser is the only one old Gson (1.8 servers) has
    private Map<String, Language> languages() throws IOException {
        try (Reader reader = new InputStreamReader(open(BASE_URL + FORMAT + "/version.json"), StandardCharsets.UTF_8)) {
            return parseLanguages(new JsonParser().parse(reader).getAsJsonObject());
        } catch (RuntimeException e) {
            throw new IOException("version.json isn't valid", e);
        }
    }

    @Nonnull
    static Map<String, Language> parseLanguages(@Nonnull JsonObject version) {
        Map<String, Language> languages = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : version.getAsJsonObject("languages").entrySet()) {
            JsonObject language = entry.getValue().getAsJsonObject();
            List<String> codes = new ArrayList<>();
            for (JsonElement code : language.getAsJsonArray("lang")) codes.add(code.getAsString().toLowerCase(Locale.ROOT));
            JsonElement number = language.get("version");
            languages.put(entry.getKey(), new Language(language.get("name").getAsString(), codes,
                    number == null ? 0 : number.getAsInt()));
        }
        return languages;
    }

    /** Downloads every file of a language; any failure leaves nothing behind. */
    private void download(String code, File target) throws IOException {
        deleteFolder(target);
        if (!target.mkdirs()) throw new IOException("Could not create " + target);
        try {
            for (String file : FILES) {
                try (InputStream in = open(BASE_URL + FORMAT + "/" + code + "/" + file)) {
                    byte[] content = YamlText.readAll(in);
                    YamlText.parse(new String(content, StandardCharsets.UTF_8), code + "/" + file);
                    Files.write(new File(target, file).toPath(), content);
                }
            }
        } catch (IOException e) {
            deleteFolder(target);
            throw e;
        }
    }

    private static InputStream open(String url) throws IOException {
        if (url.startsWith("file:")) return new BufferedInputStream(new FileInputStream(new File(url.substring(5))));
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        return new BufferedInputStream(connection.getInputStream());
    }

    private static void deleteFolder(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.delete()) file.deleteOnExit();
            }
        }
        if (dir.exists() && !dir.delete()) dir.deleteOnExit();
    }

    /**
     * translations/configs/state.yml: the language the files are in, its version, the lang it was
     * picked for and the layout (FORMAT) of the files it came from.
     */
    static final class State {
        final String language;
        final int version;
        String lang;
        final String format;

        State(String language, int version, String lang, String format) {
            this.language = language;
            this.version = version;
            this.lang = lang;
            this.format = format;
        }

        @Nullable
        static State read(File file) {
            if (!file.exists()) return null;
            YamlConfiguration yaml = YamlText.load(file);
            if (yaml == null || !yaml.isString("language")) return null;
            // The first format didn't record itself
            return new State(yaml.getString("language"), yaml.getInt("version"), yaml.getString("lang", ""),
                    yaml.getString("format", "2.0"));
        }

        void save(File file) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("language", language);
            yaml.set("version", version);
            yaml.set("lang", lang);
            yaml.set("format", format);
            try {
                if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) throw new IOException("no folder");
                Files.write(file.toPath(), ("# Which language the config files are in. Delete this file to start over (as English).\n"
                        + yaml.saveToString()).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                // Only means the next start checks again
            }
        }
    }
}
