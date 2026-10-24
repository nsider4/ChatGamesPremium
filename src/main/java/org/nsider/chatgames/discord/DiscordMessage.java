package org.nsider.chatgames.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * One Discord message: text and at most one embed, ready to send through a webhook or
 * DiscordSRV. Texts are cut to Discord's limits here, so a long answer or leaderboard never
 * gets the whole message rejected.
 */
public final class DiscordMessage {

    static final int CONTENT_LIMIT = 2000;
    static final int TITLE_LIMIT = 256;
    static final int DESCRIPTION_LIMIT = 4096;
    static final int FIELD_NAME_LIMIT = 256;
    static final int FIELD_VALUE_LIMIT = 1024;
    static final int FOOTER_LIMIT = 2048;
    static final int MAX_FIELDS = 25;
    /** Discord rejects an empty field name or value; this shows nothing. */
    static final String BLANK = String.valueOf((char) 0x200B);
    /** A Discord ID (a snowflake): only these are ever allowed to ping. */
    private static final Pattern ID = Pattern.compile("\\d{5,25}");

    @Nullable private String content;
    @Nullable private String username;
    @Nullable private String avatarUrl;
    @Nullable private Embed embed;
    /** Role IDs that may be pinged; nothing else ever is. */
    private final Set<String> roles = new LinkedHashSet<>();
    /** User IDs that may be pinged (linked winners, when that is on). */
    private final Set<String> users = new LinkedHashSet<>();

    @Nullable
    public String getContent() {
        return content;
    }

    public DiscordMessage content(@Nullable String content) {
        this.content = blankToNull(cut(content, CONTENT_LIMIT));
        return this;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    public DiscordMessage username(@Nullable String username) {
        // Webhook names are 1-80 characters
        this.username = blankToNull(cut(username, 80));
        return this;
    }

    @Nullable
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public DiscordMessage avatarUrl(@Nullable String avatarUrl) {
        this.avatarUrl = link(avatarUrl);
        return this;
    }

    @Nullable
    public Embed getEmbed() {
        return embed;
    }

    public DiscordMessage embed(@Nullable Embed embed) {
        this.embed = embed;
        return this;
    }

    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public DiscordMessage allowRoles(@Nonnull Iterable<String> ids) {
        for (String id : ids) {
            if (id != null && ID.matcher(id.trim()).matches()) roles.add(id.trim());
        }
        return this;
    }

    public Set<String> getUsers() {
        return Collections.unmodifiableSet(users);
    }

    public DiscordMessage allowUser(@Nullable String id) {
        if (id != null && ID.matcher(id).matches()) users.add(id);
        return this;
    }

    public boolean isEmpty() {
        return content == null && (embed == null || embed.isEmpty());
    }

    /** @return the webhook body: {@code username} and {@code avatar_url} are left out when editing */
    @Nonnull
    public JsonObject toJson(boolean edit) {
        JsonObject json = new JsonObject();
        // An edit with no content would keep the old text, so it is cleared explicitly
        json.addProperty("content", content != null ? content : "");
        if (!edit && username != null) json.addProperty("username", username);
        if (!edit && avatarUrl != null) json.addProperty("avatar_url", avatarUrl);
        JsonArray embeds = new JsonArray();
        if (embed != null && !embed.isEmpty()) embeds.add(embed.toJson());
        json.add("embeds", embeds);

        JsonObject mentions = new JsonObject();
        // An empty parse list: @everyone, @here and names typed by players never ping
        mentions.add("parse", new JsonArray());
        mentions.add("roles", strings(roles));
        mentions.add("users", strings(users));
        json.add("allowed_mentions", mentions);
        return json;
    }

    private static JsonArray strings(Set<String> values) {
        JsonArray array = new JsonArray();
        // Gson 2.2.4 (Spigot 1.8) has no JsonArray.add(String)
        for (String value : values) array.add(new JsonPrimitive(value));
        return array;
    }

    /** An embed: every part is optional; empty parts are left out of the message. */
    public static final class Embed {
        @Nullable String title;
        @Nullable String url;
        @Nullable String description;
        @Nullable Integer color;
        @Nullable String authorName;
        @Nullable String authorUrl;
        @Nullable String authorIcon;
        @Nullable String thumbnail;
        @Nullable String image;
        @Nullable String footerText;
        @Nullable String footerIcon;
        @Nullable Instant timestamp;
        final List<Field> fields = new ArrayList<>();

        public Embed title(@Nullable String title) {
            this.title = blankToNull(cut(title, TITLE_LIMIT));
            return this;
        }

        public Embed url(@Nullable String url) {
            this.url = link(url);
            return this;
        }

        public Embed description(@Nullable String description) {
            this.description = blankToNull(cut(description, DESCRIPTION_LIMIT));
            return this;
        }

        public Embed color(@Nullable Integer color) {
            this.color = color;
            return this;
        }

        public Embed author(@Nullable String name, @Nullable String url, @Nullable String icon) {
            this.authorName = blankToNull(cut(name, TITLE_LIMIT));
            this.authorUrl = link(url);
            this.authorIcon = link(icon);
            return this;
        }

        public Embed thumbnail(@Nullable String url) {
            this.thumbnail = link(url);
            return this;
        }

        public Embed image(@Nullable String url) {
            this.image = link(url);
            return this;
        }

        public Embed footer(@Nullable String text, @Nullable String icon) {
            this.footerText = blankToNull(cut(text, FOOTER_LIMIT));
            this.footerIcon = link(icon);
            return this;
        }

        public Embed timestamp(@Nullable Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /** Adds a field; one with nothing in it is skipped, so a template can leave a field out by emptying it. */
        public Embed field(@Nullable String name, @Nullable String value, boolean inline) {
            String n = blankToNull(cut(name, FIELD_NAME_LIMIT));
            String v = blankToNull(cut(value, FIELD_VALUE_LIMIT));
            if ((n == null && v == null) || fields.size() >= MAX_FIELDS) return this;
            fields.add(new Field(n != null ? n : BLANK, v != null ? v : BLANK, inline));
            return this;
        }

        @Nullable public String getTitle() { return title; }
        @Nullable public String getUrl() { return url; }
        @Nullable public String getDescription() { return description; }
        @Nullable public Integer getColor() { return color; }
        @Nullable public String getAuthorName() { return authorName; }
        @Nullable public String getAuthorUrl() { return authorUrl; }
        @Nullable public String getAuthorIcon() { return authorIcon; }
        @Nullable public String getThumbnail() { return thumbnail; }
        @Nullable public String getImage() { return image; }
        @Nullable public String getFooterText() { return footerText; }
        @Nullable public String getFooterIcon() { return footerIcon; }
        @Nullable public Instant getTimestamp() { return timestamp; }
        public List<Field> getFields() { return Collections.unmodifiableList(fields); }

        boolean isEmpty() {
            // Discord needs some text in an embed; a picture alone is refused
            return title == null && description == null && authorName == null && footerText == null && fields.isEmpty();
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            if (title != null) json.addProperty("title", title);
            if (url != null && title != null) json.addProperty("url", url);
            if (description != null) json.addProperty("description", description);
            if (color != null) json.addProperty("color", color);
            if (authorName != null) {
                JsonObject author = new JsonObject();
                author.addProperty("name", authorName);
                if (authorUrl != null) author.addProperty("url", authorUrl);
                if (authorIcon != null) author.addProperty("icon_url", authorIcon);
                json.add("author", author);
            }
            if (thumbnail != null) json.add("thumbnail", urlObject(thumbnail));
            if (image != null) json.add("image", urlObject(image));
            if (footerText != null) {
                JsonObject footer = new JsonObject();
                footer.addProperty("text", footerText);
                if (footerIcon != null) footer.addProperty("icon_url", footerIcon);
                json.add("footer", footer);
            }
            if (timestamp != null) json.addProperty("timestamp", DateTimeFormatter.ISO_INSTANT.format(timestamp));
            if (!fields.isEmpty()) {
                JsonArray array = new JsonArray();
                for (Field field : fields) {
                    JsonObject object = new JsonObject();
                    object.addProperty("name", field.name);
                    object.addProperty("value", field.value);
                    object.addProperty("inline", field.inline);
                    array.add(object);
                }
                json.add("fields", array);
            }
            return json;
        }

        private static JsonObject urlObject(String url) {
            JsonObject object = new JsonObject();
            object.addProperty("url", url);
            return object;
        }
    }

    /** One embed field. */
    public static final class Field {
        final String name;
        final String value;
        final boolean inline;

        Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }

        public String getName() { return name; }
        public String getValue() { return value; }
        public boolean isInline() { return inline; }
    }

    /** Cuts text to Discord's limit, ending in "…" so it is clear something was left out. */
    @Nullable
    static String cut(@Nullable String text, int limit) {
        if (text == null || text.length() <= limit) return text;
        int end = limit - 1;
        // Don't split an emoji (a surrogate pair) in half
        if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end) + "…";
    }

    @Nullable
    private static String blankToNull(@Nullable String text) {
        return text == null || text.trim().isEmpty() ? null : text;
    }

    /** Discord refuses the whole message over one bad picture link, so anything but http(s) is dropped. */
    @Nullable
    private static String link(@Nullable String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        return trimmed.startsWith("https://") || trimmed.startsWith("http://") ? trimmed : null;
    }
}
