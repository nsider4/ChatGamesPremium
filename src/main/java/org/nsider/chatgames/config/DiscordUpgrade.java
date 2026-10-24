package org.nsider.chatgames.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Moves the Discord settings of 2.0.0 (the {@code discord:} section of add-ons.yml, and events.yml's)
 * into discord.yml. The webhook, its name and its picture carry over; the old single-embed texts
 * don't fit the new messages, so they are dropped. Each file is edited as text, so comments stay.
 */
final class DiscordUpgrade {

    /** Old setting (under discord.) to where it lives in discord.yml. */
    static final Map<String, String> MOVED;

    static {
        Map<String, String> moved = new LinkedHashMap<>();
        moved.put("webhook-url", "channels.main.webhook");
        moved.put("username", "channels.main.username");
        moved.put("img-url", "channels.main.avatar");
        MOVED = Collections.unmodifiableMap(moved);
    }

    private static final String NOTE = "#Discord settings are in discord.yml now.";

    private DiscordUpgrade() {}

    /**
     * @return what was moved, for the console; empty if there was nothing to do
     */
    @Nonnull
    static List<String> run(@Nonnull File data, @Nonnull Function<String, InputStream> resources) throws IOException {
        File addons = new File(data, "add-ons.yml");
        File events = new File(data, "events.yml");
        YamlConfiguration oldAddons = addons.isFile() ? YamlText.load(addons) : null;
        YamlConfiguration oldEvents = events.isFile() ? YamlText.load(events) : null;
        boolean fromAddons = oldAddons != null && oldAddons.isConfigurationSection("discord");
        boolean fromEvents = oldEvents != null && oldEvents.isConfigurationSection("discord");
        if (!fromAddons && !fromEvents) return Collections.emptyList();

        File discord = new File(data, "discord.yml");
        String text = discord.isFile() ? read(discord) : new String(YamlText.read(resources, "discord.yml"), StandardCharsets.UTF_8);
        List<String> moved = new ArrayList<>();
        if (fromAddons) {
            for (Map.Entry<String, String> entry : MOVED.entrySet()) {
                String value = oldAddons.getString("discord." + entry.getKey());
                if (value == null || value.trim().isEmpty()) continue;
                String patched = YamlText.patch(text, entry.getValue(), value);
                if (patched == null) continue;
                text = patched;
                moved.add("discord." + entry.getKey());
            }
        }
        if (fromEvents && !oldEvents.getBoolean("discord.enabled", true)) {
            String patched = YamlText.patch(text, "events.enabled", false);
            if (patched != null) {
                text = patched;
                moved.add("events.yml discord.enabled");
            }
        }
        Files.write(discord.toPath(), text.getBytes(StandardCharsets.UTF_8));
        if (fromAddons) Files.write(addons.toPath(), removeSection(read(addons), "discord").getBytes(StandardCharsets.UTF_8));
        if (fromEvents) Files.write(events.toPath(), removeSection(read(events), "discord").getBytes(StandardCharsets.UTF_8));
        if (moved.isEmpty()) moved.add("nothing (no webhook was set)");
        return moved;
    }

    /** Replaces a top-level section and the comment lines right above it with a note. */
    static String removeSection(@Nonnull String text, @Nonnull String key) {
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        List<String> lines = new ArrayList<>(Arrays.asList(text.replace("\r\n", "\n").split("\n", -1)));
        int[] block = YamlText.block(lines, key);
        if (block == null) return text;
        int start = block[0];
        while (start > 0 && lines.get(start - 1).trim().startsWith("#")) start--;
        lines.subList(start, block[1]).clear();
        lines.add(start, NOTE);
        return String.join(newline, lines);
    }

    @Nullable
    static String mapLegacy(@Nonnull String path) {
        // "discord.webhook-url" from 1.9.9's config.yml
        if (!path.startsWith("discord.")) return null;
        return MOVED.get(path.substring("discord.".length()));
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
