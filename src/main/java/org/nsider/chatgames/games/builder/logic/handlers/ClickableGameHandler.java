package org.nsider.chatgames.games.builder.logic.handlers;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.games.builder.logic.GameHandler;
import org.nsider.chatgames.utils.messages.MessageUtils;

public class ClickableGameHandler implements GameHandler<String> {
    private final ChatGames plugin;

    public ClickableGameHandler(ChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public String handle(String... args) {
        plugin.getGameManager().generateNewClickableKey();
        String selected = plugin.getGameManager().getClickableKey();

        String toReplace = ConfigManager.getMessagesConfig().getString("clickable.clickableText");
        String hoverText = ConfigManager.getMessagesConfig().getString("clickable.hoverText");

        if (plugin.isPlaceHolderAPIEnabled()) {
            toReplace = PlaceholderAPI.setPlaceholders(null, toReplace);
            hoverText = PlaceholderAPI.setPlaceholders(null, hoverText);
        }

        TextComponent component = new TextComponent(TextComponent.fromLegacyText(MessageUtils.color(toReplace)));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chatgames " + selected));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(MessageUtils.color(hoverText)).create()));

        plugin.getGameManager().sendGameStartedComponent("clickable", component, "%clickableText%");

        return selected;
    }
}
