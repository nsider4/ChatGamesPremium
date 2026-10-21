package org.nsider.chatgames.commands.sub;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.commands.SubCommand;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.managers.PresetManager;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /chatgames preset list | load <name|none> | save <name>}: game presets, kept in presets.yml.
 */
public class PresetCommand extends SubCommand {

    private static final String USAGE = "/chatgames preset <list|load|save> [name]";

    private final ChatGames plugin;

    public PresetCommand(@Nonnull ChatGames plugin) {
        super("preset", PermissionKey.PRESET.getPermission(), false, "presets");
        this.plugin = plugin;
    }

    @Override
    public void execute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        String action = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "list":
                list(sender);
                return;
            case "load":
                if (args.length != 2) break;
                load(sender, args[1]);
                return;
            case "save":
                if (args.length != 2) break;
                save(sender, args[1]);
                return;
            default:
                break;
        }
        CommandSupport.usage(sender, USAGE);
    }

    private void list(CommandSender sender) {
        List<String> names = PresetManager.getNames();
        if (names.isEmpty()) {
            CommandSupport.send(sender, MessageKey.PRESET_LIST_EMPTY);
            return;
        }
        String active = PresetManager.getActive();
        String marker = ConfigManager.getMessages().get(MessageKey.PRESET_ACTIVE_MARKER);
        CommandSupport.send(sender, MessageKey.PRESET_LIST_HEADER);
        for (String name : names) {
            CommandSupport.send(sender, MessageKey.PRESET_LIST_ENTRY,
                    "%preset%", name,
                    "%active%", name.equals(active) ? marker : "",
                    "%games%", describe(PresetManager.getGames(name, null)));
        }
    }

    private void load(CommandSender sender, String name) {
        if (name.equalsIgnoreCase(PresetManager.NONE)) {
            plugin.getScheduler().onGlobal(() -> {
                plugin.getMainTask().applyPreset(null);
                CommandSupport.send(sender, MessageKey.PRESET_CLEARED);
            });
            return;
        }
        String preset = find(name);
        if (preset == null) {
            CommandSupport.send(sender, MessageKey.PRESET_NOT_FOUND, "%preset%", name);
            return;
        }
        plugin.getScheduler().onGlobal(() -> {
            plugin.getMainTask().applyPreset(preset);
            CommandSupport.send(sender, MessageKey.PRESET_LOADED,
                    "%preset%", preset, "%games%", describe(plugin.getMainTask().getEnabledGames()));
        });
    }

    private void save(CommandSender sender, String name) {
        if (!PresetManager.isValidName(name)) {
            CommandSupport.send(sender, MessageKey.PRESET_INVALID_NAME);
            return;
        }
        List<GameType> games = plugin.getMainTask().getEnabledGames();
        PresetManager.save(name, games);
        CommandSupport.send(sender, MessageKey.PRESET_SAVED, "%preset%", name, "%games%", describe(games));
    }

    /**
     * @return the preset's name as written in presets.yml, matched ignoring case
     */
    private static String find(String name) {
        for (String preset : PresetManager.getNames()) {
            if (preset.equalsIgnoreCase(name)) return preset;
        }
        return null;
    }

    private static String describe(List<GameType> games) {
        if (games.isEmpty()) return "-";
        List<String> keys = new ArrayList<>();
        for (GameType type : games) keys.add(type.getConfigKey());
        return String.join(", ", keys);
    }

    @Nonnull
    @Override
    public List<String> tabComplete(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length == 1) {
            return MessageUtils.filterSuggestions(Arrays.asList("list", "load", "save"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("load")) {
            List<String> names = new ArrayList<>(PresetManager.getNames());
            names.add(PresetManager.NONE);
            return MessageUtils.filterSuggestions(names, args[1]);
        }
        return Collections.emptyList();
    }
}
