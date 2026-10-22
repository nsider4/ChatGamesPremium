package org.nsider.chatgames.menus;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.registry.GameRegistry;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.menu.Menu;
import org.nsider.inventorymanager.menu.MenuManager;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Every menu, through InventoryManager. menus.yml holds the layouts and what each button
 * does ({@code actions}); the classes here only supply ChatGames' data (placeholders and
 * list entries) and the few actions that need code, written {@code [chatgames:...]}.
 * Buttons that change something mostly run the matching {@code /chatgames} command, so
 * permissions and replies stay the same as typing it.
 *
 * <p>Server owners can add menus under {@code menus:} in menus.yml or as files in the
 * {@code menus/} folder, and open them with {@code /chatgames menu <id>}.</p>
 */
public final class Menus {

    private final ChatGames plugin;
    private final MenuManager manager;
    private final ProfileMenu profile;
    private final DebugMenu debug;

    public Menus(@Nonnull ChatGames plugin) {
        this.plugin = plugin;
        this.manager = new MenuManager(plugin);
        this.profile = new ProfileMenu(plugin, this);
        this.debug = new DebugMenu(plugin, this);
        manager.registerMenu("main", new MainMenu(plugin));
        manager.registerMenu("profile", profile);
        new ChallengesMenu(plugin).register(manager);
        new StreaksMenu(plugin).register(manager);
        new LeaderboardMenu(plugin).register(manager);
        new AdminMenu(plugin).register(manager);
        debug.register(manager);
        plugin.getServer().getPluginManager().registerEvents(debug, plugin);
        manager.registerAction("chatgames:no-permission",
                (menu, argument) -> CommandSupport.send(menu.getPlayer(), MessageKey.NOT_ENOUGH_PERMISSIONS));
        reload();
    }

    /** Reads menus.yml's {@code menus} section and the {@code menus/} folder again. */
    public void reload() {
        manager.load(ConfigManager.getMenus().getConfigurationSection("menus"));
        File folder = new File(plugin.getDataFolder(), "menus");
        if (folder.isDirectory()) manager.loadFolder(folder);
    }

    /** @return false if there is no menu with that id */
    public boolean open(@Nonnull Player player, @Nonnull String id, @Nonnull String... args) {
        return manager.open(player, id, args);
    }

    public void closeAll() {
        manager.closeAll();
    }

    @Nonnull
    public MenuManager getManager() {
        return manager;
    }

    @Nonnull
    public ProfileMenu getProfile() {
        return profile;
    }

    @Nonnull
    public DebugMenu getDebug() {
        return debug;
    }

    /** @return the game's name for menus and messages: menus.yml {@code game-names}, else "Guess The Number" from the key */
    @Nonnull
    public static String gameName(@Nonnull GameType type) {
        String configured = ConfigManager.getMenus() == null ? null : ConfigManager.getMenus().getString("game-names." + type.getConfigKey());
        if (configured != null) return configured;
        String added = GameRegistry.displayName(type);
        if (added != null) return added;
        StringBuilder name = new StringBuilder();
        for (String word : type.getConfigKey().split("_")) {
            if (name.length() > 0) name.append(' ');
            name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return name.toString();
    }

    /** @return the game's icon from menus.yml {@code game-icons} */
    @Nonnull
    public static String gameIcon(@Nonnull GameType type) {
        String added = GameRegistry.icon(type);
        return ConfigManager.getMenus().getString("game-icons." + type.getConfigKey(), added != null ? added : "paper");
    }

    /** %game%, %key% (as in commands) and %icon%, for list entries. */
    @Nonnull
    static Map<String, Object> gameValues(@Nonnull GameType game) {
        Map<String, Object> values = new HashMap<>();
        values.put("%game%", gameName(game));
        values.put("%key%", game.getConfigKey());
        values.put("%icon%", gameIcon(game));
        return values;
    }

    /** Seconds with 3 decimals, as in the leaderboards. */
    @Nonnull
    public static String seconds(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    /** @return "#3", or the menus.yml text for no rank */
    @Nonnull
    static String rank(int rank) {
        return rank > 0 ? "#" + rank : ConfigManager.getMenus().getString("formats.no-rank", "-");
    }

    /** @return the menu's {@code formats.none} text, coloured */
    @Nonnull
    static String none(@Nonnull Menu menu) {
        return menu.text("formats.none", "&7-");
    }

    /** Sends a menus.yml {@code messages.<key>} line, if set. */
    static void message(@Nonnull Player player, @Nonnull String key, @Nonnull String... placeholders) {
        String text = ConfigManager.getMenus().getString("messages." + key, "");
        if (text.isEmpty()) return;
        for (int i = 0; i + 1 < placeholders.length; i += 2) text = text.replace(placeholders[i], placeholders[i + 1]);
        MessageUtils.sendMessage(player, text);
    }
}
