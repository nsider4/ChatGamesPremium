package org.nsider.chatgames.commands;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpFilterTest {

    /** The commands a player has by default (plugin.yml), as "name action". */
    private static final Set<String> PLAYER = new HashSet<>(Arrays.asList("help", "menu", "profile", "top", "fastest",
            "points", "challenges", "streaks", "streak", "claim", "event", "toggle", "togglesound"));
    private static final Set<String> KNOWN = new HashSet<>(Arrays.asList("admin", "debug", "any", "global", "stop",
            "toggleall", "preset", "+points", "remove", "logs", "discord", "reload"));

    private static HelpFilter.Access player() {
        return (name, action) -> {
            if (name.equals("(game)")) return false;
            if (!PLAYER.contains(name)) return KNOWN.contains(name) ? false : null;
            // The player's commands have admin-only actions
            return !action.equals("start") && !action.equals("reset") && !action.equals("set");
        };
    }

    private static List<String> shippedHelp() throws Exception {
        YamlConfiguration file = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(HelpFilterTest.class.getResourceAsStream("/messages.yml"), StandardCharsets.UTF_8)) {
            file.load(reader);
        }
        return file.getStringList("help_command");
    }

    @Test
    void aPlayerSeesOnlyTheirCommands() throws Exception {
        List<String> help = shippedHelp();
        List<String> shown = HelpFilter.filter(help, player());
        String text = String.join("\n", shown);
        assertTrue(text.contains("/chatgames menu"), text);
        assertTrue(text.contains("/chatgames event [list|top <event>]"), text);
        assertFalse(text.contains("ADMIN"), "the header goes with its commands");
        assertFalse(text.contains("/chatgames reload"), text);
        assertFalse(text.contains("/chatgames event start"), "needs chatgames.event.manage");
        assertFalse(text.contains("/chatgames challenges reset"), text);
        assertFalse(text.contains("/chatgames streak set"), text);
        assertTrue(text.contains("GAME TYPES"), "lines naming no command stay");
        assertEquals(help.get(help.size() - 1), shown.get(shown.size() - 1), "the closing line stays");
    }

    @Test
    void anAdminSeesEverything() throws Exception {
        List<String> help = shippedHelp();
        assertEquals(help, HelpFilter.filter(help, (name, action) -> true));
    }

    @Test
    void unknownCommandsAndColourCodesAreHandled() {
        assertEquals(Boolean.TRUE, HelpFilter.usable("&e/chatgames &lmenu &8- &7Opens", (n, a) -> n.equals("menu")));
        assertEquals(null, HelpFilter.usable("&7Just text", (n, a) -> false));
        // A line an owner added for another plugin's command is kept
        List<String> lines = Arrays.asList("&6", "&e/chatgames mything &8- &7Theirs", "&e/chatgames reload &8- &7Reloads");
        assertEquals(Arrays.asList("&6", "&e/chatgames mything &8- &7Theirs"),
                HelpFilter.filter(lines, (name, action) -> name.equals("reload") ? false : null));
    }
}
