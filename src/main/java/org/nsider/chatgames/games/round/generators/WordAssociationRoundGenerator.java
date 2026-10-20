package org.nsider.chatgames.games.round.generators;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.round.Hints;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.games.round.RoundContext;
import org.nsider.chatgames.games.round.RoundGenerator;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Word Association: three clues, one hidden link, e.g. Obsidian • Flint • Steel → Nether Portal.
 *
 * <p>Associations live in words.yml under {@code word_association.data}, each with its
 * {@code clues}, accepted {@code answers} (the first is shown as the answer) and a
 * {@code difficulty}. Easy and medium rounds show every clue and hint the answer's letters.
 * Hard rounds show only the first clue; each hint reveals the next one, so whoever
 * answers with fewer clues is faster. games.yml's {@code difficulty} picks which ones are
 * played: easy, medium, hard or mixed. Arg: an association's id.</p>
 */
public final class WordAssociationRoundGenerator implements RoundGenerator {

    static final String SEPARATOR = " &7• &b";
    static final String HIDDEN = "?";

    @Nonnull
    @Override
    public Round generate(@Nonnull RoundContext context, @Nonnull String... args) {
        ConfigurationSection data = context.getWords().getConfigurationSection("word_association.data");
        if (data == null || data.getKeys(false).isEmpty()) {
            throw new IllegalStateException("words.yml has no associations under 'word_association.data'");
        }
        String id;
        if (args.length > 0 && data.isConfigurationSection(args[0])) {
            id = args[0];
        } else {
            String wanted = context.getSettings().getString("difficulty", "mixed").trim().toLowerCase(Locale.ROOT);
            List<String> ids = new ArrayList<>();
            for (String key : data.getKeys(false)) {
                if (wanted.equals("mixed") || wanted.equalsIgnoreCase(data.getString(key + ".difficulty", "easy"))) ids.add(key);
            }
            if (ids.isEmpty()) {
                throw new IllegalStateException("words.yml has no '" + wanted + "' associations (games.yml word_association.difficulty)");
            }
            id = ids.get(context.getRandom().nextInt(ids.size()));
        }

        List<String> clues = data.getStringList(id + ".clues");
        List<String> answers = data.getStringList(id + ".answers");
        if (clues.isEmpty() || answers.isEmpty()) {
            throw new IllegalStateException("Association '" + id + "' needs clues and answers");
        }
        String difficulty = data.getString(id + ".difficulty", "easy").toLowerCase(Locale.ROOT);
        boolean hard = difficulty.equals("hard") && clues.size() > 1;

        return Round.builder(GameType.WORD_ASSOCIATION, answers.get(0))
                .answers(answers)
                .display(hard ? reveal(clues, 1) : String.join(SEPARATOR, clues))
                .placeholder("%difficulty%", difficulty)
                .hints(hard ? clueHints(clues) : Hints.revealFromStart(answers.get(0)))
                .build();
    }

    /** @return the clues with only the first {@code shown} visible */
    static String reveal(List<String> clues, int shown) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < clues.size(); i++) parts.add(i < shown ? clues.get(i) : HIDDEN);
        return String.join(SEPARATOR, parts);
    }

    /** Each hint shows one more clue. */
    static List<String> clueHints(List<String> clues) {
        List<String> hints = new ArrayList<>();
        for (int shown = 2; shown <= clues.size() && hints.size() < Hints.PREPARED; shown++) hints.add(reveal(clues, shown));
        return hints;
    }
}
