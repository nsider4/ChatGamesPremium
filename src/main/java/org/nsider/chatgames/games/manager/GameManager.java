package org.nsider.chatgames.games.manager;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.database.queries.UpdateQueries;
import org.nsider.chatgames.database.queries.ValueQueries;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.Game;
import org.nsider.chatgames.games.builder.algorithms.RandomCharacterGenerator;
import org.nsider.chatgames.games.manager.messages.*;

import java.util.List;

public class GameManager {
    private final ChatGames plugin;
    @Getter private final GamePlayerManager gamePlayerManager;
    @Setter @Getter private Game game;
    @Getter private String clickableKey;

    private final GameStartMessage gameStartMessage;
    private final VariableMessage variableMessage;
    private final WinnerMessage winnerMessage;
    private final MemorizationMessage memorizationMessage;
    private final GameStartComponentMessage gameStartComponentMessage;

    public GameManager(ChatGames plugin) {
        this.plugin = plugin;

        this.gamePlayerManager = new GamePlayerManager(plugin);
        GameSoundManager.initialize(plugin);

        this.gameStartMessage = new GameStartMessage(plugin);
        this.variableMessage = new VariableMessage(plugin);
        this.winnerMessage = new WinnerMessage(plugin);
        this.memorizationMessage = new MemorizationMessage(plugin);
        this.gameStartComponentMessage = new GameStartComponentMessage(plugin);
    }

    public void sendGameStartedMessage(String game, String variable, String word) {
        gameStartMessage.sendGameStartedMessage(game, variable, word);
    }

    public void sendGameStartedComponent(String game, TextComponent component, String variable) {
        gameStartComponentMessage.sendGameStartedComponent(game, component, variable);
    }

    public void sendVariableMessages(List<String> game, List<String> list, String replace, String replacement, String replace1, String replacement1) {
        variableMessage.sendVariableGameMessage(game, list, replace, replacement, replace1, replacement1);
    }

    // Method to handle sending winner messages and displaying winner stats
    public void sendWinnerMessages() {
        winnerMessage.sendMessageForWinners();
    }

    // Method to handle the memorization game logic and send corresponding messages
    public void startMemorizationGame(String displayList, String selected) {
        memorizationMessage.sendMemorizationStartMessage(displayList, selected);
    }

    public void startGame(GameType type) {
        if (GameUtility.isRace(type)) {
            plugin.getRaceBuilder().startRace(type);
        } else {
            plugin.getGameBuilder().startGame(type);
        }
    }

    public void endGame() {
        plugin.getExpirationTask().cancel();
        plugin.setInGame(false);
    }

    public void generateNewClickableKey() {
        this.clickableKey = plugin.getGameAlgorithms().generateRandomCharacters(12, RandomCharacterGenerator.Type.ALPHANUMERIC);
    }

    public boolean gamesIsToggled(Player player) {
        if (plugin.getConfig().getBoolean("use-permission-toplay") && !player.hasPermission("chatgames.play")) {
            return false;
        } else {
            return ValueQueries.getPlayerToggle(player);
        }
    }

    public boolean worldEnabled(Player player) {
        for(String world : plugin.getGameConfigManager().getDisabledWorlds()) {
            if(world.equalsIgnoreCase(player.getWorld().getName())) {
                return false;
            }
        }

        return true;
    }

    public void toggleGames(Player player, boolean value) {
        UpdateQueries.updatePlayerToggle(player, value);
    }
}
