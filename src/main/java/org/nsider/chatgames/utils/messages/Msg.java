package org.nsider.chatgames.utils.messages;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.games.round.Round;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A message from messages.yml, ready to send: placeholders, PlaceholderAPI,
 * colours, {@code <center>} and interactive text are handled in one place.
 *
 * <pre>
 *   Msg.of(lines).with("%winner_1%", name).with("%time_1%", "2.31").send(player);
 * </pre>
 *
 * Placeholders are replaced literally (no regex), so values containing "$" or
 * "\" are safe. A Msg is built once and can be sent to many players.
 */
public final class Msg {

    private static final String CENTER = "<center>";

    private final List<String> lines;
    private final Map<String, String> replacements = new LinkedHashMap<>();
    @Nullable private Round.ComponentSpec component;

    private Msg(List<String> lines) {
        this.lines = lines;
    }

    @Nonnull
    public static Msg of(@Nullable List<String> lines) {
        return new Msg(lines == null ? Collections.<String>emptyList() : new ArrayList<>(lines));
    }

    @Nonnull
    public static Msg of(@Nullable String line) {
        return new Msg(line == null || line.isEmpty() ? Collections.<String>emptyList() : Collections.singletonList(line));
    }

    @Nonnull
    public Msg with(@Nonnull String placeholder, @Nullable Object value) {
        replacements.put(placeholder, value == null ? "" : String.valueOf(value));
        return this;
    }

    @Nonnull
    public Msg with(@Nonnull Map<String, String> values) {
        replacements.putAll(values);
        return this;
    }

    /** Replaces the spec's placeholder with clickable/hoverable text. */
    @Nonnull
    public Msg component(@Nullable Round.ComponentSpec spec) {
        this.component = spec;
        return this;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /**
     * @return the lines with placeholders and colours applied, for the console or logs
     */
    @Nonnull
    public List<String> render(@Nullable CommandSender viewer) {
        List<String> rendered = new ArrayList<>();
        for (String line : lines) {
            rendered.add(MessageUtils.color(papi(prepare(line), viewer)));
        }
        return rendered;
    }

    public void send(@Nonnull CommandSender viewer) {
        for (String line : lines) {
            String text = prepare(line);
            if (component != null && viewer instanceof Player && text.contains(component.getPlaceholder())) {
                sendWithComponent((Player) viewer, papi(text, viewer));
            } else {
                // Applies PlaceholderAPI, colours and <center> itself
                MessageUtils.sendMessage(viewer, text);
            }
        }
    }

    private String prepare(String line) {
        String text = line;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    private static String papi(String text, CommandSender viewer) {
        if (viewer instanceof Player && ChatGames.getPlugin(ChatGames.class).isPlaceHolderAPIEnabled()) {
            return PlaceholderAPI.setPlaceholders((Player) viewer, text);
        }
        return text;
    }

    private void sendWithComponent(Player player, String text) {
        boolean centered = text.contains(CENTER);
        text = text.replace(CENTER, "");
        int split = text.indexOf(component.getPlaceholder());
        String before = MessageUtils.color(text.substring(0, split));
        String after = MessageUtils.color(text.substring(split + component.getPlaceholder().length()));

        TextComponent interactive = new TextComponent(TextComponent.fromLegacyText(MessageUtils.color(papi(prepare(component.getText()), player))));
        String hover = papi(prepare(component.getHoverText()), player);
        if (!hover.isEmpty()) {
            interactive.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(MessageUtils.color(hover)).create()));
        }
        if (component.getClickCommand() != null) {
            interactive.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, component.getClickCommand()));
        }

        String padding = "";
        if (centered) {
            String visible = before + ChatColor.stripColor(MessageUtils.color(component.getText())) + after;
            padding = CenteredMessageUtils.padding(visible);
        }
        List<BaseComponent> parts = new ArrayList<>();
        Collections.addAll(parts, TextComponent.fromLegacyText(padding + before));
        parts.add(interactive);
        if (!after.isEmpty()) Collections.addAll(parts, TextComponent.fromLegacyText(after));
        player.spigot().sendMessage(parts.toArray(new BaseComponent[0]));
    }
}
