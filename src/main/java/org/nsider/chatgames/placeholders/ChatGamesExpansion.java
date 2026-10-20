package org.nsider.chatgames.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.progression.ProgressionManager;
import org.nsider.chatgames.races.Race;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

/**
 * {@code %chatgames_<id>%} for PlaceholderAPI. The values are documented in
 * {@link PlaceholderResolver}; everything is answered from memory, so scoreboard
 * and tab plugins can ask as often as they like, from any thread.
 */
public class ChatGamesExpansion extends PlaceholderExpansion {

    private final ChatGames plugin;

    public ChatGamesExpansion(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    @Nonnull
    public String getIdentifier() {
        return "chatgames";
    }

    @Override
    @Nonnull
    public String getAuthor() {
        return "Nsider";
    }

    @Override
    @Nonnull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /** Keeps the expansion registered across {@code /papi reload}; it ships inside the plugin. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    @Nullable
    public String onRequest(@Nullable OfflinePlayer player, @Nonnull String params) {
        ChatGamesPlayer data = player == null ? null : ChatGamesPlayerManager.get(player.getUniqueId());
        String empty = ConfigValue.getString(ConfigPaths.PLACEHOLDER_EMPTY_VALUE);
        Race race = plugin.getRaceManager().getCurrent();
        PlaceholderResolver.Status status = new PlaceholderResolver.Status(
                plugin.getGameManager().getCurrentGame(), plugin.getMainTask().getSecondsUntilNextGame(),
                race == null ? null : race.getDisplayName(), race == null ? 0 : race.getAmount(),
                race == null || player == null ? 0 : race.getProgress().getCount(player.getUniqueId()));
        ProgressionManager progression = plugin.getProgressionManager();
        return new PlaceholderResolver(plugin.getLeaderboardCache().getSnapshot(), data, status,
                empty == null ? "" : empty, progression.getChallenges(), progression.today()).resolve(params);
    }
}
