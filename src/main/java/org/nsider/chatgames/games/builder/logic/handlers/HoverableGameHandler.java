package org.nsider.chatgames.games.builder.logic.handlers;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.games.builder.BuilderUtils;
import org.nsider.chatgames.games.builder.logic.GameHandler;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.Collection;

public class HoverableGameHandler implements GameHandler<String> {
    private final ChatGames plugin;

    public HoverableGameHandler(ChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public String handle(String... args) {
        Collection<String> words = ConfigManager.getWords().getStringList("hoverable.Words");
        String selected = args.length > 0 && !args[0].isEmpty() ? args[0] : BuilderUtils.getRandomValue(plugin, words);

        String toReplace = ConfigManager.getMessagesConfig().getString("hoverable.hoverableText");
        String hoverText = ConfigManager.getMessagesConfig().getString("hoverable.hoverText").replaceAll("%selection%", selected);

        if (plugin.isPlaceHolderAPIEnabled()) {
            toReplace = PlaceholderAPI.setPlaceholders(null, toReplace);
            hoverText = PlaceholderAPI.setPlaceholders(null, hoverText);
        }

        TextComponent component = new TextComponent(TextComponent.fromLegacyText(MessageUtils.color(toReplace)));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(MessageUtils.color(hoverText)).create()));

        plugin.getGameManager().sendGameStartedComponent("hoverable", component, "%hoverableText%");

        return selected;
    }
}
