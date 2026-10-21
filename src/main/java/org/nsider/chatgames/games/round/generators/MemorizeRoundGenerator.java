package org.nsider.chatgames.games.round.generators;

import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.round.Hints;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.games.round.RoundContext;
import org.nsider.chatgames.games.round.RoundGenerator;

import org.nsider.inventorymanager.extras.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Memorize: a list is shown, chat is cleared, and players type the list back, comma separated.
 *
 * <p>Each round draws its items from one of the pools listed in games.yml:</p>
 * <ul>
 *   <li>{@code numbers}: random numbers from 1 to 99</li>
 *   <li>{@code blocks} and {@code mobs}: Minecraft names from words.yml, shown in the server's language</li>
 *   <li>any other name: a plain list at {@code memorize.pools.<name>} in words.yml.
 *       {@code shopping_list} falls back to 1.9.9's {@code shoppinglist.items}.</li>
 * </ul>
 * Arg: the list length.
 */
public final class MemorizeRoundGenerator implements RoundGenerator {

    @Nonnull
    @Override
    public Round generate(@Nonnull RoundContext context, @Nonnull String... args) {
        int length = context.getSettings().getInt("listLength", 3);
        if (args.length > 0) {
            Integer requested = SimpleRoundGenerators.parseInt(args[0]);
            if (requested != null) length = requested;
        }
        length = Math.max(1, Math.min(10, length));

        List<String> pools = context.getSettings().getStringList("pools");
        if (pools.isEmpty()) pools = Collections.singletonList("shopping_list");
        String pool = pools.get(context.getRandom().nextInt(pools.size()));

        List<String> items = draw(context, pool.toLowerCase(Locale.ROOT), length);
        if (items.isEmpty()) {
            throw new IllegalStateException("The memorize pool '" + pool + "' has no items in words.yml");
        }
        String list = String.join(", ", items);
        int seconds = Math.max(1, context.getSettings().getInt("timeToMemorize", 7));
        return Round.builder(GameType.MEMORIZE, list)
                .display(list)
                .placeholder("%timeToMemorize%", seconds)
                .placeholder("%pool%", pool)
                .memorize(seconds)
                .listAnswer(context.getSettings().getBoolean("order-matters", true))
                .hints(firstItems(items))
                .build();
    }

    private static List<String> draw(RoundContext context, String pool, int length) {
        List<String> items = new ArrayList<>();
        if (pool.equals("numbers")) {
            while (items.size() < length) {
                String number = String.valueOf(1 + context.getRandom().nextInt(99));
                if (!items.contains(number)) items.add(number);
            }
            return items;
        }

        List<String> source = context.getWords().getStringList("memorize.pools." + pool);
        if (source.isEmpty() && pool.equals("shopping_list")) {
            source = context.getWords().getStringList("shoppinglist.items");
        }
        source = new ArrayList<>(context.filterBlacklisted(source));
        Collections.shuffle(source, context.getRandom());
        for (String entry : source) {
            if (items.size() >= length) break;
            String name = pool.equals("blocks") ? translate(context, entry, "block.minecraft.", "item.minecraft.")
                    : pool.equals("mobs") ? translate(context, entry, "entity.minecraft.")
                    : entry;
            // Commas separate the answer's items, so an item can't contain one
            name = name.replace(",", "");
            if (!items.contains(name)) items.add(name);
        }
        return items;
    }

    /** Looks the id up under each translation prefix in turn; falls back to "Diamond Block" style. */
    private static String translate(RoundContext context, String id, String... prefixes) {
        String key = id.trim().toLowerCase(Locale.ROOT);
        for (String prefix : prefixes) {
            String translated = context.getTranslator().apply(prefix + key);
            if (translated != null && !translated.isEmpty()) return translated;
        }
        StringBuilder pretty = new StringBuilder();
        for (String part : key.split("_")) {
            if (part.isEmpty()) continue;
            if (pretty.length() > 0) pretty.append(' ');
            pretty.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return pretty.toString();
    }

    /** "apple, ?, ?" then "apple, banana, ?". */
    private static List<String> firstItems(List<String> items) {
        List<String> hints = new ArrayList<>();
        for (int shown = 1; shown < items.size() && hints.size() < Hints.PREPARED; shown++) {
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) parts.add(i < shown ? items.get(i) : "?");
            hints.add(String.join(", ", parts));
        }
        return hints;
    }
}
