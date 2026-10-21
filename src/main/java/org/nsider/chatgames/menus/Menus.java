package org.nsider.chatgames.menus;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Locale;

/**
 * Every menu (menus.yml), built per player when opened. Buttons that change something
 * mostly run the matching {@code /chatgames} command as the player, so permissions,
 * messages and threading stay the same as typing it.
 */
public final class Menus {

    private final MainMenu main;
    private final ProfileMenu profile;
    private final ChallengesMenu challenges;
    private final StreaksMenu streaks;
    private final LeaderboardMenu leaderboards;
    private final AdminMenu admin;
    private final DebugMenu debug;

    public Menus(@Nonnull ChatGames plugin) {
        this.main = new MainMenu(plugin, this);
        this.profile = new ProfileMenu(plugin, this);
        this.challenges = new ChallengesMenu(plugin, this);
        this.streaks = new StreaksMenu(plugin, this);
        this.leaderboards = new LeaderboardMenu(plugin, this);
        this.admin = new AdminMenu(plugin, this);
        this.debug = new DebugMenu(plugin, this);
    }

    @Nonnull public MainMenu getMain() { return main; }
    @Nonnull public ProfileMenu getProfile() { return profile; }
    @Nonnull public ChallengesMenu getChallenges() { return challenges; }
    @Nonnull public StreaksMenu getStreaks() { return streaks; }
    @Nonnull public LeaderboardMenu getLeaderboards() { return leaderboards; }
    @Nonnull public AdminMenu getAdmin() { return admin; }
    @Nonnull public DebugMenu getDebug() { return debug; }

    /** @return the game's name for menus: menus.yml {@code game-names}, else "Guess The Number" from the key */
    @Nonnull
    public static String gameName(@Nonnull GameType type) {
        String configured = ConfigManager.getMenus().getString("game-names." + type.getConfigKey());
        if (configured != null) return configured;
        StringBuilder name = new StringBuilder();
        for (String word : type.getConfigKey().split("_")) {
            if (name.length() > 0) name.append(' ');
            name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return name.toString();
    }

    /** @return the game's icon from menus.yml {@code game-icons} */
    @Nonnull
    public static String gameIcon(@Nonnull GameType type) {
        return ConfigManager.getMenus().getString("game-icons." + type.getConfigKey(), "paper");
    }

    /** Seconds with 3 decimals, as in the leaderboards. */
    @Nonnull
    public static String seconds(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    /** Sends a menus.yml {@code messages.<key>} line, if set. */
    static void message(@Nonnull Player player, @Nonnull String key, @Nonnull String... placeholders) {
        String text = ConfigManager.getMenus().getString("messages." + key, "");
        if (text.isEmpty()) return;
        for (int i = 0; i + 1 < placeholders.length; i += 2) text = text.replace(placeholders[i], placeholders[i + 1]);
        MessageUtils.sendMessage(player, text);
    }
}
