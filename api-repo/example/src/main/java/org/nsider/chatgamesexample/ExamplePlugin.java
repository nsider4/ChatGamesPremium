package org.nsider.chatgamesexample;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.nsider.chatgames.api.ChatGame;
import org.nsider.chatgames.api.ChatGamesAPI;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.api.Question;
import org.nsider.chatgames.api.RaceGame;
import org.nsider.chatgames.api.event.ChatGamesWinEvent;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Everything the ChatGames API can add, in one plugin:
 * <ul>
 *     <li>a chat game, "capitals": name the capital of a country;</li>
 *     <li>a race, "travel": walk, sprint or sneak a number of blocks first;</li>
 *     <li>a reward action, {@code [example_broadcast] <message>}, for rewards.yml and the other reward lists;</li>
 *     <li>a challenge type, {@code type: blocks_travelled}, for challenges.yml;</li>
 *     <li>an event listener that doubles the points won at weekends.</li>
 * </ul>
 * Everything is removed again when this plugin disables.
 */
public final class ExamplePlugin extends JavaPlugin implements Listener {

    private static final List<String[]> CAPITALS = Arrays.asList(
            new String[]{"France", "Paris"},
            new String[]{"Japan", "Tokyo"},
            new String[]{"Canada", "Ottawa"},
            new String[]{"Australia", "Canberra"},
            new String[]{"Brazil", "Brasilia", "Brasília"},
            new String[]{"Egypt", "Cairo"});

    private GameType travel;

    @Override
    public void onEnable() {
        ChatGamesAPI api = ChatGamesAPI.get();

        // A chat game. Its sections are added to games.yml, messages.yml and rewards.yml the
        // first time, and it joins the rotation (games.yml: capitals.enable).
        api.registerGame(this, ChatGame.builder("capitals")
                .displayName("Capitals")
                .icon("map")
                .setting("timeToGuess_seconds", 30)
                .messages(
                        Arrays.asList("", "&6&lCAPITALS &7» &fWhat is the capital of &e%selection%&f?", ""),
                        Arrays.asList("&6&lCAPITALS &7» &a%winner_1% &fknew it was &e%correct_answer% &7(%time_1%s)"),
                        Arrays.asList("&6&lCAPITALS &7» &fNobody knew. It was &e%correct_answer%&f."))
                .rewards("[example_broadcast] &e%player% &fknows their capitals!")
                .generator(context -> {
                    String[] pick = CAPITALS.get(context.getRandom().nextInt(CAPITALS.size()));
                    Question.Builder question = Question.builder(pick[1]).display(pick[0]).revealHints();
                    for (int i = 2; i < pick.length; i++) question.answer(pick[i]); // other spellings
                    return question.build();
                })
                .build());

        // A race. ChatGames picks a target and an amount from races.yml, announces it and
        // decides the winner; this plugin reports progress (see onMove).
        travel = api.registerRace(this, RaceGame.builder("travel")
                .displayName("Travel")
                .icon("leather_boots")
                .target("walk", 50, 100)
                .target("sprint", 100, 200)
                .target("sneak", 20, 40)
                .targetName(target -> target.charAt(0) + target.substring(1).toLowerCase())
                .build());

        // A reward action: "[example_broadcast] <message>" in any reward list
        api.registerRewardAction(this, "example_broadcast",
                (player, message) -> Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message)));

        // A challenge type: "type: blocks_travelled" in challenges.yml
        api.registerChallengeType(this, "blocks_travelled");

        getServer().getPluginManager().registerEvents(this, this);
    }

    /** Counts each block a player moves into towards the race and the challenge. */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ())) return;

        Player player = event.getPlayer();
        String mode = player.isSneaking() ? "sneak" : player.isSprinting() ? "sprint" : "walk";
        ChatGamesAPI api = ChatGamesAPI.get();
        // Both are safe from any thread, including Folia's region threads, and cheap when idle
        api.addRaceProgress(player, travel, mode, 1);
        api.addChallengeProgress(player, "blocks_travelled", 1);
    }

    /** Double points at weekends, for every game. */
    @EventHandler
    public void onWin(ChatGamesWinEvent event) {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            event.setPoints(event.getPoints() * 2);
        }
    }
}
