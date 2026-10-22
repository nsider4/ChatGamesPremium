package org.nsider.chatgames.menus;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.commands.CommandSupport;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.gameevents.EventDefinition;
import org.nsider.chatgames.gameevents.EventManager;
import org.nsider.chatgames.globalgames.GlobalGameConfig;
import org.nsider.chatgames.globalgames.GlobalGameManager;
import org.nsider.chatgames.globalgames.GlobalSession;
import org.nsider.chatgames.managers.PresetManager;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.tasks.MainTask;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.menu.Menu;
import org.nsider.inventorymanager.menu.MenuManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code /chatgames admin} and its pages. Each button runs the matching command as the admin
 * (see menus.yml), so it also needs that command's permission and replies the same way;
 * these providers only fill in the status and the lists.
 */
final class AdminMenu {

    private final ChatGames plugin;

    AdminMenu(ChatGames plugin) {
        this.plugin = plugin;
    }

    void register(@Nonnull MenuManager manager) {
        manager.registerMenu("admin", this::buildMain);
        manager.registerMenu("admin-start", this::buildStart);
        manager.registerMenu("admin-global", this::buildGlobal);
        manager.registerMenu("admin-events", this::buildEvents);
        manager.registerMenu("admin-presets", this::buildPresets);
    }

    private void buildMain(Menu menu) {
        MainTask rotation = plugin.getMainTask();
        GameType current = plugin.getGameManager().getCurrentGame();
        EventManager.Run event = plugin.getEventManager().getRunning();
        ChatGamesPlayer data = ChatGamesPlayerManager.get(menu.getPlayer().getUniqueId());
        String none = Menus.none(menu);
        String preset = PresetManager.getActive();
        menu.set("%current_game%", plugin.isInGame() && current != null ? Menus.gameName(current) : none)
                .set("%next_game%", rotation.isTaskEnabled() ? rotation.getSecondsUntilNextGame() + "s" : none)
                .set("%rotation_on%", rotation.isTaskEnabled())
                .set("%rotation_state%", CommandSupport.state(rotation.isTaskEnabled()))
                .set("%games_on%", data == null || data.isToggled())
                .set("%event%", event != null ? event.getDefinition().getName() : none)
                .set("%preset%", preset != null ? preset : none);
    }

    /** Every game and race (Count is started from the global page). */
    private void buildStart(Menu menu) {
        for (GameType game : GameType.values()) {
            if (!game.isGlobalOnly()) menu.addEntry(game.isRace() ? "race" : "game", Menus.gameValues(game));
        }
    }

    /** The games global_games.yml enables, and the one in play. */
    private void buildGlobal(Menu menu) {
        GlobalGameManager globals = plugin.getGlobalGameManager();
        GlobalSession running = globals.getCurrent();
        String none = Menus.none(menu);
        menu.set("%global_running%", running != null)
                .set("%global_game%", running != null ? Menus.gameName(running.getType()) : none)
                .set("%global_goal%", running != null ? running.getGoalText() : none)
                .set("%global_progress%", running != null ? running.getProgress().getProgress() : 0)
                .set("%global_goal_amount%", running != null ? running.getProgress().getGoal() : 0)
                .set("%global_percent%", running != null ? running.getProgress().getPercent() : 0)
                .set("%global_contributors%", running != null ? running.getProgress().contributors() : 0)
                .set("%global_time_left%", running != null ? running.getSecondsLeft() : 0);
        for (GameType game : globals.getConfig().getEnabled()) {
            GlobalGameConfig.Entry entry = globals.getConfig().find(game);
            Map<String, Object> values = Menus.gameValues(game);
            values.put("%goal%", entry.getGoal());
            values.put("%duration%", entry.getDurationSeconds());
            values.put("%points%", entry.getPoints());
            menu.addEntry(game.isGlobalOnly() ? "count" : game.isRace() ? "race" : "game", values);
        }
    }

    /** events.yml events; the one running uses the {@code running} template. */
    private void buildEvents(Menu menu) {
        EventManager events = plugin.getEventManager();
        EventManager.Run running = events.getRunning();
        for (EventDefinition event : events.getConfig().getAll()) {
            boolean isRunning = running != null && running.getDefinition().getId().equals(event.getId());
            Map<String, Object> values = new HashMap<>();
            values.put("%event%", event.getName());
            values.put("%id%", event.getId());
            values.put("%steps%", event.getSteps().size());
            values.put("%step%", isRunning ? running.getStepNumber() : 0);
            menu.addEntry(isRunning ? "running" : "event", values);
        }
    }

    /** The presets plus "none"; the loaded one uses the {@code active} template. */
    private void buildPresets(Menu menu) {
        List<String> names = new ArrayList<>(PresetManager.getNames());
        names.add(PresetManager.NONE);
        String active = PresetManager.getActive();
        for (String name : names) {
            Map<String, Object> values = new HashMap<>();
            values.put("%preset%", name);
            values.put("%games%", name.equals(PresetManager.NONE) ? "-" : PresetManager.getGames(name, null).size());
            menu.addEntry(name.equalsIgnoreCase(active == null ? PresetManager.NONE : active) ? "active" : "preset", values);
        }
    }
}
