package org.nsider.chatgames.config;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Adds the sections that came after a server's config files were made (Tame and Shear, and
 * the /chatgames discord messages, added after 2.0.0), copied as text from the jar's files so
 * their comments come along.
 *
 * <p>Each section is offered once: {@code .added-sections} in the data folder lists the ones
 * already handled, so a section the owner deletes stays deleted. Translated files get the
 * English text; switching the config language again translates it.</p>
 */
final class BuiltInSections {

    /** "file:path" for every section added after 2.0.0. */
    static final List<String> SECTIONS = Arrays.asList(
            "races.yml:tame", "races.yml:shear",
            "messages.yml:tame", "messages.yml:shear",
            "global_games.yml:games.tame", "global_games.yml:games.shear",
            "menus.yml:game-names.tame", "menus.yml:game-names.shear",
            "menus.yml:game-icons.tame", "menus.yml:game-icons.shear",
            "messages.yml:discord");
    static final String RECORD = ".added-sections";

    private BuiltInSections() {}

    /**
     * Adds the missing sections. Runs before the files are loaded.
     *
     * @return the "file:path" entries added
     */
    @Nonnull
    static List<String> addMissing(@Nonnull File data, @Nonnull Function<String, InputStream> resources) throws IOException {
        File record = new File(data, RECORD);
        Set<String> handled = new LinkedHashSet<>();
        if (record.isFile()) {
            for (String line : Files.readAllLines(record.toPath(), StandardCharsets.UTF_8)) {
                if (!line.trim().isEmpty()) handled.add(line.trim());
            }
        }
        List<String> added = new ArrayList<>();
        boolean changed = false;
        for (String entry : SECTIONS) {
            if (handled.contains(entry)) continue;
            int colon = entry.indexOf(':');
            String name = entry.substring(0, colon);
            File file = new File(data, name);
            // A file made now comes from the jar, which already has the section
            if (file.isFile()) {
                String owner = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                String shipped = new String(YamlText.read(resources, name), StandardCharsets.UTF_8);
                String result = add(owner, shipped, entry.substring(colon + 1));
                if (result != null) {
                    Files.write(file.toPath(), result.getBytes(StandardCharsets.UTF_8));
                    added.add(entry);
                }
            }
            handled.add(entry);
            changed = true;
        }
        if (changed && data.isDirectory()) {
            Files.write(record.toPath(), String.join("\n", handled).concat("\n").getBytes(StandardCharsets.UTF_8));
        }
        return added;
    }

    /**
     * Copies one section from the shipped text into the owner's text: at the end for a top-level
     * key, otherwise after the last line of its parent.
     *
     * @return the new text, or null if the owner already has it, or its parent is gone
     */
    @Nullable
    static String add(@Nonnull String owner, @Nonnull String shipped, @Nonnull String path) {
        String newline = owner.contains("\r\n") ? "\r\n" : "\n";
        List<String> mine = new ArrayList<>(Arrays.asList(owner.replace("\r\n", "\n").split("\n", -1)));
        List<String> theirs = Arrays.asList(shipped.replace("\r\n", "\n").split("\n", -1));
        if (YamlText.block(mine, path) != null) return null;
        int[] source = YamlText.block(theirs, path);
        if (source == null) return null;
        List<String> section = new ArrayList<>(theirs.subList(source[0], source[1]));

        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            while (!mine.isEmpty() && mine.get(mine.size() - 1).trim().isEmpty()) mine.remove(mine.size() - 1);
            mine.add("");
            mine.addAll(section);
            mine.add("");
        } else {
            int[] parent = YamlText.block(mine, path.substring(0, dot));
            // A parent written inline ("games: {}") has no lines to add under
            if (parent == null || !mine.get(parent[0]).replaceAll("\\s+#.*$", "").trim().endsWith(":")) return null;
            mine.addAll(parent[1], section);
        }
        return String.join(newline, mine);
    }
}
