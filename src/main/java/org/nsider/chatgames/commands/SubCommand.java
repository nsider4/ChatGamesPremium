package org.nsider.chatgames.commands;

import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * One {@code /chatgames <name>} sub-command.
 *
 * <p>{@link ChatGamesCommand} checks the permission and player-only rule before
 * calling {@link #execute}, so implementations only handle their own arguments.</p>
 */
@Getter
public abstract class SubCommand {

    private final String name;
    @Nullable private final String permission;
    private final boolean playerOnly;
    private final List<String> aliases;

    /**
     * @param name       the word after {@code /chatgames}, lower case
     * @param permission the permission required, or null for everyone
     * @param playerOnly true if the console can't use it
     * @param aliases    other words that run this command
     */
    protected SubCommand(@Nonnull String name, @Nullable String permission, boolean playerOnly, String... aliases) {
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
        this.aliases = Collections.unmodifiableList(Arrays.asList(aliases));
    }

    /**
     * Runs the command.
     *
     * @param sender who ran it
     * @param args   the arguments after the sub-command name
     */
    public abstract void execute(@Nonnull CommandSender sender, @Nonnull String[] args);

    /**
     * Suggestions for the argument being typed.
     *
     * @param args the arguments after the sub-command name; the last one is being typed
     */
    @Nonnull
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        return Collections.emptyList();
    }

    public boolean canUse(@Nonnull CommandSender sender) {
        return permission == null || sender.hasPermission(permission);
    }
}
