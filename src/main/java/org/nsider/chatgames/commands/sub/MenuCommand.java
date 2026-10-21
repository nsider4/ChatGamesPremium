package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.inventorymanager.extras.Nonnull;

/**
 * {@code /chatgames menu} and {@code /chatgames admin}: open the main or the admin menu.
 */
public class MenuCommand extends SubCommand {

    private final ChatGames plugin;
    private final boolean admin;

    private MenuCommand(ChatGames plugin, String name, PermissionKey permission, boolean admin, String... aliases) {
        super(name, permission.getPermission(), true, aliases);
        this.plugin = plugin;
        this.admin = admin;
    }

    @Nonnull
    public static MenuCommand main(@Nonnull ChatGames plugin) {
        return new MenuCommand(plugin, "menu", PermissionKey.MENU, false, "gui");
    }

    @Nonnull
    public static MenuCommand admin(@Nonnull ChatGames plugin) {
        return new MenuCommand(plugin, "admin", PermissionKey.ADMIN, true);
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        Player player = (Player) sender;
        if (admin) plugin.getMenus().getAdmin().open(player);
        else plugin.getMenus().getMain().open(player);
    }
}
