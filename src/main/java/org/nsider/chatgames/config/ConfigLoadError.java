package org.nsider.chatgames.config;

import lombok.Getter;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A config file that failed to load, and where the YAML broke if the parser said so.
 * {@code /chatgames reload} lists these so staff in game see which file and line to fix.
 */
@Getter
public final class ConfigLoadError {

    private static final Pattern POSITION = Pattern.compile("line (\\d+), column (\\d+)");

    private final String file;
    /** 1-based, or -1 when the file couldn't be read at all. */
    private final int line;
    private final int column;

    ConfigLoadError(@Nonnull String file, int line, int column) {
        this.file = file;
        this.line = line;
        this.column = column;
    }

    /**
     * Reads the position out of a SnakeYAML message. Its last "line X, column Y" is the
     * problem itself; earlier ones only point at where the surrounding block started.
     */
    @Nonnull
    public static ConfigLoadError of(@Nonnull String file, @Nonnull Exception exception) {
        String message = exception.getMessage();
        int line = -1;
        int column = -1;
        if (message != null) {
            Matcher matcher = POSITION.matcher(message);
            while (matcher.find()) {
                line = Integer.parseInt(matcher.group(1));
                column = Integer.parseInt(matcher.group(2));
            }
        }
        return new ConfigLoadError(file, line, column);
    }

    public boolean hasPosition() {
        return line > 0;
    }
}
