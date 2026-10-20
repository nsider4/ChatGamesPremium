package org.nsider.chatgames.utils.gameplay;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import org.nsider.inventorymanager.extras.Nonnull;
import java.util.Collections;
import java.util.Set;

public final class CommandUtils {

    private CommandUtils() {}

    /**
     * Runs a command as the player with every permission, for {@code [player_as_op]} rewards.
     * Ported from 1.9.9; still to be checked on 26.x in the test matrix (see RECODE_PLAN step 1 notes).
     */
    public static void executeAsOp(@Nonnull Player player, @Nonnull String command) {
        CommandSender proxySender = new CommandSender() {
            @Override public boolean isOp() { return true; }
            @Override public void setOp(boolean value) {}
            @Override public void sendMessage(String message) { player.sendMessage(message); }
            @Override public void sendMessage(String[] messages) { player.sendMessage(messages); }
            @Override public boolean hasPermission(String name) { return true; }
            @Override public boolean hasPermission(Permission permission) { return true; }
            @Override public boolean isPermissionSet(String name) { return true; }
            @Override public boolean isPermissionSet(Permission permission) { return true; }
            @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) { return null; }
            @Override public PermissionAttachment addAttachment(Plugin plugin) { return null; }
            @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) { return null; }
            @Override public PermissionAttachment addAttachment(Plugin plugin, int ticks) { return null; }
            @Override public void removeAttachment(PermissionAttachment attachment) {}
            @Override public void recalculatePermissions() {}
            @Override public Set<PermissionAttachmentInfo> getEffectivePermissions() { return Collections.emptySet(); }
            @Override public Server getServer() { return Bukkit.getServer(); }
            @Override public String getName() { return player.getName(); }
        };
        Bukkit.dispatchCommand(proxySender, command);
    }
}
