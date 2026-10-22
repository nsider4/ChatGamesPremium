package org.nsider.chatgames.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.nsider.chatgames.api.GameType;
import org.nsider.inventorymanager.extras.Nonnull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.nsider.chatgames.config.YamlText.leaves;
import static org.nsider.chatgames.config.YamlText.load;
import static org.nsider.chatgames.config.YamlText.parse;
import static org.nsider.chatgames.config.YamlText.plain;
import static org.nsider.chatgames.config.YamlText.read;
import static org.nsider.chatgames.config.YamlText.values;
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
            write(file, YamlText.write(shipped, merged).getBytes(StandardCharsets.UTF_8));
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

    private static void write(File file, byte[] content) throws IOException {
        Files.write(file.toPath(), content);
    }
}
