package org.nsider.chatgames.discord;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * One message layout from discord.yml:
 *
 * <pre>
 * content: 'text above the embed'
 * embed:
 *   color: '%game_color%'
 *   author: {name: '%game%', icon: '...', url: '...'}
 *   title: '...'
 *   url: '...'
 *   description: ['line', 'line']     # a list is joined into lines
 *   thumbnail: '%winner_head%'
 *   image: '...'
 *   fields:
 *     - {name: 'Time', value: '%time%s', inline: true}
 *   footer: {text: '...', icon: '...'}
 *   timestamp: true
 * </pre>
 *
 * Every text goes through the placeholders, then {@code :emoji:} names. Read once per
 * (re)load; rendering never touches the file.
 */
public final class DiscordTemplate {

    @Nullable private final String content;
    @Nullable private final String username;
    @Nullable private final String avatar;
    private final boolean hasEmbed;
    @Nullable private final String color;
    @Nullable private final String authorName;
    @Nullable private final String authorUrl;
    @Nullable private final String authorIcon;
    @Nullable private final String title;
    @Nullable private final String url;
    @Nullable private final String description;
    @Nullable private final String thumbnail;
    @Nullable private final String image;
    @Nullable private final String footerText;
    @Nullable private final String footerIcon;
    private final boolean timestamp;
    private final List<String[]> fields;

    private DiscordTemplate(ConfigurationSection section) {
        content = text(section, "content");
        username = section.getString("username");
        avatar = section.getString("avatar");
        ConfigurationSection embed = section.getConfigurationSection("embed");
        hasEmbed = embed != null;
        ConfigurationSection e = embed != null ? embed : new MemoryConfiguration();
        color = e.getString("color");
        authorName = part(e, "author", "name");
        authorUrl = part(e, "author", "url");
        authorIcon = part(e, "author", "icon");
        title = e.getString("title");
        url = e.getString("url");
        description = text(e, "description");
        thumbnail = e.getString("thumbnail");
        image = e.getString("image");
        footerText = part(e, "footer", "text");
        footerIcon = part(e, "footer", "icon");
        timestamp = e.getBoolean("timestamp", false);
        List<String[]> parsed = new ArrayList<>();
        for (Map<?, ?> field : e.getMapList("fields")) {
            Object name = field.get("name");
            Object value = field.get("value");
            Object inline = field.get("inline");
            parsed.add(new String[]{name == null ? "" : String.valueOf(name), joined(value),
                    String.valueOf(inline == null || Boolean.parseBoolean(String.valueOf(inline)))});
        }
        fields = Collections.unmodifiableList(parsed);
    }

    /** @return the layout in this section, or null if there is none (the notification is then skipped) */
    @Nullable
    public static DiscordTemplate read(@Nullable ConfigurationSection section) {
        if (section == null) return null;
        if (!section.isConfigurationSection("embed") && text(section, "content") == null) return null;
        return new DiscordTemplate(section);
    }

    /**
     * @param values  placeholders, keys with their % signs
     * @param extra   applied to each text after the placeholders (PlaceholderAPI), or null
     */
    @Nonnull
    public DiscordMessage render(@Nonnull Map<String, String> values, @Nullable UnaryOperator<String> extra) {
        DiscordMessage message = new DiscordMessage()
                .content(apply(content, values, extra))
                .username(plain(apply(username, values, extra)))
                .avatarUrl(apply(avatar, values, extra));
        if (!hasEmbed) return message;
        DiscordMessage.Embed embed = new DiscordMessage.Embed()
                .color(DiscordText.color(apply(color, values, extra)))
                .author(plain(apply(authorName, values, extra)), apply(authorUrl, values, extra), apply(authorIcon, values, extra))
                .title(apply(title, values, extra))
                .url(apply(url, values, extra))
                .description(apply(description, values, extra))
                .thumbnail(apply(thumbnail, values, extra))
                .image(apply(image, values, extra))
                .footer(plain(apply(footerText, values, extra)), apply(footerIcon, values, extra))
                .timestamp(timestamp ? Instant.now() : null);
        for (String[] field : fields) {
            embed.field(apply(field[0], values, extra), apply(field[1], values, extra), Boolean.parseBoolean(field[2]));
        }
        return message.embed(embed);
    }

    @Nullable
    private static String apply(@Nullable String text, Map<String, String> values, @Nullable UnaryOperator<String> extra) {
        if (text == null) return null;
        String filled = DiscordText.fill(text, values);
        if (extra != null && filled.indexOf('%') >= 0) filled = extra.apply(filled);
        // Lines left empty by a placeholder with nothing in it (no other winners, say) are dropped
        return DiscordText.emoji(dropEmptyLines(filled, text));
    }

    /** Author names, footers and usernames show Markdown as typed, so the escapes come off there. */
    @Nullable
    private static String plain(@Nullable String text) {
        return text == null ? null : DiscordText.unescape(text);
    }

    /** Removes lines that had text in the template but became empty, and trims what is left. */
    private static String dropEmptyLines(String filled, String template) {
        if (filled.indexOf('\n') < 0) return filled.trim().isEmpty() ? "" : filled;
        String[] before = template.split("\n", -1);
        String[] after = filled.split("\n", -1);
        if (before.length != after.length) return filled.trim();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < after.length; i++) {
            if (after[i].trim().isEmpty() && !before[i].trim().isEmpty()) continue;
            if (out.length() > 0) out.append('\n');
            out.append(after[i]);
        }
        return out.toString().trim();
    }

    /** A text written as one string or as a list of lines. */
    @Nullable
    private static String text(ConfigurationSection section, String key) {
        if (section.isList(key)) return String.join("\n", section.getStringList(key));
        return section.getString(key);
    }

    @Nullable
    private static String part(ConfigurationSection section, String key, String part) {
        // "author: 'Name'" is the same as "author: {name: 'Name'}"
        if (section.isString(key)) return part.equals("name") || part.equals("text") ? section.getString(key) : null;
        return section.getString(key + "." + part);
    }

    private static String joined(@Nullable Object value) {
        if (value == null) return "";
        if (value instanceof List) {
            List<String> lines = new ArrayList<>();
            for (Object line : (List<?>) value) lines.add(String.valueOf(line));
            return String.join("\n", lines);
        }
        return String.valueOf(value);
    }
}
