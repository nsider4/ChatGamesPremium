package org.nsider.chatgames.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A chat game or race: one of the built-in ones, or one another plugin added through
 * {@link ChatGamesAPI#registerGame} or {@link ChatGamesAPI#registerRace}.
 *
 * <p>There is one instance per game, so compare them with {@code ==}. The id is the game's
 * section name in the config files (games.yml, messages.yml, rewards.yml, races.yml) and what
 * players type in {@code /chatgames <game>}. Ids are lower-case letters, digits and underscores.</p>
 */
public final class GameType {

    /** What kind of game it is. */
    public enum Kind {
        /** Answered in chat. */
        CHAT,
        /** Won by doing something in the world a number of times (killing mobs, mining...). */
        RACE,
        /** Only played as a global game, like Count. */
        GLOBAL_ONLY
    }

    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]{1,31}");
    private static final String CHATGAMES = "ChatGames";
    /** Guarded by GameType.class; {@link #values} is a snapshot, so readers never lock. */
    private static final Map<String, GameType> BY_ID = new LinkedHashMap<>();
    private static volatile GameType[] all = new GameType[0];

    public static final GameType UNSCRAMBLE = builtIn("unscramble", Kind.CHAT);
    public static final GameType MATH = builtIn("math", Kind.CHAT);
    public static final GameType TRIVIA = builtIn("trivia", Kind.CHAT);
    public static final GameType REACTION = builtIn("reaction", Kind.CHAT);
    public static final GameType CLICKABLE = builtIn("clickable", Kind.CHAT);
    public static final GameType RANDOM = builtIn("random", Kind.CHAT);
    public static final GameType UNREVERSE = builtIn("unreverse", Kind.CHAT);
    public static final GameType VARIABLE = builtIn("variable", Kind.CHAT);
    public static final GameType GUESS_THE_NUMBER = builtIn("guess_the_number", Kind.CHAT);
    public static final GameType MEMORIZE = builtIn("memorize", Kind.CHAT);
    public static final GameType SEQUENCE = builtIn("sequence", Kind.CHAT);
    public static final GameType FILLOUT = builtIn("fillout", Kind.CHAT);
    public static final GameType HOVERABLE = builtIn("hoverable", Kind.CHAT);
    public static final GameType WORD_ASSOCIATION = builtIn("word_association", Kind.CHAT);
    /** Counting together in chat; only played as a global game. */
    public static final GameType COUNT = builtIn("count", Kind.GLOBAL_ONLY);
    public static final GameType HUNT = builtIn("hunt", Kind.RACE);
    public static final GameType PLACE = builtIn("place", Kind.RACE);
    public static final GameType MINE = builtIn("mine", Kind.RACE);
    public static final GameType FISH = builtIn("fish", Kind.RACE);
    public static final GameType EAT = builtIn("eat", Kind.RACE);
    public static final GameType CRAFT = builtIn("craft", Kind.RACE);
    public static final GameType FURNACE = builtIn("furnace", Kind.RACE);
    public static final GameType TAME = builtIn("tame", Kind.RACE);
    public static final GameType SHEAR = builtIn("shear", Kind.RACE);

    private final String id;
    private final Kind kind;
    private final String owner;
    private final boolean builtIn;

    private GameType(String id, Kind kind, String owner, boolean builtIn) {
        this.id = id;
        this.kind = kind;
        this.owner = owner;
        this.builtIn = builtIn;
    }

    private static GameType builtIn(String id, Kind kind) {
        return add(new GameType(id, kind, CHATGAMES, true));
    }

    /** @return the id, e.g. {@code "word_association"} */
    public String getId() {
        return id;
    }

    /** @return the game's section name in the config files; the same as {@link #getId()} */
    public String getConfigKey() {
        return id;
    }

    /** @return the id in upper case, e.g. {@code "WORD_ASSOCIATION"}, as shown in logs */
    public String name() {
        return id.toUpperCase(Locale.ROOT);
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isRace() {
        return kind == Kind.RACE;
    }

    /** @return true for games only played as a global game, never in the rotation */
    public boolean isGlobalOnly() {
        return kind == Kind.GLOBAL_ONLY;
    }

    /** @return true for the built-in races that target a material (every one but Hunt, Tame and Shear) */
    public boolean isMaterialRace() {
        return builtIn && kind == Kind.RACE && !isMobRace();
    }

    /** @return true for the built-in races that target a mob: Hunt, Tame and Shear */
    public boolean isMobRace() {
        return this == HUNT || this == TAME || this == SHEAR;
    }

    /** @return true for the games ChatGames comes with */
    public boolean isBuiltIn() {
        return builtIn;
    }

    /** @return the name of the plugin that added the game ("ChatGames" for built-in games) */
    public String getOwner() {
        return owner;
    }

    @Override
    public String toString() {
        return name();
    }

    /** @return every game, built-in ones first, then the others in the order they were added */
    public static GameType[] values() {
        return all.clone();
    }

    /**
     * Looks a game up by id, ignoring case. Accepts the 1.9.9 name "shoppinglist" for {@link #MEMORIZE}.
     *
     * @return the game, or null if no game has that id
     */
    public static GameType fromName(String name) {
        if (name == null) return null;
        String id = name.trim().toLowerCase(Locale.ROOT);
        if (id.equals("shoppinglist")) return MEMORIZE;
        for (GameType type : all) {
            if (type.id.equals(id)) return type;
        }
        return null;
    }

    /** @return true if the id is well-formed: 2-32 lower-case letters, digits or underscores, starting with a letter */
    public static boolean isValidId(String id) {
        return id != null && ID.matcher(id).matches();
    }

    /**
     * Adds a game. For ChatGames itself; other plugins use {@link ChatGamesAPI#registerGame}
     * or {@link ChatGamesAPI#registerRace}, which also set up its files and logic.
     *
     * @throws IllegalArgumentException if the id is malformed or already taken
     */
    public static GameType register(String id, Kind kind, String owner) {
        if (!isValidId(id)) {
            throw new IllegalArgumentException("Game id '" + id + "' must be 2-32 lower-case letters, digits or _ and start with a letter");
        }
        if (kind == null || owner == null) throw new IllegalArgumentException("kind and owner are required");
        return add(new GameType(id, kind, owner, false));
    }

    /**
     * Removes a game another plugin added. For ChatGames itself; see {@link ChatGamesAPI#unregister}.
     *
     * @return false for built-in games and games that aren't registered
     */
    public static boolean unregister(GameType type) {
        if (type == null || type.builtIn) return false;
        synchronized (GameType.class) {
            if (BY_ID.get(type.id) != type) return false;
            BY_ID.remove(type.id);
            all = BY_ID.values().toArray(new GameType[0]);
            return true;
        }
    }

    private static GameType add(GameType type) {
        synchronized (GameType.class) {
            if (BY_ID.containsKey(type.id) || type.id.equals("shoppinglist")) {
                throw new IllegalArgumentException("A game with the id '" + type.id + "' already exists");
            }
            BY_ID.put(type.id, type);
            List<GameType> types = new ArrayList<>(BY_ID.values());
            all = types.toArray(new GameType[0]);
        }
        return type;
    }
}
