package org.nsider.chatgames.games.manager;

import com.cryptomorin.xseries.XSound;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;

import java.util.Locale;
import java.util.Optional;

/**
 * Plays the configured game sounds (config.yml → GameSounds) to players who haven't muted them.
 *
 * <p>Sounds may be written the modern way ({@code minecraft:entity.player.levelup}) or as
 * enum names ({@code ENTITY_PLAYER_LEVELUP}); XSeries maps both to whatever the server
 * version calls that sound, so one config works from 1.8 to 26.x. A name XSeries doesn't
 * know is passed to the client as-is, which lets resource-pack sounds work.</p>
 */
public final class GameSoundManager {

    private static ChatGames plugin;

    private GameSoundManager() {}

    static void initialize(ChatGames plugin) {
        GameSoundManager.plugin = plugin;
    }

    /**
     * @param type the key under GameSounds, e.g. "win" or "game-start"
     */
    public static void playGameSound(Player player, String type) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data != null && !data.isSoundEnabled()) return;

        String name = plugin.getGameConfigManager().getGameSound(type);
        if (name == null || name.trim().isEmpty()) return;
        float volume = plugin.getGameConfigManager().getGameSoundVolume();
        float pitch = plugin.getGameConfigManager().getGameSoundPitch();

        Optional<XSound> sound = XSound.of(toEnumName(name));
        Sound bukkitSound = sound.isPresent() ? sound.get().parseSound() : null;
        try {
            if (bukkitSound != null) {
                player.playSound(player.getLocation(), bukkitSound, volume, pitch);
            } else {
                player.playSound(player.getLocation(), name, volume, pitch);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound in config.yml GameSounds." + type + ": " + name);
        }
    }

    /** "minecraft:entity.player.levelup" → "ENTITY_PLAYER_LEVELUP". */
    static String toEnumName(String name) {
        String key = name.trim();
        int namespace = key.indexOf(':');
        if (namespace >= 0) key = key.substring(namespace + 1);
        return key.replace('.', '_').toUpperCase(Locale.ROOT);
    }
}
