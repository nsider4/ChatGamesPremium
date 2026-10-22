package org.nsider.chatgames.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Reading, comparing and editing config files as text, for the code that rewrites a server
 * owner's files (the 1.9.9 upgrade, switching the config language).
 *
 * <p>Bukkit before 1.18 drops every comment when it saves a file. {@link #patch} changes one
 * value in the text instead, so the comments stay on every version.</p>
 */
final class YamlText {

    private YamlText() {}

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

    /**
     * Writes {@code values} as a file: {@code shipped} with each value that differs patched in, if
     * that reads back as exactly {@code values}; otherwise {@code merged}'s own save (which keeps
     * comments only on 1.18+).
     *
     * @param merged the configuration holding {@code values}, loaded from {@code shipped}
     */
    @Nonnull
    static String write(@Nonnull String shipped, @Nonnull YamlConfiguration merged) {
        String patched = shipped;
        Map<String, Object> wanted = values(merged);
        try {
            Map<String, Object> current = values(parse(shipped, "file"));
            if (!current.keySet().equals(wanted.keySet())) return merged.saveToString();
            for (Map.Entry<String, Object> entry : wanted.entrySet()) {
                if (patched == null) break;
                if (!entry.getValue().equals(current.get(entry.getKey()))) {
                    patched = patch(patched, entry.getKey(), merged.get(entry.getKey()));
                }
            }
            if (patched != null && values(parse(patched, "file")).equals(wanted)) return patched;
        } catch (IOException e) {
            // falls through to Bukkit's save
        }
        return merged.saveToString();
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

    /** Every non-section path, so a value is compared and moved on its own. */
    static List<String> leaves(ConfigurationSection section) {
        List<String> paths = new ArrayList<>();
        for (String path : section.getKeys(true)) {
            if (!section.isConfigurationSection(path)) paths.add(path);
        }
        return paths;
    }

    static Map<String, Object> values(ConfigurationSection section) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String path : leaves(section)) values.put(path, plain(section.get(path)));
        return values;
    }

    /** Compares numbers by value: YAML reads 5 and 5.0 as different types. */
    static Object plain(Object value) {
        return value instanceof Number && !(value instanceof Double) ? ((Number) value).doubleValue() : value;
    }

    @Nullable
    static YamlConfiguration load(File file) {
        try {
            return parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), file.getName());
        } catch (IOException e) {
            return null;
        }
    }

    static YamlConfiguration parse(String text, String name) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IOException(name + " isn't valid YAML", e);
        }
        return yaml;
    }

    static byte[] read(Function<String, InputStream> resources, String name) throws IOException {
        try (InputStream in = resources.apply(name)) {
            if (in == null) throw new IOException(name + " is missing from the jar");
            return readAll(in);
        }
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}
