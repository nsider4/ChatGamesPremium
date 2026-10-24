package org.nsider.chatgames.games.round;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

/**
 * Everything a generator may read: the game's settings, the word lists, its
 * messages section and a translator. Passing these in (rather than reading
 * the plugin's files) keeps generators testable without a server.
 */
@Getter
public final class RoundContext {

    /** The game's section of games.yml. */
    private final ConfigurationSection settings;
    /** The whole of words.yml. */
    private final ConfigurationSection words;
    /** The game's section of messages.yml. */
    private final ConfigurationSection messages;
    private final Random random;
    /** Minecraft translation key to text, or null if unknown. */
    private final Function<String, String> translator;

    public RoundContext(@Nullable ConfigurationSection settings, @Nullable ConfigurationSection words,
                        @Nullable ConfigurationSection messages, @Nonnull Random random,
                        @Nonnull Function<String, String> translator) {
        this.settings = orEmpty(settings);
        this.words = orEmpty(words);
        this.messages = orEmpty(messages);
        this.random = random;
        this.translator = translator;
    }

    /**
     * Picks a random entry from a words.yml list, skipping blacklisted words.
     *
     * @throws IllegalStateException if the list has no usable word
     */
    @Nonnull
    public String randomWord(@Nonnull String path) {
        List<String> usable = filterBlacklisted(words.getStringList(path));
        if (usable.isEmpty()) {
            throw new IllegalStateException("words.yml has no usable entries at '" + path + "'");
        }
        return usable.get(random.nextInt(usable.size()));
    }

    @Nonnull
    public List<String> filterBlacklisted(@Nonnull List<String> list) {
        Set<String> blacklist = new HashSet<>();
        for (String word : words.getStringList("blacklistedWords")) {
            blacklist.add(word.toLowerCase(Locale.ROOT));
        }
        List<String> usable = new ArrayList<>();
        for (String word : list) {
            if (word != null && !word.trim().isEmpty() && !blacklist.contains(word.toLowerCase(Locale.ROOT))) {
                usable.add(word);
            }
        }
        return usable;
    }

    /** A random int in [min, max]; the bounds may come in either order. */
    public int between(int min, int max) {
        int low = Math.min(min, max);
        int high = Math.max(min, max);
        return low + random.nextInt(high - low + 1);
    }

    private static ConfigurationSection orEmpty(ConfigurationSection section) {
        return section != null ? section : new MemoryConfiguration();
    }
}
