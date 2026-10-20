package org.nsider.chatgames.commands.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * {@code /chatgames toggle [player]} (see and play chat games) and
 * {@code /chatgames togglesound [player]} (game sounds). Toggling another online
 * player needs the {@code .others} permission.
 */
public class ToggleCommand extends SubCommand {

    private final boolean sound;
    private final PermissionKey othersPermission;

    private ToggleCommand(String name, PermissionKey permission, PermissionKey othersPermission, boolean sound) {
        super(name, permission.getPermission(), false);
        this.othersPermission = othersPermission;
        this.sound = sound;
    }

    @Nonnull
    public static ToggleCommand games() {
        return new ToggleCommand("toggle", PermissionKey.TOGGLE, PermissionKey.TOGGLE_OTHERS, false);
    }

    @Nonnull
    public static ToggleCommand sound() {
        return new ToggleCommand("togglesound", PermissionKey.TOGGLE_SOUND, PermissionKey.TOGGLE_SOUND_OTHERS, true);
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        Player target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                CommandSupport.usage(sender, "/chatgames " + getName() + " <player>");
                return;
            }
            target = (Player) sender;
        } else {
            if (!sender.hasPermission(othersPermission.getPermission())) {
                CommandSupport.send(sender, MessageKey.NOT_ENOUGH_PERMISSIONS);
                return;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                CommandSupport.send(sender, MessageKey.PLAYER_NOT_FOUND, "%player%", args[0]);
                return;
            }
        }

        ChatGamesPlayer data = ChatGamesPlayerManager.get(target.getUniqueId());
        if (data == null) {
            // Loaded a moment after joining
            CommandSupport.send(sender, MessageKey.DATA_LOADING);
            return;
        }
        boolean on;
        if (sound) {
            on = !data.isSoundEnabled();
            data.setSoundEnabled(on);
        } else {
            on = !data.isToggled();
            data.setToggled(on);
        }

        String state = CommandSupport.state(on);
        if (target == sender) {
            CommandSupport.send(sender, sound ? MessageKey.TOGGLE_SOUND : MessageKey.TOGGLE_GAME, "%state%", state);
        } else {
            CommandSupport.send(sender, sound ? MessageKey.TOGGLE_SOUND_OTHER : MessageKey.TOGGLE_GAME_OTHER,
                    "%state%", state, "%player%", target.getName());
            CommandSupport.send(target, sound ? MessageKey.TOGGLE_SOUND : MessageKey.TOGGLE_GAME, "%state%", state);
        }
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        return args.length == 1 && sender.hasPermission(othersPermission.getPermission())
                ? CommandSupport.onlinePlayerNames(args[0]) : Collections.<String>emptyList();
    }
}
