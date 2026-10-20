package org.nsider.chatgames.races.setup;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.events.ChatGamesStartEvent;
import org.nsider.chatgames.races.*;
import org.nsider.chatgames.races.validator.EntityValidator;
import org.nsider.chatgames.races.validator.MaterialValidator;
import org.nsider.chatgames.utils.management.TimeUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public class RaceStarter {
    private final ChatGames plugin;
    private final MaterialValidator materialValidator;
    private final EntityValidator entityValidator;
    private final RaceUtils raceUtils;
    private final RaceMessageSender raceMessageSender;

    public RaceStarter(ChatGames plugin, MaterialValidator materialValidator, EntityValidator entityValidator, RaceUtils raceUtils) {
        this.plugin = plugin;
        this.materialValidator = materialValidator;
        this.entityValidator = entityValidator;
        this.raceUtils = raceUtils;
        raceMessageSender = new RaceMessageSender(plugin, raceUtils);
    }

    public void startRaceWithEntityType(HashMap<String, List<Integer>> data, String raceName, ValidatedEntity validatedEntity, int amount) {
        try {
            String entityTypeName;

            /*if (plugin.isMythicMobsEnabled() && validatedEntity.isMythicMob()) {
                MythicMob mythicMob = validatedEntity.getMythicMob();
                entityTypeName = mythicMob.getInternalName();
            } else {*/
            EntityType entityType = validatedEntity.getEntityType();
            entityTypeName = entityType.name();

            List<Integer> amountList = data.get(entityTypeName.toUpperCase());
            if (amountList == null || amountList.isEmpty()) {
                plugin.getLogger().log(Level.INFO, "No data found for entity type: {0}", entityTypeName);
                return;
            }

            if (amount <= 0) {
                plugin.getLogger().log(Level.INFO, "Invalid amount specified: {0}", amount);
                return;
            }

            plugin.getRaceBuilder().setRace(getRaceType(raceName, validatedEntity, amount, new ArrayList<>(raceUtils.getRewardsForGame(raceName, entityTypeName, amount))));
            startRace(raceName, amount);
        } catch (Exception e) {
            plugin.getLogger().log(Level.INFO, "Error while trying to start game", e);
        }
    }

    public void startRaceWithMaterial(HashMap<String, List<Integer>> data, String raceName, Material material, int amount) {
        try {
            String materialName = material.name();

            List<Integer> amountList = data.get(materialName);
            if (amountList == null || amountList.isEmpty()) {
                plugin.getLogger().log(Level.INFO, "No data found for material: {0}", materialName);
                return;
            }

            if (amount <= 0) {
                plugin.getLogger().log(Level.INFO, "Invalid amount specified: {0}", amount);
                return;
            }

            plugin.getRaceBuilder().setRace(getRaceType(raceName, material, amount, new ArrayList<>(raceUtils.getRewardsForGame(raceName, materialName, amount))));
            startRace(raceName, amount);
        } catch (Exception e) {
            plugin.getLogger().log(Level.INFO, "Error while trying to start game", e);
        }
    }

    public Race getRaceType(String raceName, Object... args) throws Exception {
        RaceType raceType = RaceType.valueOf(raceName.toUpperCase());
        Class<?> raceClass = raceType.getRaceClass();
        Constructor<?> constructor = raceClass.getConstructor(getParameterTypes(args));
        return (Race) constructor.newInstance(args);
    }

    private Class<?>[] getParameterTypes(Object[] args) {
        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ValidatedEntity) {
                parameterTypes[i] = ValidatedEntity.class;
            } else if (args[i] instanceof Material) {
                parameterTypes[i] = Material.class;
            } else if (args[i] instanceof Integer) {
                parameterTypes[i] = Integer.TYPE;
            } else if (args[i] instanceof List) {
                parameterTypes[i] = List.class;
            } else {
                plugin.getLogger().warning("Error while trying to get one of the values to start a game");
            }
        }
        return parameterTypes;
    }

    private void startRace(String game, int amount) {
        plugin.setInGame(true);
        TimeUtils.startTimer();
        RaceHandler.addPlayersOnline();
        plugin.getRaceBuilder().setCurrentRace(game);

        raceMessageSender.sendRaceStartedMessage(game, "%amount%", amount);

        plugin.getExpirationTask().scheduleExpirationTask(game);

        ChatGamesStartEvent startEvent = new ChatGamesStartEvent(plugin, GameType.valueOf(game.toUpperCase()));
        plugin.getScheduler().global(() -> plugin.getServer().getPluginManager().callEvent(startEvent));
    }

    public void startEntityTypeRace(String raceName, HashMap<String, List<Integer>> data) {
        String randomPick = getRandomDataKey(data);
        List<Integer> amountList = data.get(randomPick);
        int randomAmount = amountList.get(new Random().nextInt(amountList.size()));

        ValidatedEntity validatedEntity = entityValidator.validateEntityType(randomPick);
        if (validatedEntity == null) return;

        startRaceWithEntityType(data, raceName, validatedEntity, randomAmount);
    }

    public void startMaterialTypeRace(String raceName, HashMap<String, List<Integer>> data) {
        String randomMaterial = getRandomDataKey(data);
        List<Integer> amountList = data.get(randomMaterial);
        int randomAmount = amountList.get(new Random().nextInt(amountList.size()));
        Material material = materialValidator.validateMaterial(randomMaterial);
        if(material == null) return;

        startRaceWithMaterial(data, raceName, material, randomAmount);
    }

    private String getRandomDataKey(HashMap<String, List<Integer>> data) {
        return data.keySet().stream()
                .skip(new Random().nextInt(data.size()))
                .findFirst()
                .orElse(null);
    }
}
