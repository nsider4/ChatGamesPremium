package org.nsider.chatgames.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Helpers shared by the sub-commands.
 */
public final class CommandSupport {

    private CommandSupport() {}

    /**
     * Sends a message from messages.yml.
     *
     * @param placeholders placeholder/value pairs
     */
    public static void send(@Nonnull CommandSender sender, @Nonnull MessageKey key, @Nonnull String... placeholders) {
        String message = ConfigManager.getMessages().get(key, placeholders);
        if (!message.isEmpty()) MessageUtils.sendMessage(sender, message);
    }

    /**
     * @return a message from messages.yml, for use inside another message
     */
    @Nonnull
    public static String message(@Nonnull MessageKey key, @Nonnull String... placeholders) {
        return ConfigManager.getMessages().get(key, placeholders);
    }

    public static void usage(@Nonnull CommandSender sender, @Nonnull String usage) {
        send(sender, MessageKey.USAGE, "%usage%", usage);
    }

    @Nonnull
    public static String state(boolean on) {
        return ConfigManager.getMessages().get(on ? MessageKey.STATE_ON : MessageKey.STATE_OFF);
    }

    /**
     * @return the number, or null (after telling the sender) if it isn't a whole number of 0 or more
     */
    @Nullable
    public static Integer parseAmount(@Nonnull CommandSender sender, @Nonnull String text) {
        try {
            int amount = Integer.parseInt(text.trim());
            if (amount >= 0) return amount;
        } catch (NumberFormatException ignored) {
            // Falls through to the message below
        }
        send(sender, MessageKey.NUMBER_NOT_FOUND);
        return null;
    }

    /**
     * Finds a player by name, online or not, and calls {@code found} on the main thread
     * (global region on Folia). Tells the sender instead if no such player ever joined.
     *
     * <p>Offline names are looked up off the main thread: on online-mode servers
     * {@link Bukkit#getOfflinePlayer(String)} may ask Mojang for the UUID.</p>
     */
    @SuppressWarnings("deprecation")
    public static void findPlayer(@Nonnull ChatGames plugin, @Nonnull CommandSender sender, @Nonnull String name,
                                  @Nonnull Consumer<OfflinePlayer> found) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            plugin.getScheduler().onGlobal(() -> found.accept(online));
            return;
        }
        plugin.getScheduler().async(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(name);
            boolean known = player.hasPlayedBefore() || ChatGamesPlayerManager.isLoaded(player.getUniqueId());
            plugin.getScheduler().global(() -> {
                if (known) {
                    found.accept(player);
                } else {
                    send(sender, MessageKey.PLAYER_NOT_FOUND, "%player%", name);
                }
            });
        });
    }

    /**
     * @return the name to show for a player, falling back to what was typed
     */
    @Nonnull
    public static String nameOf(@Nonnull OfflinePlayer player, @Nonnull String typed) {
        return player.getName() != null ? player.getName() : typed;
    }

    @Nonnull
    public static List<String> onlinePlayerNames(@Nonnull String typed) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
        return MessageUtils.filterSuggestions(names, typed);
    }

    @Nonnull
    public static List<String> chatGameNames(@Nonnull String typed) {
        List<String> names = new ArrayList<>();
        for (GameType type : GameType.values()) {
            if (!type.isRace()) names.add(type.getConfigKey());
        }
        return MessageUtils.filterSuggestions(names, typed);
    }

    /**
     * @return true if the sender may start this game by command
     */
    public static boolean canStart(@Nonnull CommandSender sender, @Nonnull GameType type) {
        return sender.hasPermission(PermissionKey.START_ALL.getPermission())
                || sender.hasPermission(PermissionKey.startPermission(type));
    }
}
