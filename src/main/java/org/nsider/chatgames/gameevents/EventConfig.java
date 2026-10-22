package org.nsider.chatgames.gameevents;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.globalgames.GlobalGameConfig;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * events.yml: the events that can run, by id. Unchanging; a reload builds a new one.
 */
public final class EventConfig {

    public static final EventConfig EMPTY = new EventConfig(Collections.<String, EventDefinition>emptyMap());

    /** Ids end up in stat names ({@code event.<id>.points}), which hold 64 characters. */
    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,32}");

    private final Map<String, EventDefinition> events;

    private EventConfig(Map<String, EventDefinition> events) {
        this.events = Collections.unmodifiableMap(events);
    }

    @Nonnull
    public static EventConfig load(@Nonnull ConfigurationSection file, @Nonnull Consumer<String> warn) {
        Map<String, EventDefinition> events = new LinkedHashMap<>();
        ConfigurationSection section = file.getConfigurationSection("events");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection event = section.getConfigurationSection(key);
                String id = key.toLowerCase(Locale.ROOT);
                if (event == null || !ID.matcher(id).matches()) {
                    warn.accept("events." + key + ": ids may only use a-z, 0-9, - and _ (up to 32); skipped");
                    continue;
                }
                EventDefinition definition = definition(id, event, problem -> warn.accept("events." + key + ": " + problem));
                if (definition != null) events.put(id, definition);
            }
        }
        return new EventConfig(events);
    }

    @Nullable
    private static EventDefinition definition(String id, ConfigurationSection event, Consumer<String> warn) {
        List<EventStep> steps = new ArrayList<>();
        List<Map<?, ?>> stepMaps = event.getMapList("steps");
        for (int i = 0; i < stepMaps.size(); i++) {
            EventStep step = step(stepMaps.get(i), problem -> warn.accept(problem));
            if (step != null) steps.add(step);
        }
        if (steps.isEmpty()) {
            warn.accept("no valid steps; skipped");
            return null;
        }

        List<EventSchedule> schedule = new ArrayList<>();
        for (String text : event.getStringList("schedule")) {
            EventSchedule parsed = EventSchedule.parse(text);
            if (parsed == null) warn.accept("schedule '" + text + "' isn't like 'SATURDAY 20:00' or 'DAILY 18:30'; ignored");
            else schedule.add(parsed);
        }

        List<Integer> pointList = event.getIntegerList("points");
        int[] points = new int[pointList.size()];
        for (int i = 0; i < points.length; i++) points[i] = Math.max(0, pointList.get(i));

        Map<String, List<String>> rewards = new LinkedHashMap<>();
        ConfigurationSection rewardSection = event.getConfigurationSection("rewards");
        if (rewardSection != null) {
            for (String place : rewardSection.getKeys(false)) {
                rewards.put(place.toLowerCase(Locale.ROOT), rewardSection.getStringList(place));
            }
        }

        return new EventDefinition(id, event.getString("name", id), schedule,
                Math.max(1, event.getInt("minimum-players", 1)),
                Math.max(1, event.getInt("pause-seconds", 10)),
                points, Math.max(0, event.getInt("participation-points", 0)), steps, rewards);
    }

    @Nullable
    private static EventStep step(Map<?, ?> map, Consumer<String> warn) {
        Object game = map.get("game");
        GameType type = game == null ? null : GameType.fromName(String.valueOf(game));
        if (type == null) {
            warn.accept("step game '" + game + "' isn't a game or race; skipped");
            return null;
        }
        boolean global = type.isGlobalOnly() || Boolean.TRUE.equals(map.get("global"))
                || "true".equalsIgnoreCase(String.valueOf(map.get("global")));
        if (global && !GlobalGameConfig.supports(type)) {
            warn.accept(type.getConfigKey() + " can't be played globally; it plays as a normal game");
            global = false;
        }

        Map<String, Object> settings = new LinkedHashMap<>();
        Object raw = map.get("settings");
        if (raw instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) settings.put(String.valueOf(entry.getKey()), entry.getValue());
        } else if (raw instanceof ConfigurationSection) {
            settings.putAll(((ConfigurationSection) raw).getValues(true));
        }

        String[] args;
        Object rawArgs = map.get("args");
        if (rawArgs instanceof Collection) {
            List<String> list = new ArrayList<>();
            for (Object arg : (Collection<?>) rawArgs) list.add(String.valueOf(arg));
            args = list.toArray(new String[0]);
        } else if (rawArgs != null && !String.valueOf(rawArgs).trim().isEmpty()) {
            args = String.valueOf(rawArgs).trim().split("\\s+");
        } else {
            args = new String[0];
        }

        return new EventStep(type, global, Math.max(0, number(map.get("time"))), Math.max(0, number(map.get("winners"))),
                settings, args);
    }

    private static int number(@Nullable Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Nullable
    public EventDefinition get(@Nonnull String id) {
        return events.get(id.toLowerCase(Locale.ROOT));
    }

    /** @return every event, in file order */
    @Nonnull
    public Collection<EventDefinition> getAll() {
        return events.values();
    }
}
