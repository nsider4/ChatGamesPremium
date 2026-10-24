package org.nsider.chatgames.discord;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text helpers for Discord: emoji by name, Markdown escaping, Minecraft colour codes,
 * colours and progress bars.
 *
 * <p>Emoji are written as Discord names ({@code :trophy:}) in discord.yml because Spigot 1.8's
 * YAML reader refuses the whole file over a pasted emoji. Discord itself only turns those names
 * into emoji when a person types them, so they are replaced here.</p>
 */
public final class DiscordText {

    private static final Pattern SHORTCODE = Pattern.compile("(?<![<\\w]|<a):([a-z0-9_+-]{2,40}):");
    private static final Pattern COLOR_CODES = Pattern.compile("(?i)[&§]([0-9a-fk-orx]|#[0-9a-f]{6})");
    private static final Pattern HEX = Pattern.compile("#?([0-9a-fA-F]{6})");
    private static final String MARKDOWN = "\\*_~`|>#[]()-";
    private static final char ZERO_WIDTH_SPACE = (char) 0x200B;
    private static final Map<String, String> EMOJI = new HashMap<>();
    private static final Map<String, Integer> COLORS = new HashMap<>();

    static {
        String[] emoji = {
                "trophy", "1F3C6", "first_place", "1F947", "second_place", "1F948", "third_place", "1F949", "medal", "1F3C5",
                "military_medal", "1F396", "crown", "1F451", "star", "2B50", "star2", "1F31F", "sparkles", "2728", "tada", "1F389",
                "confetti_ball", "1F38A", "fire", "1F525", "zap", "26A1", "boom", "1F4A5", "100", "1F4AF", "gem", "1F48E",
                "moneybag", "1F4B0", "gift", "1F381", "video_game", "1F3AE", "game_die", "1F3B2", "jigsaw", "1F9E9",
                "dart", "1F3AF", "checkered_flag", "1F3C1", "stopwatch", "23F1", "timer", "23F2", "alarm_clock", "23F0",
                "hourglass", "231B", "hourglass_flowing_sand", "23F3", "clock", "1F570", "calendar", "1F4C5",
                "white_check_mark", "2705", "heavy_check_mark", "2714", "x", "274C", "no_entry", "26D4", "warning", "26A0",
                "question", "2753", "grey_question", "2754", "exclamation", "2757", "bell", "1F514", "loudspeaker", "1F4E2",
                "mega", "1F4E3", "speech_balloon", "1F4AC", "thought_balloon", "1F4AD", "brain", "1F9E0", "bulb", "1F4A1",
                "abc", "1F524", "abcd", "1F521", "capital_abcd", "1F520", "1234", "1F522", "symbols", "1F523", "pencil", "1F4DD",
                "pencil2", "270F", "memo", "1F4DD", "book", "1F4D6", "books", "1F4DA", "scroll", "1F4DC", "clipboard", "1F4CB",
                "link", "1F517", "mag", "1F50D", "eyes", "1F440", "eye", "1F441", "repeat", "1F501", "arrows_counterclockwise", "1F504",
                "leftwards_arrow_with_hook", "21A9", "heavy_plus_sign", "2795", "heavy_minus_sign", "2796", "heavy_multiplication_x", "2716",
                "heavy_division_sign", "2797", "infinity", "267E", "keycap_ten", "1F51F", "mouse_three_button", "1F5B1",
                "point_up", "261D", "point_right", "1F449", "point_down", "1F447", "raised_hands", "1F64C", "clap", "1F44F",
                "muscle", "1F4AA", "handshake", "1F91D", "wave", "1F44B", "thinking", "1F914", "sunglasses", "1F60E",
                "partying_face", "1F973", "rocket", "1F680", "earth_americas", "1F30E", "globe_with_meridians", "1F310",
                "busts_in_silhouette", "1F465", "bust_in_silhouette", "1F464",
                "bar_chart", "1F4CA", "chart_with_upwards_trend", "1F4C8", "chart_with_downwards_trend", "1F4C9",
                "crossed_swords", "2694", "dagger", "1F5E1", "bow_and_arrow", "1F3F9", "shield", "1F6E1", "pick", "26CF",
                "hammer", "1F528", "hammer_and_pick", "2692", "tools", "1F6E0", "axe", "1FA93", "bricks", "1F9F1",
                "fishing_pole_and_fish", "1F3A3", "fish", "1F41F", "tropical_fish", "1F420", "meat_on_bone", "1F356",
                "poultry_leg", "1F357", "bread", "1F35E", "apple", "1F34E", "green_apple", "1F34F", "cake", "1F370",
                "cookie", "1F36A", "fork_and_knife", "1F374", "cooking", "1F373", "bone", "1F9B4",
                "dog", "1F415", "cat", "1F408", "horse", "1F40E", "parrot", "1F99C", "sheep", "1F411", "ram", "1F40F",
                "scissors", "2702", "cow", "1F404", "pig", "1F416", "chicken", "1F414", "zombie", "1F9DF", "skull", "1F480",
                "seedling", "1F331", "evergreen_tree", "1F332", "deciduous_tree", "1F333", "mushroom", "1F344", "rock", "1FAA8",
                "diamond_shape_with_a_dot_inside", "1F4A0", "large_blue_diamond", "1F537", "small_orange_diamond", "1F538",
                "red_circle", "1F534", "orange_circle", "1F7E0", "yellow_circle", "1F7E1", "green_circle", "1F7E2",
                "blue_circle", "1F535", "purple_circle", "1F7E3", "white_circle", "26AA", "black_circle", "26AB",
                "green_square", "1F7E9", "white_large_square", "2B1C", "black_large_square", "2B1B", "arrow_right", "27A1",
                "arrow_up", "2B06", "arrow_down", "2B07", "small_red_triangle_down", "1F53B", "small_red_triangle", "1F53A",
                "lock", "1F512", "unlock", "1F513", "key", "1F511", "new", "1F195", "up", "1F199", "cool", "1F192", "free", "1F193",
                "information_source", "2139", "heart", "2764", "green_heart", "1F49A", "blue_heart", "1F499",
                "yellow_heart", "1F49B", "purple_heart", "1F49C", "orange_heart", "1F9E1", "map", "1F5FA", "compass", "1F9ED",
                "label", "1F3F7", "ticket", "1F3AB", "tickets", "1F39F", "admission_tickets", "1F39F", "joystick", "1F579",
                "slot_machine", "1F3B0", "spades", "2660", "clubs", "2663", "hearts", "2665", "diamonds", "2666",
                "beginner", "1F530", "game_controller", "1F3AE", "pushpin", "1F4CC", "round_pushpin", "1F4CD", "triangular_flag_on_post", "1F6A9",
        };
        for (int i = 0; i < emoji.length; i += 2) {
            EMOJI.put(emoji[i], new String(Character.toChars(Integer.parseInt(emoji[i + 1], 16))));
        }
        // Discord's own colours first, then Minecraft's
        Object[] colors = {
                "blurple", 0x5865F2, "discord", 0x5865F2, "green", 0x57F287, "yellow", 0xFEE75C, "fuchsia", 0xEB459E,
                "red", 0xED4245, "white", 0xFFFFFF, "black", 0x23272A, "gold", 0xF1C40F, "orange", 0xE67E22,
                "blue", 0x3498DB, "aqua", 0x1ABC9C, "purple", 0x9B59B6, "gray", 0x95A5A6, "grey", 0x95A5A6,
                "dark_red", 0xAA0000, "dark_green", 0x00AA00, "dark_blue", 0x0000AA, "dark_aqua", 0x00AAAA,
                "dark_purple", 0xAA00AA, "dark_gray", 0x555555, "light_purple", 0xFF55FF, "pink", 0xFF55FF,
        };
        for (int i = 0; i < colors.length; i += 2) COLORS.put((String) colors[i], (Integer) colors[i + 1]);
    }

    private DiscordText() {}

    /** Replaces {@code :name:} emoji that this table knows; anything else (custom {@code <:name:id>}) stays as it is. */
    @Nonnull
    public static String emoji(@Nonnull String text) {
        if (text.indexOf(':') < 0) return text;
        Matcher matcher = SHORTCODE.matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String emoji = EMOJI.get(matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(emoji != null ? emoji : matcher.group()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** Escapes Markdown, so a name like {@code __Bob__} shows as written instead of underlined. */
    @Nonnull
    public static String escape(@Nullable String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder(text.length() + 8);
        for (char c : text.toCharArray()) {
            if (MARKDOWN.indexOf(c) >= 0) out.append('\\');
            // Breaks "@everyone" and "<@123>" (a second guard: allowed_mentions already stops the ping)
            out.append(c);
            if (c == '@') out.append(ZERO_WIDTH_SPACE);
        }
        return out.toString();
    }

    /** Undoes {@link #escape}, for the parts of a message that show Markdown as typed. */
    @Nonnull
    public static String unescape(@Nonnull String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length() && MARKDOWN.indexOf(text.charAt(i + 1)) >= 0) continue;
            if (c == ZERO_WIDTH_SPACE && i > 0 && text.charAt(i - 1) == '@') continue;
            out.append(c);
        }
        return out.toString();
    }

    /** Removes Minecraft colour codes ({@code &a}, {@code §l}, {@code &#ff0000}). */
    @Nonnull
    public static String stripColors(@Nullable String text) {
        return text == null ? "" : COLOR_CODES.matcher(text).replaceAll("");
    }

    /**
     * @param value {@code #RRGGBB}, {@code RRGGBB}, a name ({@code gold}, {@code blurple}...) or a whole number
     * @return the colour, or null to leave the embed's default
     */
    @Nullable
    public static Integer color(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String trimmed = value.trim();
        Matcher hex = HEX.matcher(trimmed);
        if (hex.matches()) return Integer.parseInt(hex.group(1), 16);
        Integer named = COLORS.get(trimmed.toLowerCase(Locale.ROOT));
        if (named != null) return named;
        try {
            int number = Integer.parseInt(trimmed);
            return number >= 0 && number <= 0xFFFFFF ? number : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** A bar such as ▰▰▰▱▱▱ for {@code progress} of {@code goal}. */
    @Nonnull
    public static String bar(int progress, int goal, int length, @Nonnull String full, @Nonnull String empty) {
        int size = Math.max(1, Math.min(length, 40));
        int filled = goal <= 0 ? size : (int) Math.min(size, Math.max(0, (long) progress * size / goal));
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < size; i++) bar.append(i < filled ? full : empty);
        return bar.toString();
    }

    /** Replaces each {@code %key%} with its value. */
    @Nonnull
    public static String fill(@Nullable String text, @Nonnull Map<String, String> values) {
        if (text == null || text.isEmpty()) return "";
        if (text.indexOf('%') < 0) return text;
        StringBuilder out = new StringBuilder(text.length() + 32);
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            int end = c == '%' ? text.indexOf('%', i + 1) : -1;
            if (end > i + 1) {
                String value = values.get(text.substring(i, end + 1));
                if (value != null) {
                    out.append(value);
                    i = end + 1;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }
}
