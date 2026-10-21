package org.nsider.chatgames.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nsider.chatgames.enums.GameType;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Turns a 1.9.9 data folder into a 2.0.0 one, before any file is loaded.
 *
 * <p>1.9.9 kept the game settings, Discord and logging in {@code config.yml}, the races in
 * {@code chatRaces.yml}, and called Memorize "shoppinglist". Only the values a server owner
 * changed from 1.9.9's own defaults (bundled under {@code legacy/1.9.9/}) are carried over, each to
 * its 2.0.0 file and path; everything else comes from the 2.0.0 defaults. That way a 2.0.0
 * default that was improved isn't replaced by the old one, and a file nobody changed is written
 * exactly as shipped, comments included. (Bukkit before 1.18 drops comments when it saves, so on
 * those servers a file with changes loses its comments.)</p>
 *
 * <p>Every old file is copied to {@code backups/1.9.9-<date>/} first.</p>
 */
public final class LegacyMigration {

    /** The 2.0.0 files a 1.9.9 folder is turned into. */
    static final String[] TARGETS = {"config.yml", "games.yml", "add-ons.yml", "races.yml", "messages.yml",
            "words.yml", "rewards.yml"};
    private static final String[] OLD_FILES = {"config.yml", "chatRaces.yml", "messages.yml", "words.yml", "rewards.yml"};
    private static final String[] ADDON_KEYS = {"discord", "log-file", "log-console", "log-local"};
    private static final String OLD_MEMORIZE = "shoppinglist";

    private LegacyMigration() {}

    /** What happened, for the log. */
    public static final class Result {
        /** Changed values carried over, per 2.0.0 file (rewards.yml isn't counted). */
        public final Map<String, Integer> carried = new LinkedHashMap<>();
        /** Whether rewards.yml had changes and was kept whole. */
        public boolean rewardsKept;
        public File backup;
    }

    /**
     * @return whether the folder holds a 1.9.9 setup: 2.0.0 never has {@code chatRaces.yml}, nor
     *         game settings in config.yml, and always writes games.yml
     */
    public static boolean isNeeded(@Nonnull File folder) {
        if (new File(folder, "games.yml").exists() || !new File(folder, "config.yml").exists()) return false;
        if (new File(folder, "chatRaces.yml").exists()) return true;
        YamlConfiguration config = load(new File(folder, "config.yml"));
        if (config == null) return false;
        for (String game : oldGameKeys()) {
            if (config.isConfigurationSection(game)) return true;
        }
        return false;
    }

    /**
     * Rewrites a 1.9.9 folder. Call only when {@link #isNeeded} is true.
     *
     * @param resources opens a file shipped in the jar ("games.yml", "legacy/1.9.9/config.yml"...)
     */
    @Nonnull
    public static Result migrate(@Nonnull File folder, @Nonnull Function<String, InputStream> resources) throws IOException {
        Result result = new Result();
        result.backup = backup(folder);

        Map<String, YamlConfiguration> changes = new LinkedHashMap<>();
        for (String target : TARGETS) changes.put(target, new YamlConfiguration());

        // config.yml: game sections → games.yml, Discord and logs → add-ons.yml, the rest stays
        for (Map.Entry<String, Object> change : changed(folder, "config.yml", resources).entrySet()) {
            String path = change.getKey();
            String root = path.split("\\.", 2)[0];
            String target = "config.yml";
            if (isOldGameKey(root)) {
                target = "games.yml";
                path = renameMemorize(path);
            } else if (contains(ADDON_KEYS, root)) {
                target = "add-ons.yml";
            }
            changes.get(target).set(path, change.getValue());
        }
        for (Map.Entry<String, Object> change : changed(folder, "chatRaces.yml", resources).entrySet()) {
            changes.get("races.yml").set(change.getKey(), change.getValue());
        }
        for (Map.Entry<String, Object> change : changed(folder, "messages.yml", resources).entrySet()) {
            changes.get("messages.yml").set(renameMemorize(change.getKey()), change.getValue());
        }
        for (Map.Entry<String, Object> change : changed(folder, "words.yml", resources).entrySet()) {
            String path = change.getKey().equals(OLD_MEMORIZE + ".items") ? "memorize.pools.shopping_list"
                    : renameMemorize(change.getKey());
            changes.get("words.yml").set(path, change.getValue());
        }

        for (String target : TARGETS) {
            if (target.equals("rewards.yml")) continue;
            YamlConfiguration changed = changes.get(target);
            List<String> paths = leaves(changed);
            result.carried.put(target, paths.size());
            File file = new File(folder, target);
            if (paths.isEmpty()) {
                write(file, read(resources, target));
                continue;
            }
            String shipped = new String(read(resources, target), StandardCharsets.UTF_8);
            YamlConfiguration merged = parse(shipped, target);
            for (String path : paths) merged.set(path, changed.get(path));

            // Bukkit before 1.18 drops every comment when it saves, so first try writing the
            // values into the shipped text, and use that only if it reads back as exactly the
            // merged configuration
            String patched = shipped;
            for (String path : paths) {
                patched = patched == null ? null : patch(patched, path, changed.get(path));
            }
            if (patched != null && values(parse(patched, target)).equals(values(merged))) {
                write(file, patched.getBytes(StandardCharsets.UTF_8));
            } else {
                write(file, merged.saveToString().getBytes(StandardCharsets.UTF_8));
            }
        }

        migrateRewards(folder, resources, result);
        Files.deleteIfExists(new File(folder, "chatRaces.yml").toPath());
        return result;
    }

    /**
     * Rewards are lists the owner builds (places, reward ids), so an edited file is kept whole:
     * shoppinglist becomes memorize, and only games it has no section for get the 2.0.0 defaults.
     */
    private static void migrateRewards(File folder, Function<String, InputStream> resources, Result result) throws IOException {
        File file = new File(folder, "rewards.yml");
        YamlConfiguration old = file.exists() ? load(file) : null;
        if (file.exists() && old == null) throw new IOException("rewards.yml isn't valid YAML; fix it or remove it, then restart");
        YamlConfiguration stock = parse(new String(read(resources, "legacy/1.9.9/rewards.yml"), StandardCharsets.UTF_8), "rewards.yml");
        if (old == null || values(old).equals(values(stock))) {
            write(file, read(resources, "rewards.yml"));
            return;
        }
        YamlConfiguration defaults = parse(new String(read(resources, "rewards.yml"), StandardCharsets.UTF_8), "rewards.yml");
        YamlConfiguration merged = new YamlConfiguration();
        for (String key : old.getKeys(false)) {
            merged.set(key.equals(OLD_MEMORIZE) && !old.contains("memorize") ? "memorize" : key, old.get(key));
        }
        for (String key : defaults.getKeys(false)) {
            // The 2.0.0 file still has a shoppinglist section for configs that were never upgraded
            if (!merged.contains(key) && !key.equals(OLD_MEMORIZE)) merged.set(key, defaults.get(key));
        }
        result.rewardsKept = true;
        write(file, merged.saveToString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Replaces one value in YAML text, keeping every other line (and so every comment) as it is.
     * Handles a scalar on the key's line, or a list written as "- item" lines under the key.
     *
     * @return the new text, or null if the path or its layout isn't one of those
     */
    @Nullable
    static String patch(String text, String path, Object value) {
        List<String> lines = new ArrayList<>(Arrays.asList(text.split("\n", -1)));
        List<Integer> indents = new ArrayList<>(); // of each key the current line is under
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("- ") || trimmed.equals("-")) continue;
            int colon = keyEnd(trimmed);
            if (colon < 0) continue;
            int indent = line.indexOf(trimmed);
            while (!indents.isEmpty() && indents.get(indents.size() - 1) >= indent) {
                indents.remove(indents.size() - 1);
                keys.remove(keys.size() - 1);
            }
            String key = unquote(trimmed.substring(0, colon));
            indents.add(indent);
            keys.add(key);
            if (!String.join(".", keys).equals(path)) continue;

            String rest = trimmed.substring(colon + 1).trim();
            if (rest.startsWith("|") || rest.startsWith(">")) return null;
            String prefix = line.substring(0, indent) + trimmed.substring(0, colon) + ":";

            // The "- item" lines under the key, if its value is a list written that way
            int end = i + 1;
            String itemIndent = null;
            if (rest.isEmpty()) {
                while (end < lines.size()) {
                    String next = lines.get(end);
                    String nextTrimmed = next.trim();
                    if (nextTrimmed.startsWith("- ") || nextTrimmed.equals("-")) {
                        if (itemIndent == null) itemIndent = next.substring(0, next.indexOf(nextTrimmed));
                        end++;
                    } else if (nextTrimmed.isEmpty() || nextTrimmed.startsWith("#")) {
                        end++;
                    } else {
                        break;
                    }
                }
                // Keep blank lines and comments that follow the list where they were
                while (end > i + 1 && (lines.get(end - 1).trim().isEmpty() || lines.get(end - 1).trim().startsWith("#"))) end--;
                // Nothing but a nested section under the key: not a value
                if (itemIndent == null) return null;
            }

            List<String> replacement = new ArrayList<>();
            if (value instanceof List) {
                if (itemIndent == null) itemIndent = line.substring(0, indent) + "  ";
                for (Object item : (List<?>) value) {
                    String scalar = scalar(item);
                    if (scalar == null) return null;
                    replacement.add(itemIndent + "- " + scalar);
                }
                replacement.add(0, prefix + (replacement.isEmpty() ? " []" : ""));
            } else {
                String scalar = scalar(value);
                if (scalar == null) return null;
                replacement.add(prefix + " " + scalar);
            }
            lines.subList(i, end).clear();
            lines.addAll(i, replacement);
            return String.join("\n", lines);
        }
        return null;
    }

    /** Where a "key:" ends in a trimmed line, or -1 if the line isn't a key. */
    private static int keyEnd(String trimmed) {
        if (trimmed.startsWith("'") || trimmed.startsWith("\"")) {
            int close = trimmed.indexOf(trimmed.charAt(0), 1);
            return close > 0 && trimmed.startsWith(":", close + 1) ? close + 1 : -1;
        }
        int colon = trimmed.indexOf(':');
        while (colon >= 0 && colon + 1 < trimmed.length() && trimmed.charAt(colon + 1) != ' ') colon = trimmed.indexOf(':', colon + 1);
        return colon;
    }

    private static String unquote(String key) {
        return key.length() >= 2 && (key.startsWith("'") || key.startsWith("\"")) ? key.substring(1, key.length() - 1) : key;
    }

    /** A value as one YAML token, or null for anything that needs more than one line. */
    @Nullable
    private static String scalar(Object value) {
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof String && ((String) value).indexOf('\n') < 0) return "'" + ((String) value).replace("'", "''") + "'";
        return null;
    }

    /** The values in an old file that differ from 1.9.9's default for the same path. */
    private static Map<String, Object> changed(File folder, String name, Function<String, InputStream> resources) throws IOException {
        Map<String, Object> changed = new LinkedHashMap<>();
        File file = new File(folder, name);
        if (!file.exists()) return changed;
        YamlConfiguration old = load(file);
        if (old == null) throw new IOException(name + " isn't valid YAML; fix it or remove it, then restart");
        YamlConfiguration stock = parse(new String(read(resources, "legacy/1.9.9/" + name), StandardCharsets.UTF_8), name);
        for (String path : leaves(old)) {
            Object value = old.get(path);
            if (!stock.contains(path) || !Objects.equals(plain(value), plain(stock.get(path)))) changed.put(path, value);
        }
        return changed;
    }

    private static File backup(File folder) throws IOException {
        File backup = new File(folder, "backups/1.9.9-" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()));
        if (!backup.mkdirs() && !backup.isDirectory()) throw new IOException("Could not create " + backup);
        for (String name : OLD_FILES) {
            File file = new File(folder, name);
            if (file.exists()) Files.copy(file.toPath(), new File(backup, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return backup;
    }

    /** Every non-section path, so a value is compared and moved on its own. */
    private static List<String> leaves(ConfigurationSection section) {
        List<String> paths = new ArrayList<>();
        for (String path : section.getKeys(true)) {
            if (!section.isConfigurationSection(path)) paths.add(path);
        }
        return paths;
    }

    private static Map<String, Object> values(ConfigurationSection section) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String path : leaves(section)) values.put(path, plain(section.get(path)));
        return values;
    }

    /** Compares numbers by value: YAML reads 5 and 5.0 as different types. */
    private static Object plain(Object value) {
        return value instanceof Number && !(value instanceof Double) ? ((Number) value).doubleValue() : value;
    }

    private static boolean isOldGameKey(String key) {
        return contains(oldGameKeys(), key);
    }

    private static String[] oldGameKeys() {
        List<String> keys = new ArrayList<>();
        for (GameType type : GameType.values()) {
            if (!type.isRace()) keys.add(type.getConfigKey());
        }
        keys.add(OLD_MEMORIZE);
        return keys.toArray(new String[0]);
    }

    private static String renameMemorize(String path) {
        return path.equals(OLD_MEMORIZE) || path.startsWith(OLD_MEMORIZE + ".") ? "memorize" + path.substring(OLD_MEMORIZE.length()) : path;
    }

    private static boolean contains(String[] values, String value) {
        for (String candidate : values) {
            if (candidate.equals(value)) return true;
        }
        return false;
    }

    @Nullable
    private static YamlConfiguration load(File file) {
        try {
            return parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), file.getName());
        } catch (IOException e) {
            return null;
        }
    }

    private static YamlConfiguration parse(String text, String name) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IOException(name + " isn't valid YAML", e);
        }
        return yaml;
    }

    private static byte[] read(Function<String, InputStream> resources, String name) throws IOException {
        try (InputStream in = resources.apply(name)) {
            if (in == null) throw new IOException(name + " is missing from the jar");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return out.toByteArray();
        }
    }

    private static void write(File file, byte[] content) throws IOException {
        Files.write(file.toPath(), content);
    }
}
