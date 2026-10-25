package org.nsider.chatgames.commands;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trims the help list from messages.yml to what one sender can use. A line naming a
 * {@code /chatgames} command the sender can't use is left out, and so is a block (a run of
 * lines after blank ones) whose commands are all left out, so an "ADMIN:" header doesn't stand
 * alone. Lines naming no command, or one this doesn't know, are always kept: the list is the
 * server owner's text.
 */
public final class HelpFilter {

    /** Whether the sender can use {@code /chatgames <name> [action]}. */
    public interface Access {
        /**
         * @param name   the word after /chatgames, lower case (the first of "a|b|c")
         * @param action the word after that, lower case, or empty
         * @return null if {@code name} isn't a command
         */
        @Nullable
        Boolean canUse(@Nonnull String name, @Nonnull String action);
    }

    private static final Pattern COLOUR = Pattern.compile("(?i)[&§](?:#[0-9a-f]{6}|[0-9a-fk-or])");
    private static final Pattern COMMAND = Pattern.compile("(?i)/chatgames?\\s+(\\S+)(?:\\s+(\\S+))?");

    private HelpFilter() {
    }

    @Nonnull
    public static List<String> filter(@Nonnull List<String> lines, @Nonnull Access access) {
        List<String> out = new ArrayList<>();
        List<String> blanks = new ArrayList<>();
        List<String> kept = new ArrayList<>();
        boolean[] commands = new boolean[2]; // [0]: the block names a command, [1]: one is kept
        for (String line : lines) {
            if (isBlank(line)) {
                if (!kept.isEmpty() || commands[0]) {
                    flush(out, blanks, kept, commands);
                }
                blanks.add(line);
                continue;
            }
            boolean named = namesCommand(line);
            Boolean usable = named ? usable(line, access) : null;
            if (named) commands[0] = true;
            if (usable == null || usable) {
                kept.add(line);
                // A command this doesn't know is kept, and keeps its block
                if (named) commands[1] = true;
            }
        }
        flush(out, blanks, kept, commands);
        out.addAll(blanks);
        return out;
    }

    private static void flush(List<String> out, List<String> blanks, List<String> kept, boolean[] commands) {
        // A block that only had commands the sender can't use goes, with the gap before it
        if (!commands[0] || commands[1]) {
            out.addAll(blanks);
            out.addAll(kept);
        }
        blanks.clear();
        kept.clear();
        commands[0] = false;
        commands[1] = false;
    }

    /** @return whether the sender can use the command the line names, null if it names none */
    @Nullable
    static Boolean usable(@Nonnull String line, @Nonnull Access access) {
        Matcher matcher = COMMAND.matcher(COLOUR.matcher(line).replaceAll(""));
        if (!matcher.find()) return null;
        String name = matcher.group(1).toLowerCase(Locale.ROOT);
        int bar = name.indexOf('|');
        if (bar > 0) name = name.substring(0, bar);
        String action = matcher.group(2) == null ? "" : matcher.group(2).toLowerCase(Locale.ROOT);
        return access.canUse(name, action);
    }

    private static boolean namesCommand(String line) {
        return COMMAND.matcher(COLOUR.matcher(line).replaceAll("")).find();
    }

    private static boolean isBlank(String line) {
        return COLOUR.matcher(line).replaceAll("").trim().isEmpty();
    }
}
