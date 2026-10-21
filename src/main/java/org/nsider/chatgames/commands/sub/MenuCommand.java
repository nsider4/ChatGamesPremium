package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /chatgames menu [id]} and {@code /chatgames admin}: open the main (or any) menu, or the admin menu.
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

    /** {@code menu [id]} opens any menu, including ones server owners added; each checks its own open-requirements. */
    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        Player player = (Player) sender;
        String id = admin ? "admin" : args.length > 0 ? args[0] : "main";
        if (!plugin.getMenus().open(player, id, Arrays.copyOfRange(args, Math.min(1, args.length), args.length))) {
            CommandSupport.send(sender, MessageKey.UNKNOWN_MENU, "%menu%", id);
        }
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (admin || args.length != 1) return Collections.emptyList();
        List<String> ids = new ArrayList<>();
        for (String id : plugin.getMenus().getManager().getMenuIds()) {
            if (id.startsWith(args[0].toLowerCase(Locale.ROOT))) ids.add(id);
        }
        return ids;
    }
}
