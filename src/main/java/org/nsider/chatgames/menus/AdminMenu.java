package org.nsider.chatgames.menus;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.config.MessageKey;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.gameevents.EventDefinition;
import org.nsider.chatgames.gameevents.EventManager;
import org.nsider.chatgames.managers.PresetManager;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.tasks.MainTask;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code /chatgames admin}. Each button runs the matching command as the admin, so it
 * also needs that command's permission and replies the same way.
 */
public final class AdminMenu {

    private final ChatGames plugin;
    private final Menus menus;

    AdminMenu(ChatGames plugin, Menus menus) {
        this.plugin = plugin;
        this.menus = menus;
    }

    public void open(@Nonnull Player player) {
        if (!player.hasPermission(PermissionKey.ADMIN.getPermission())) {
            CommandSupport.send(player, MessageKey.NOT_ENOUGH_PERMISSIONS);
            return;
        }
        MainTask rotation = plugin.getMainTask();
        GameType current = plugin.getGameManager().getCurrentGame();
        EventManager.Run event = plugin.getEventManager().getRunning();
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        MenuBuilder menu = MenuBuilder.of(plugin, "admin", player);
        String none = menu.text("formats.none", "&7-");
        String preset = PresetManager.getActive();
        menu.with("%current_game%", plugin.isInGame() && current != null ? Menus.gameName(current) : none)
                .with("%next_game%", rotation.isTaskEnabled() ? rotation.getSecondsUntilNextGame() + "s" : none)
                .with("%rotation_state%", CommandSupport.state(rotation.isTaskEnabled()))
                .with("%event%", event != null ? event.getDefinition().getName() : none)
                .with("%preset%", preset != null ? preset : none);

        menu.item("status", null);
        menu.item("start-game", () -> openStart(player, 0));
        menu.item("global", () -> openGlobal(player, 0));
        menu.item("events", () -> openEvents(player, 0));
        menu.item("toggle", data == null || data.isToggled(), () -> run(player, "toggle"));
        menu.item("toggle-all", rotation.isTaskEnabled(), () -> run(player, "toggleAll"));
        menu.item("presets", () -> openPresets(player, 0));
        menu.item("logs", () -> openLogs(player));
        menu.item("stop", () -> run(player, "stop"));
        menu.item("reload", () -> {
            player.closeInventory();
            player.performCommand("chatgames reload");
        });
        if (player.hasPermission(PermissionKey.DEBUG.getPermission())) {
            menu.item("debug", () -> menus.getDebug().open(player));
        }
        menu.item("back", () -> menus.getMain().open(player));
        menu.item("close", player::closeInventory);
        menu.open();
    }

    /** Runs a command and shows this menu again with the new state. */
    private void run(Player player, String command) {
        player.performCommand("chatgames " + command);
        // toggleAll and stop finish on the global thread, which on Folia can be a tick later
        plugin.getScheduler().globalLater(() -> plugin.getScheduler().atPlayer(player, () -> open(player)), 2L);
    }

    /** Every game and race; clicking starts it. */
    public void openStart(@Nonnull Player player, int page) {
        MenuBuilder menu = MenuBuilder.of(plugin, "admin-start", player);
        menu.paged(Arrays.asList(GameType.values()), page, (slot, game) ->
                menu.place(slot, menu.template(game.isRace() ? "race" : "game"), gameValues(game), () -> {
                    player.closeInventory();
                    player.performCommand("chatgames " + game.getConfigKey());
                }), other -> openStart(player, other));
        menu.item("back", () -> open(player));
        menu.item("close", player::closeInventory);
        menu.open();
    }

    /** The games global_games.yml enables; clicking starts one as a global game. */
    public void openGlobal(@Nonnull Player player, int page) {
        MenuBuilder menu = MenuBuilder.of(plugin, "admin-global", player);
        List<GameType> games = plugin.getGlobalGameManager().getConfig().getEnabled();
        if (games.isEmpty()) menu.item("none", null);
        menu.paged(games, page, (slot, game) ->
                menu.place(slot, menu.template(game.isRace() ? "race" : "game"), gameValues(game), () -> {
                    player.closeInventory();
                    player.performCommand("chatgames global " + game.getConfigKey());
                }), other -> openGlobal(player, other));
        menu.item("back", () -> open(player));
        menu.item("close", player::closeInventory);
        menu.open();
    }

    /** events.yml events; clicking starts one, or stops the one running. */
    public void openEvents(@Nonnull Player player, int page) {
        EventManager events = plugin.getEventManager();
        EventManager.Run running = events.getRunning();
        MenuBuilder menu = MenuBuilder.of(plugin, "admin-events", player);
        List<EventDefinition> all = new ArrayList<>(events.getConfig().getAll());
        if (all.isEmpty()) menu.item("none", null);
        menu.paged(all, page, (slot, event) -> {
            boolean isRunning = running != null && running.getDefinition().getId().equals(event.getId());
            Map<String, String> values = new HashMap<>();
            values.put("%event%", event.getName());
            values.put("%id%", event.getId());
            values.put("%steps%", String.valueOf(event.getSteps().size()));
            values.put("%step%", isRunning ? String.valueOf(running.getStepNumber()) : "0");
            menu.place(slot, menu.template(isRunning ? "running" : "event"), values, () -> {
                player.performCommand(isRunning ? "chatgames event stop" : "chatgames event start " + event.getId());
                openEvents(player, page);
            });
        }, other -> openEvents(player, other));
        menu.item("back", () -> open(player));
        menu.item("close", player::closeInventory);
        menu.open();
    }

    /** The presets plus "none"; clicking loads one. */
    public void openPresets(@Nonnull Player player, int page) {
        MenuBuilder menu = MenuBuilder.of(plugin, "admin-presets", player);
        List<String> names = new ArrayList<>(PresetManager.getNames());
        names.add(PresetManager.NONE);
        String active = PresetManager.getActive();
        menu.paged(names, page, (slot, name) -> {
            boolean isActive = name.equalsIgnoreCase(active == null ? PresetManager.NONE : active);
            Map<String, String> values = new HashMap<>();
            values.put("%preset%", name);
            values.put("%games%", name.equals(PresetManager.NONE) ? "-"
                    : String.valueOf(PresetManager.getGames(name, null).size()));
            menu.place(slot, menu.template(isActive ? "active" : "preset"), values, () -> {
                player.performCommand("chatgames preset load " + name);
                openPresets(player, page);
            });
        }, other -> openPresets(player, other));
        menu.item("back", () -> open(player));
        menu.item("close", player::closeInventory);
        menu.open();
    }

    /** How many log entries to print in chat. */
    public void openLogs(@Nonnull Player player) {
        MenuBuilder menu = MenuBuilder.of(plugin, "admin-logs", player);
        List<Integer> amounts = menu.getSection().getIntegerList("amounts");
        List<Integer> slots = menu.listSlots("list-slots");
        for (int i = 0; i < Math.min(amounts.size(), slots.size()); i++) {
            int amount = amounts.get(i);
            Map<String, String> values = new HashMap<>();
            values.put("%amount%", String.valueOf(amount));
            menu.place(slots.get(i), menu.template("amount"), values, () -> {
                player.closeInventory();
                player.performCommand("chatgames logs " + amount);
            });
        }
        menu.item("back", () -> open(player));
        menu.item("close", player::closeInventory);
        menu.open();
    }

    @Nonnull
    static Map<String, String> gameValues(@Nonnull GameType game) {
        Map<String, String> values = new HashMap<>();
        values.put("%game%", Menus.gameName(game));
        values.put("%key%", game.getConfigKey());
        values.put("%icon%", Menus.gameIcon(game));
        return values;
    }
}
